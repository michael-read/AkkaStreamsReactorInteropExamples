package com.lightbend.reactor;

import akka.Done;
import akka.stream.Attributes;
import akka.stream.Inlet;
import akka.stream.SinkShape;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.GraphStageLogic;
import akka.stream.stage.GraphStageWithMaterializedValue;
import reactor.core.publisher.BaseSubscriber;
import org.reactivestreams.Subscription;

import scala.Tuple2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ReactorCustomSubscriberSink<T> extends GraphStageWithMaterializedValue<SinkShape<T>, CompletionStage<Done>> {
    public final Inlet<T> in = Inlet.create("ReactorCustomSubscriberSink.in");

    private final SinkShape<T> shape = SinkShape.of(in);

    private final BaseSubscriber<T> subscriber;

    public ReactorCustomSubscriberSink(BaseSubscriber<T> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public SinkShape<T> shape() {
        return shape;
    }

    @Override
    public Tuple2<GraphStageLogic, CompletionStage<Done>> createLogicAndMaterializedValue(Attributes inheritedAttributes) throws Exception, Exception {
        CompletableFuture<Done> finishPromise = new CompletableFuture<Done>();

        return new Tuple2<>(new GraphStageLogic(shape()) {

            /*
            We need to provide a subscription for the onSubscribe, but not sure we
             */
            final Subscription subscription = new Subscription() {
                @Override
                public void request(long n) {
//                    System.out.println(String.format("subscription request %d", n));
                    pull(in);
                }

                @Override
                public void cancel() {
                    System.out.println("subscription cancel");
                }
            };

            // This requests one element at the Sink startup.
            @Override
            public void preStart() {
//                pull(in);
                subscriber.onSubscribe(subscription);
            }

            {
                setHandler(
                        in,
                        new AbstractInHandler() {
                            @Override
                            public void onPush() throws Exception {
                                 T element = grab(in);
                                 subscriber.onNext(element);
//                                 pull(in); // this is handled in the subscription
                            }

                            /*
                            is called once the upstream has completed and no longer can be pulled
                            for new elements. No more onPush() will arrive after this event. If not
                            overridden this will default to stopping the operator.
                             */

                            @Override
                            public void onUpstreamFinish() {
                                System.out.println("received onUpstreamFinish");
                                finishPromise.complete(Done.done());
                                completeStage();
                                subscriber.cancel();
                            }

                            /*
                            is called if the upstream failed with an exception and no longer can be
                            pulled for new elements. No more onPush() will arrive after this event.
                            If not overridden this will default to failing the operator.
                             */
                            @Override
                            public void onUpstreamFailure(Throwable cause) {
                                System.out.printf("received onUpstreamFailure: %s%n", cause.getMessage());
                                finishPromise.exceptionally((ex) -> Done.done());
                                failStage(cause);
                                subscriber.cancel();
                            }
                        });
            }
        }, finishPromise.toCompletableFuture());
    }
}
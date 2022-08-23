package com.mread.reactor;

import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.stream.Graph;
import akka.stream.SinkShape;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.mread.ops.ReactorCustomSubscriberSink;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.concurrent.CompletionStage;

public class TestReactorCustomSubscriberSink {

    public static Behavior<NotUsed> rootBehavior() {
        return Behaviors.setup(context -> {

            Flux<String> x = Flux.just("red", "white", "blue");

            class ReactorSubscriber<T> extends BaseSubscriber<T> {

                @Override
                public void hookOnSubscribe(Subscription subscription) {
                    System.out.println("hookOnSubscribe received:" + subscription.toString());
                    request(1);
                }

                @Override
                public void hookOnNext(T value) {
                    System.out.println(String.format("hookOnNext received %s", value.toString()));
                    request(1);
                }

                @Override
                public void hookOnComplete() {
                    System.out.println("hookOnComplete received.");
                    // do whatever, and then notify the subscription we're done.
                }

                @Override
                public void hookOnCancel() {
                    System.out.println("hookOnCancel received.");
                }

/*                @Override
                public void hookOnError(Throwable cause) {

                }*/

                @Override
                public void hookFinally(SignalType type) {
                    System.out.println(String.format("hookFinally received %s",type.toString()));
                }
            }

            ReactorSubscriber<String> reactorSubscriber = new ReactorSubscriber<String>();

            Graph<SinkShape<String>, CompletionStage<Done>> customSink = new ReactorCustomSubscriberSink<>(reactorSubscriber);

            CompletionStage<Done> done = Source.fromPublisher(x)
                    .map(String::toUpperCase)
                    .toMat(Sink.fromGraph(customSink), Keep.right())
                    .run(context.getSystem());

            done.thenRun(() -> {
                context.getSystem().terminate();
            });

            return Behaviors.empty();
        });
    }

    public static void main(String[] args) {
        ActorSystem<NotUsed> system = ActorSystem.create(rootBehavior(), "ActorSystem");
        system.getWhenTerminated();
    }

}

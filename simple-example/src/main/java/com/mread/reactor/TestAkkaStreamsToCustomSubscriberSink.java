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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class TestAkkaStreamsToCustomSubscriberSink {

    public static Behavior<NotUsed> rootBehavior() {
        return Behaviors.setup(context -> {

            List<String> x = Arrays.asList("red", "white", "blue");
            Iterable<String> iterable = () -> x.iterator();

            class ReactorSubscriber<T> extends BaseSubscriber<T> {

                public void hookOnSubscribe(Subscription subscription) {
                    System.out.println("Subscribed:" + subscription.toString());
                    request(1);
                }

                public void hookOnNext(T value) {
                    System.out.println(value);
                    request(1);
                }
            }

            ReactorSubscriber<String> reactorSubscriber = new ReactorSubscriber<String>();

            Graph<SinkShape<String>, CompletionStage<Done>> customSink = new ReactorCustomSubscriberSink<>(reactorSubscriber);

            CompletionStage<Done> done = Source.from(iterable)
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

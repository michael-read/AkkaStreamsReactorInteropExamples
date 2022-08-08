package com.lightbend.reactor;

import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

import java.util.Arrays;
import java.util.List;

public class TestAkkaStreamsToReactorSubscriber {

    public static Behavior<NotUsed> rootBehavior() {
        return Behaviors.setup(context -> {

            List<String> x = Arrays.asList("red", "white", "blue");
            Iterable<String> iterable = () -> x.iterator();

            class ReactorSubscriber<T> extends BaseSubscriber<T> {

                public void hookOnSubscribe(Subscription subscription) {
                    System.out.println("Subscribed");
                    request(1);
                }

                public void hookOnNext(T value) {
                    System.out.println(value);
                    request(1);
                }
            }

            ReactorSubscriber<String> reactorSubscriber = new ReactorSubscriber<String>();

            Source.from(iterable)
                    .map(String::toUpperCase)
                    .to(Sink.fromSubscriber(reactorSubscriber))
                    .run(context.getSystem());

            context.getSystem().terminate();
            return Behaviors.empty();
        });
    }

    public static void main(String[] args) {
        ActorSystem<NotUsed> system = ActorSystem.create(rootBehavior(), "ActorSystem");
        system.getWhenTerminated();
    }

}

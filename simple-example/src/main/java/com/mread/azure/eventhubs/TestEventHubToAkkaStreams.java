package com.mread.azure.eventhubs;

import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import reactor.core.publisher.Flux;

public class TestEventHubToAkkaStreams {

    public static Behavior<NotUsed> rootBehavior() {
        return Behaviors.setup(context -> {

            Flux<String> x = Flux.just("red", "white", "blue");

            Source.fromPublisher(x)
                .map(String::toUpperCase)
                .to(Sink.foreach(System.out::println))
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
package com.mread.azure.eventhubs;

import akka.Done;
import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletionStage;

public class TestEventHubToAkkaStreams {

    public static Behavior<NotUsed> rootBehavior() {
        return Behaviors.setup(context -> {

            Flux<String> x = Flux.just("red", "white", "blue");

            CompletionStage<Done> done = Source.fromPublisher(x)
                .map(String::toUpperCase)
                .toMat(Sink.foreach(System.out::println), Keep.right())
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
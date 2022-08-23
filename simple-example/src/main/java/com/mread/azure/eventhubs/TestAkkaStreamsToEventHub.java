package com.mread.azure.eventhubs;

import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.BaseSubscriber;

import java.util.Arrays;
import java.util.List;

public class TestAkkaStreamsToEventHub {
    private static final Logger log = LoggerFactory.getLogger(TestAkkaStreamsToEventHub.class);

    private static final Config appConfig = ConfigFactory.load();

    private static final String connectionString = appConfig.getString("event-hub.connection-string");
    private static final String eventHubName = appConfig.getString("event-hub.event-hub-name");
    private static final String consumerGroup = appConfig.getString("event-hub.consumer-group");

    public static Behavior<NotUsed> rootBehavior(EventHubProducerAsyncClient producer) {
        return Behaviors.setup(context -> {

            List<String> x = Arrays.asList("red", "white", "blue");
            Iterable<String> iterable = x::iterator;

            class ReactorSubscriber<T> extends BaseSubscriber<T> {

                public void hookOnSubscribe(Subscription subscription) {
                    log.info("Subscribed to ReactorSubscriber");
                    request(1);
                }

                public void hookOnNext(T value) {
                    log.info("Attempting to send a single event");
                    producer
                        .send(java.util.Collections.singleton(new EventData(value.toString())))
                        .doOnSuccess(result -> {
                            log.info("single event sent");
                            request(1);
                        })
                        .doOnError(ex -> log.error(String.format("send failed: %s", ex.getMessage())));
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
        try {
            final EventHubProducerAsyncClient producer = new EventHubClientBuilder()
                    .connectionString(connectionString, eventHubName)
                    .buildAsyncProducerClient();
            ActorSystem<NotUsed> system = ActorSystem.create(rootBehavior(producer), "ActorSystem", appConfig);
            system.getWhenTerminated();
        }
        catch (Exception ex) {
            log.error(String.format("Configuration of EventHub failed: %s", ex.getMessage()));
        }
    }

}

package com.example.javafxdemo.event;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventSubscriber {
    private static Map<EventType, List<Consumer<?>>> handlerMap = new ConcurrentHashMap<>();
    @Subscribe
    public void handleAllEvent(Event<?> event) {
            processEvent(event);
    }

    private void processEvent(Event<?> event) {
     List<Consumer<?>> consumerList=   handlerMap.get(event.getEventType());
     if(CollectionUtils.isNotEmpty(consumerList)){
         for(Consumer consumer:consumerList){
             consumer.accept(event);
         }
     }
    }


    public void registerConsumer(EventType eventType, Consumer<Event<?>> consumer){
        handlerMap.computeIfAbsent(eventType,k->new ArrayList<>());
        handlerMap.get(eventType).add(consumer);
    }
}

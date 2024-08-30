package com.example.javafxdemo.event;

import com.google.common.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DefaultEventBus {
    private static final EventBus eventBus = new EventBus();

    private List<EventSubscriber> eventSubscriberList = new ArrayList<>();
    private DefaultEventBus() {
    }
    public   <T> void sendEvent(Event<T> event){
        eventBus.post(event);
    }
    public   void register(EventSubscriber eventSubscriber){
        eventSubscriberList.add(eventSubscriber);
        eventBus.register(eventSubscriber);
    }
    public void registerConsumer(EventType eventType, Consumer<Event<?>> consumer){
        for(EventSubscriber eventSubscriber:eventSubscriberList){
            eventSubscriber.registerConsumer(eventType,consumer);
        }
    }

    public static DefaultEventBus getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static class InstanceHolder {

        private static final DefaultEventBus INSTANCE = new DefaultEventBus();
    }
}

package com.example.javafxdemo.event;


import java.util.Objects;
import java.util.UUID;

public class Event<T> {
    private String id;
    private EventType eventType;
    private T eventData;

    public Event(){
        this.id = UUID.randomUUID().toString();
    }

    public Event(EventType eventType,T eventData){
        this.id = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.eventData =eventData;

    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public T getEventData() {
        return eventData;
    }

    public void setEventData(T eventData) {
        this.eventData = eventData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event<?> event = (Event<?>) o;
        return Objects.equals(id, event.id) && eventType == event.eventType && Objects.equals(eventData, event.eventData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, eventType, eventData);
    }
}

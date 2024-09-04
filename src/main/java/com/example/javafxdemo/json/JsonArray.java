package com.example.javafxdemo.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class JsonArray implements JsonElement, Iterable<Object> {

    private final ArrayList<JsonValue<?>> list = new ArrayList<>();

    public static JsonArray load(Iterable<?> obj) {
        JsonArray jsonArray = new JsonArray();
        for(Object o : obj){
            jsonArray.add(o);
        }
        return jsonArray;
    }

    void addJsonValue(JsonValue<?> jsonValue){
        list.add(jsonValue);
    }

    void addObject(JsonObject jsonObject)
    {
        list.add(new ObjectJsonValue(jsonObject));
    }

    void addArray(JsonArray jsonArray)
    {
        list.add(new ArrayJsonValue(jsonArray));
    }

    public Iterator<JsonValue<?>> getIterator(){
        Iterator<JsonValue<?>> iterator = list.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public JsonValue<?> next() {
                return iterator.next();
            }
        };
    }

    @Override
    public String toCompressString() {
        return toCompressString(new SerializerConfiguration());
    }

    @Override
    public String toPrettyString() {
        return toPrettyString(new SerializerConfiguration());
    }

    @Override
    public String toCompressString(SerializerConfiguration configuration) {
        CompactJsonWriter writer = new CompactJsonWriter(configuration);
        writer.writeArray(this);
        return writer.toJson();
    }

    @Override
    public String toPrettyString(SerializerConfiguration configuration) {
        PrettyJsonWriter writer = new PrettyJsonWriter(configuration);
        writer.writeArray(this);
        return writer.toJson();
    }

    @Override
    public void customWrite(IJsonWriter jsonWriter) {
        jsonWriter.writeArray(this);
    }

    public void add(Object value) {
        if(value == null){
            this.addJsonValue(new ConstJsonValue("null"));
        }else if(value instanceof CharSequence){
            this.addJsonValue(new StringJsonValue(((CharSequence) value).toString()));
        }else if(value instanceof Boolean){
            this.addJsonValue(new ConstJsonValue(Boolean.TRUE.equals(value) ? "true" : "false"));
        }else if(value instanceof Integer || value instanceof Double || value instanceof Short || value instanceof Byte || value instanceof Float || value instanceof Long){
            this.addJsonValue(new ConstJsonValue(value.toString()));
        }else if(value instanceof JsonObject){
            this.addObject((JsonObject) value);
        }else if(value instanceof JsonArray){
            this.addArray((JsonArray) value);
        }else if(value instanceof Iterable){
            JsonArray jsonArray = new JsonArray();
            for (Object o : (Iterable<?>) value) {
                jsonArray.add(o);
            }
            this.addArray(jsonArray);
        }else{
            this.addObject(JsonObject.load(value));
        }
    }

    @Override
    public Iterator<Object> iterator() {
        return new JsonArrayIterator(list);
    }

    private static class JsonArrayIterator implements Iterator<Object>{

        private final Iterator<JsonValue<?>> iterator;

        public JsonArrayIterator(List<JsonValue<?>> list) {
            iterator = list.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Object next() {
            JsonValue<?> next = iterator.next();
            return next == null ? null : next.getValue();
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }
}

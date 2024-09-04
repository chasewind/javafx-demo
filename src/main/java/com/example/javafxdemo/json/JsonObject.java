package com.example.javafxdemo.json;


import com.example.javafxdemo.util.BeanUtils;

import java.util.*;

public class JsonObject implements JsonElement {

    private final LinkedHashMap<String, JsonValue<?>> kvMap = new LinkedHashMap<>();

    public static JsonObject load(Object obj) {
        JsonObject jsonObject = new JsonObject();
        if(obj instanceof Map){
            Map objMap = (Map) obj;
            Set<Map.Entry> set = objMap.entrySet();
            for (Map.Entry o : set) {
                Object key = o.getKey();
                jsonObject.put(key == null ? "null" : key.toString(), o.getValue());
            }
        }else{
            Map<String, Object> map = BeanUtils.getMap(obj);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        }
        return jsonObject;
    }

    void putJsonValue(String key, JsonValue<?> jsonValue){
        kvMap.put(key, jsonValue);
    }

    void putObject(String key, JsonObject jsonObject) {
        kvMap.put(key, new ObjectJsonValue(jsonObject));
    }

    void putArray(String key, JsonArray jsonArray) {
        kvMap.put(key, new ArrayJsonValue(jsonArray));
    }

    public Iterator<Map.Entry<String, JsonValue<?>>> getIterator() {
        Set<Map.Entry<String, JsonValue<?>>> entries = kvMap.entrySet();
        Iterator<Map.Entry<String, JsonValue<?>>> iterator = entries.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Map.Entry<String, JsonValue<?>> next() {
                Map.Entry<String, JsonValue<?>> entry = iterator.next();
                return new Map.Entry<>() {
                    @Override
                    public String getKey() {
                        return entry.getKey();
                    }

                    @Override
                    public JsonValue<?> getValue() {
                        return entry.getValue();
                    }

                    @Override
                    public JsonValue<?> setValue(JsonValue<?> value) {
                        throw new UnsupportedOperationException("setValue");
                    }
                };
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
        writer.writeObject(this);
        return writer.toJson();
    }

    @Override
    public String toPrettyString(SerializerConfiguration configuration) {
        PrettyJsonWriter writer = new PrettyJsonWriter(configuration);
        writer.writeObject(this);
        return writer.toJson();
    }

    @Override
    public void customWrite(IJsonWriter jsonWriter) {
        jsonWriter.writeObject(this);
    }

    public void put(String key, Object value) {
        if(value == null){
            this.putJsonValue(key, new ConstJsonValue("null"));
        }else if(value instanceof CharSequence){
            this.putJsonValue(key, new StringJsonValue(((CharSequence) value).toString()));
        }else if(value instanceof Boolean){
            this.putJsonValue(key, new ConstJsonValue(Boolean.TRUE.equals(value) ? "true" : "false"));
        }else if(value instanceof Integer || value instanceof Double || value instanceof Short || value instanceof Byte || value instanceof Float || value instanceof Long){
            this.putJsonValue(key, new ConstJsonValue(value.toString()));
        }else if(value instanceof JsonObject){
            this.putObject(key, (JsonObject) value);
        }else if(value instanceof JsonArray){
            this.putArray(key, (JsonArray) value);
        }else if(value instanceof Iterable){
            JsonArray jsonArray = JsonArray.load((Iterable<?>) value);
            this.putArray(key, jsonArray);
        }else{
            JsonObject jsonObject = load(value);
            this.putObject(key, jsonObject);
        }
    }

    public Object get(String key){
        JsonValue<?> jsonValue = this.kvMap.get(key);
        return jsonValue == null ? null : jsonValue.getValue();
    }

    public List<String> keys(){
        Set<Map.Entry<String, JsonValue<?>>> entries = kvMap.entrySet();
        ArrayList<String> keyList = new ArrayList<>(entries.size());
        for (Map.Entry<String, JsonValue<?>> entry : entries) {
            keyList.add(entry.getKey());
        }
        return keyList;
    }

    public boolean containsKey(String key){
        return this.kvMap.containsKey(key);
    }
}

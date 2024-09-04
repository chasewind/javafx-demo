package com.example.javafxdemo.json;

public interface IJsonWriter{

    void writeObject(JsonObject jsonObject);

    void writeArray(JsonArray array);

    String toJson();

}

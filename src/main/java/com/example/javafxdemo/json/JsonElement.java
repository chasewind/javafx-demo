package com.example.javafxdemo.json;

public interface JsonElement {

    String toCompressString();

    String toPrettyString();

    String toCompressString(SerializerConfiguration configuration);

    String toPrettyString(SerializerConfiguration configuration);

    void customWrite(IJsonWriter jsonWriter);
}

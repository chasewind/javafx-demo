package com.example.javafxdemo.json;

import java.util.List;

public class JsonOperator {

    private static final DeserializerConfiguration DEFAULT_DESERIALIZER_CONFIGURATION = new DeserializerConfiguration();

    public static JsonElement parse(String json) throws JsonParseException {
        return parse(json, DEFAULT_DESERIALIZER_CONFIGURATION);
    }

    public static JsonElement parse(String json, DeserializerConfiguration configuration) throws JsonParseException {
        // 词法分析
        List<JsonToken> tokens = JsonLexicalAnalyzer.lexicalAnalysis(json, configuration);
        if(!tokens.isEmpty()){
            JsonToken last = tokens.get(tokens.size() - 1);
            if(last.isError()){
                JsonParseUtil.triggerException(last, json);
            }
        }

        // 语法分析
        return JsonSyntacticAnalysis.syntacticAnalysis(tokens, json);
    }

    public static JsonElement load(Object obj){
        if(obj instanceof Iterable){
            return JsonArray.load((Iterable)obj);
        }else{
            return JsonObject.load(obj);
        }
    }

}

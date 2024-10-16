package com.example.javafxdemo.json;

import java.util.Iterator;
import java.util.Map;

class PrettyJsonWriter implements IJsonWriter, IJsonValueWriter{

    private final static String SPACE = " ";

    private final static String NEXT_LINE = "\n";
    //tab adjust to one space
    private final static String TAB_SPACE = " ";

    private int tab = 0;

    private final SerializerConfiguration configuration;

    public PrettyJsonWriter(SerializerConfiguration configuration) {
        this.configuration = configuration;
    }

    private final StringBuilder sb = new StringBuilder();

    @Override
    public void writeString(String str) {
        if(configuration.isEscape()){
            sb.append(JsonWord.QUOTATION).append(JsonEscapeUtils.escapeForSerialize(str)).append(JsonWord.QUOTATION);
        }else{
            sb.append(JsonWord.QUOTATION).append(str).append(JsonWord.QUOTATION);
        }
    }

    @Override
    public void writeOrigin(String origin) {
        sb.append(origin);
    }

    @Override
    public void writeObject(JsonObject jsonObject) {
        sb.append(JsonWord.OBJECT_BEGIN).append(NEXT_LINE);
        tab++;
        Iterator<Map.Entry<String, JsonValue<?>>> iterator = jsonObject.getIterator();
        if(iterator.hasNext()){
            String retract = TAB_SPACE.repeat(tab);
            while (true) {
                Map.Entry<String, JsonValue<?>> entry = iterator.next();
                if(configuration.isEscape()){
                    sb.append(retract).append(JsonWord.QUOTATION).append(JsonEscapeUtils.escapeForSerialize(entry.getKey())).append(JsonWord.QUOTATION)
                            .append(JsonWord.COLON).append(SPACE);
                }else{
                    sb.append(retract).append(JsonWord.QUOTATION).append(entry.getKey()).append(JsonWord.QUOTATION)
                            .append(JsonWord.COLON).append(SPACE);
                }
                entry.getValue().write(this);
                if(iterator.hasNext()){
                    sb.append(JsonWord.COMMA).append(NEXT_LINE);
                }else{
                    sb.append(NEXT_LINE);
                    break;
                }
            }
        }
        tab--;
        sb.append(TAB_SPACE.repeat(tab)).append(JsonWord.OBJECT_END);
    }

    @Override
    public void writeArray(JsonArray array) {
        sb.append(JsonWord.ARRAY_BEGIN).append(NEXT_LINE);
        tab++;
        Iterator<JsonValue<?>> iterator = array.getIterator();
        if(iterator.hasNext()){
            String retract = TAB_SPACE.repeat(tab);
            while (true) {
                JsonValue<?> jsonValue = iterator.next();
                sb.append(retract);
                jsonValue.write(this);
                if(iterator.hasNext()){
                    sb.append(JsonWord.COMMA).append(NEXT_LINE);
                }else{
                    sb.append(NEXT_LINE);
                    break;
                }
            }
        }
        tab--;
        sb.append(TAB_SPACE.repeat(tab)).append(JsonWord.ARRAY_END);
    }

    @Override
    public String toJson() {
        return sb.toString();
    }
}

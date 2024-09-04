package com.example.javafxdemo.components;

import org.fxmisc.richtext.CodeArea;

import java.util.HashMap;

public class CustomCodeArea extends CodeArea {

    private final CustomCodeAreaContext context = new CustomCodeAreaContext();

    public CustomCodeAreaContext getContext(){
        return context;
    }

    public static class CustomCodeAreaContext {
        private final HashMap<Class, Object> variable = new HashMap<>();

        @SuppressWarnings("unchecked")
        public <T> T getVariable(Class<T> clazz){
            return (T) variable.get(clazz);
        }
        public <T> void put(Class<T> clazz, T value){
            variable.put(clazz, value);
        }
    }

}
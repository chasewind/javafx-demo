package com.example.javafxdemo.json;

public class DeserializerConfiguration {

    private boolean escape = true;

    private boolean abortWhenIncorrect = true;

    public boolean isEscape() {
        return escape;
    }

    public void setEscape(boolean escape) {
        this.escape = escape;
    }

    public boolean isAbortWhenIncorrect() {
        return abortWhenIncorrect;
    }

    public void setAbortWhenIncorrect(boolean abortWhenIncorrect) {
        this.abortWhenIncorrect = abortWhenIncorrect;
    }
}

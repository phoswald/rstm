package com.github.phoswald.rstm.databind;

class Deferred<T> {

    private boolean defined;
    private T value;

    void define(T value) {
        if (defined) {
            throw new IllegalStateException("value already defined");
        }
        this.defined = true;
        this.value = value;
    }

    T access() {
        if (!defined) {
            throw new IllegalStateException("value not yet defined");
        }
        return value;
    }
}

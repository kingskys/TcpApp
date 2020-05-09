package com.kingskys.tcp.common;

public class Value<T> {
    private T value;
    public void set(T v) {
        value = v;
    }

    public T get() {
        return value;
    }
}

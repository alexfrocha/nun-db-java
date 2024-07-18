package com.alexfrocha.data;

public class LocalValue {
    public Value value;
    private int version;
    private boolean pending;

    public LocalValue(Value value, int version, boolean pending) {
        this.value = value;
        this.version = version;
        this.pending = pending;
    }

    public Value getValue() {
        return value;
    }

    public int getVersion() {
        return version;
    }

    public boolean isPending() {
        return pending;
    }

}

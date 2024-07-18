package com.alexfrocha.data;

public class Value {
    private long id;
    private String value;

    public Value(long id, String value) {
        this.id = id;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }


    @Override
    public String toString() {
        return value;
    }

    public static Value fromString(String str) {
        String[] parts = str.split(":");
        long id = Long.parseLong(parts[0]);
        String value = parts[1];
        return new Value(id, value);
    }

}

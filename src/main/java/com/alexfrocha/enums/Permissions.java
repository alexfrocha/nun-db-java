package com.alexfrocha.enums;

public enum Permissions {
    READ("r"),
    WRITE("w"),
    INCREMENT("i"),
    REMOVE("x");

    private String value;
    Permissions(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static boolean contains(String permissions, Permissions permission) {
        return permissions.contains(permission.getValue());
    };

}

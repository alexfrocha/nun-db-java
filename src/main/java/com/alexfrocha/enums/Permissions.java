package com.alexfrocha.enums;

public enum Permissions {
    READ('q'),
    WRITE('w'),
    INCREMENT('i'),
    REMOVE('x');

    private char value;
    Permissions(char value) {
        this.value = value;
    }

    public char getValue() {
        return this.value;
    }

    public static boolean contains(String permissions, Permissions permission) {
        return permissions.contains("" + permission.getValue());
    };

}

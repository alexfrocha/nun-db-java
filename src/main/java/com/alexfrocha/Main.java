package com.alexfrocha;

public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012", "alex", "alex");
        db.createDb("josebinho", "josefina");
        db.goOffline();
    }
}
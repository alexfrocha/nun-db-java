package com.alexfrocha;

public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012", "alex", "alex");
        db.useDb("balacobaco2", "123");

        Object a = db.get("teste").thenApply(values -> {
            return values;
        }).join();

        Object b = db.get("testsadasdasdde").thenApply(values -> {
            return values;
        }).join();

        System.out.println(a);
        System.out.println(b);

    }
}
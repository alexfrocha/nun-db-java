package com.alexfrocha;

public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012", "alex", "alex");

        db.createDb("123123123123123", "josefina");

        db.useDb("123123123123123", "josefina").thenAccept(result -> {
            System.out.println("Resultado de useDb(): " + result);
        }).exceptionally(e -> {
            System.err.println("Erro ao chamar useDb(): " + e.getMessage());
            return null;
        });

        db.goOffline();
    }
}
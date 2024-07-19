package com.alexfrocha;

import com.alexfrocha.async.interfaces.Watcher;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012", "alex", "alex");
        db.showLogs(true);
        db.useDb("aware", "aware");

        db.set("dizoiprovideo", "1");

        db.increment("dizoiprovideo", "20");

        db.addWatch("dizoiprovideo", e -> {
           System.out.println("[WATCH] 'dizoiprovideo' foi alterado: " + e);
        });

        while (true) {}
    }
}
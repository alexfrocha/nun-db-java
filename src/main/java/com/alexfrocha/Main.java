package com.alexfrocha;

import com.alexfrocha.enums.Permissions;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012", "alex", "alex");
//        db.showLogs(true);
//        db.useDb("aware", "aware");
        db.addWatch("dizoiprovideo", novoValor -> {
            System.out.println("o valor 'dizoiprovideo' agora virou: " + novoValor);});
        db.set("dizoiprovideo", "oi video 1");

        db.set("dizoiprovideo", "oi video 2");


        List<String> databases = db.getAllDatabases().join();
        System.out.println(databases);

        while(true) {}
    }
}
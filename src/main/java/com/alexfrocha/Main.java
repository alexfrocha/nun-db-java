package com.alexfrocha;

import com.alexfrocha.async.interfaces.Watcher;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012", "alex", "alex");
//        db.showLogs(true);
        db.useDb("aware", "aware");
        Watcher print = System.out::println;
        List<String> keys = db.keys("").join();
        System.out.println(keys);
    }
}
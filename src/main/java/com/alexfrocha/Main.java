package com.alexfrocha;

import com.alexfrocha.async.interfaces.Watcher;
import com.alexfrocha.data.LocalValue;

import javax.security.auth.callback.Callback;

public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012", "alex", "alex");
        db.useDb("aware", "aware");
        Watcher print = v -> {
            System.out.println("valor de voting: " + v);
        };
        db.addWatch("teste", print);

        while (true) {}
    }
}
package com.alexfrocha;

import com.alexfrocha.async.interfaces.Watcher;
import com.alexfrocha.enums.Permissions;

import java.util.List;

// Should go to a test project
public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012/", "user-name", "user-pwd");
//        db.showLogs(true);
        db.useDb("aware", "aware");
        db.addWatch("remove2", e -> {
            System.out.println("valor de remove2 mudou para: " + e);
        });

        db.addWatch("teste2", e -> {
            System.out.println("valor de teste2 mudou para: " + e);
        });

        db.remove("teste2");
        db.remove("test");
        db.remove("value");
        db.remove("remove2");
        db.remove("after");
        db.remove("age");
        db.remove("km");
        db.remove("remove");

        while(true) {}
    }
}

package com.alexfrocha;

import com.alexfrocha.async.interfaces.Watcher;
import com.alexfrocha.enums.Permissions;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012/", "user-name", "user-pwd");
//        db.showLogs(true);
        db.createDb("aware", "aware");
        db.useDb("aware2", "aware");
        db.addWatch("teste", e -> {
            System.out.println("watch: " + e);
        });
        db.set("teste", "123123");
        db.set("teste", "123");
        db.set("teste", "8128");
        while(true) {}
    }
}
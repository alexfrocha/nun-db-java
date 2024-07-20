package com.alexfrocha;

import com.alexfrocha.async.interfaces.Watcher;
import com.alexfrocha.enums.Permissions;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012/", "user-name", "user-pwd");
//        db.createDb("aware", "aware");
//        db.showLogs(true);
        db.useDb("aware", "aware");
        db.addWatch("teste", e -> {
            System.out.println("valor alterou: " + e);
        });
        db.set("teste", "123123");
        db.set("teste", "123");
        db.set("teste", "8128");
        System.out.println("addm: " + db.get("teste").join());
        System.out.println("databases: " + db.getAllDatabases().join());
        while(true) {}
    }
}
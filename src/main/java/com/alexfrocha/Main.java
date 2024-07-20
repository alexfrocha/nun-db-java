package com.alexfrocha;

import com.alexfrocha.async.interfaces.Watcher;
import com.alexfrocha.enums.Permissions;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        NunDB db = new NunDB("ws://localhost:3012/", "user-name", "user-pwd");
        db.createDb("aware", "aware");
        db.showLogs(true);
    }
}
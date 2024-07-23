package com.alexfrocha.test;

import com.alexfrocha.NunDB;
import com.alexfrocha.async.interfaces.Watcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


// i don't know how to do performance, my metrics is just the ms of each second waits to be done, sorry about that =)
class NunDBPerformance {

    private static final NunDB nun = new NunDB("ws://localhost:3012/", "user-name", "user-pwd");

    @Test
    void getTestBeforeSettingValue() {
        nun.createDb("aware", "aware");
        nun.useDb("aware", "aware");
        Object value = nun.get("before").join();
        assertTrue(true);
    }

    @Test
    void getTestAfterSettingValue() {
        nun.createDb("aware", "aware");
        nun.useDb("aware", "aware");
        nun.set("after", "ok");
        Object value = nun.get("after").join();
        assertTrue(true);
    }


    @Test
    void testIncrement() {
        nun.createDb("aware", "aware");
        nun.useDb("aware", "aware");
        nun.set("age", "17");
        nun.increment("age", 1);
        assertTrue(true);
    }


    @Test
    void testGetKeys() {
        nun.createDb("aware", "aware");
        nun.useDb("aware", "aware");
        nun.allKeys().join();
        nun.keysContains("es").join();
        nun.keysStartingWith("t").join();
        nun.keysEndingWith("st").join();
        assertTrue(true);
    }

    @Test
    void testGetAllDatabases() {
        List<String> allDatabases = nun.getAllDatabases().join();
        assertTrue(true);
    }
}

package com.alexfrocha.test;

import com.alexfrocha.NunDB;
import com.alexfrocha.async.interfaces.Watcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;


class NunDBTest {

    private static final NunDB nun = new NunDB("ws://localhost:3012/", "user-name", "user-pwd");

    @Test
    @DisplayName("Getting value before setting a value for it")
    void getTestBeforeSettingValue() {
        nun.createDb("aware", "aware");
        nun.useDb("aware", "aware");
        Object value = nun.get("before").join();
        assertEquals("<Empty>\n", value.toString());
    }

    @Test
    @DisplayName("Getting value after setting a value for it")
    void getTestAfterSettingValue() {
        nun.createDb("aware", "aware");
        nun.useDb("aware", "aware");
        nun.set("after", "ok");
        Object value = nun.get("after").join();
        assertEquals("ok\n", value.toString());
    }

    @Test
    @DisplayName("Watching value in realtime")
    void testingWatchingValue() throws InterruptedException {
        List<Object> cacheFromWatcher = new ArrayList<>();
        nun.createDb("aware", "aware");
        nun.useDb("aware", "aware");

        Watcher saveThis = e -> {
            cacheFromWatcher.add(e);
        };

        CompletableFuture<Void> kmWatcher = nun.addWatch("km", saveThis);
        nun.set("km", "2");
        nun.set("km", "3");
        nun.set("km", "4");
        nun.set("km", "5");

        // just a way to delay the code and the async watcher be completed before the code run is finished
        nun.get("km").join();
        nun.get("km").join();
        nun.get("km").join();
        nun.get("km").join();
        nun.get("km").join();


        assertEquals(4, cacheFromWatcher.size());
        assertEquals(true, cacheFromWatcher.containsAll(Arrays.asList("2", "3", "4", "5")));
    }

    @Test
    @DisplayName("Incrementing value")
    void testIncrement() {
        nun.createDb("aware", "aware");
        nun.useDb("aware", "aware");

        nun.set("age", "17");
        nun.increment("age", 1);

        Object incrementedValue = nun.get("age").join();
        assertEquals("18\n", incrementedValue.toString());
    }

    @Test
    @DisplayName("Removing watcher")
    void testRemoveWatcher() {
        nun.createDb("aware", "aware");
        nun.useDb("aware", "aware");

        List<Object> cacheFromWatcher = new ArrayList<>();

        Watcher saveThis = e -> {
            cacheFromWatcher.add(e);
        };

        nun.addWatch("remove", saveThis);
        nun.set("remove", "initial");

        // just a way to delay the code and the async watcher be completed before the code run is finished
        nun.get("remove").join();
        nun.get("remove").join();
        nun.get("remove").join();

        nun.removeWatcher("remove");

        nun.set("remove", "modified");


        assertEquals(1, cacheFromWatcher.size());
        assertEquals("initial", cacheFromWatcher.get(0));
    }

    @Test
    @DisplayName("Getting keys")
    void testGetKeys() {
        nun.createDb("aware", "aware");
        nun.useDb("aware", "aware");

        nun.set("test", "123");

        List<String> allKeys = nun.allKeys().join();
        List<String> keysContaining = nun.keysContains("es").join();
        List<String> keysStartingWith = nun.keysStartingWith("t").join();
        List<String> keysEndingWith = nun.keysEndingWith("st").join();
        List<String> expectedKeys = Arrays.asList("$$token", "$connections", "after", "age", "km", "remove", "test");

        assertEquals(expectedKeys.toString(), allKeys.toString().replace("\n", ""));
        assertEquals("[test\n]", keysContaining.toString());
        assertEquals("[test\n]", keysStartingWith.toString());
        assertEquals("[test\n]", keysEndingWith.toString());
    }

    @Test
    @DisplayName("Getting all databases")
    void testGetAllDatabases() {
        List<String> allDatabases = nun.getAllDatabases().join();
        assertEquals(Arrays.asList("$admin", "aware"), allDatabases);
    }
}

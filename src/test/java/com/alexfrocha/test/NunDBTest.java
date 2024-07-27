package com.alexfrocha.test;

import com.alexfrocha.NunDB;
import com.alexfrocha.async.interfaces.Watcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit ;

import java.time.Duration;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;


class NunDBTest {

    private static final String host = System.getenv("NUNDB_HOST");
    private static final String user = System.getenv("NUNDB_USER");
    private static final String pwd = System.getenv("NUNDB_PWD");

    private static final NunDB nun = new NunDB(host, user, pwd);

    @Test
    @DisplayName("Getting value before setting a value for it")
    void getTestBeforeSettingValue() {
        //assertEquals(host, "mateus1");
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
        assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
            CompletableFuture<String> waitForWatchers = new CompletableFuture<>();

            List<Object> cacheFromWatcher = new ArrayList<>();
            nun.createDb("aware", "aware");
            nun.useDb("aware", "aware");

            Watcher saveThis = e -> {
                cacheFromWatcher.add(e);
                if (cacheFromWatcher.size() == 4) {
                    waitForWatchers.complete("done");
                }
            };

            CompletableFuture<Void> kmWatcher = nun.addWatch("km", saveThis);
            nun.set("km", "2");
            nun.set("km", "3");
            nun.set("km", "4");
            nun.set("km", "5");
            waitForWatchers.join();
            assertEquals(4, cacheFromWatcher.size());
            assertEquals(true, cacheFromWatcher.containsAll(Arrays.asList("2", "3", "4", "5")));
        });
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
        //nun.createDb("aware", "aware");
        assertTimeoutPreemptively(Duration.ofMillis(1000), () -> {
            CompletableFuture<String> waitForWatchers = new CompletableFuture<>();
            //nun.createDb("aware", "aware");
            nun.useDb("aware", "aware");

            List<Object> cacheFromWatcher = new ArrayList<>();

            Watcher saveThis = e -> {
                System.out.println("here we are po/");
                cacheFromWatcher.add(e);
                waitForWatchers.complete("done");
            };

            nun.addWatch("remove", saveThis);
            nun.set("remove", "initial");
            Thread.sleep(1);
            nun.removeWatcher("remove");
            nun.set("remove", "modified");
            waitForWatchers.join();
            assertEquals(1, cacheFromWatcher.size());
            assertEquals("initial", cacheFromWatcher.get(0));
        });
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
        assertTrue(allDatabases.containsAll(Arrays.asList("$admin", "aware")));
    }
}

package com.alexfrocha.handlers;

import com.alexfrocha.async.PendingPromise;
import com.alexfrocha.async.interfaces.ExecuteAllWatchersCallback;
import com.alexfrocha.data.Value;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ResponseHandler {
    public static void noDatabaseSelected(String message) {
        if (message.contains("no-db-selected")) {
            throw new IllegalStateException("Please select a database with useDb()");
        }
    }
    public static void noValidDatabaseName(String message) {
        if (message.contains("Not a valid database name")) {
            throw new IllegalStateException("Database not found on useDb()");
        }
    }
    public static void invalidAuth(String message) {
        if (message.contains("invalid auth")) {
            throw new IllegalStateException("Invalid auth!");
        }
    }
    public static void allDatabases(String command, String[] messageParts, List<PendingPromise> pendingPromises) {
        if ("dbs-list".equals(command)) {
            String payload = messageParts.length > 1 ? messageParts[1] : "";
            String[] rawDatabases = payload.split("\n");

            List<String> databases = Arrays.stream(rawDatabases)
                    .map(db -> db.split(" : ")[0])
                    .collect(Collectors.toList());

            pendingPromises.stream()
                    .filter(promise -> "dbs-list".equals(promise.getCommand()))
                    .forEach(promise -> {
                        promise.getPromise().complete(databases);
                    });
        }
    }
    public static void clusterState(String command, String[] messageParts, List<PendingPromise> pendingPromises) {
        if ("cluster-state".equals(command)) {
            String payload = messageParts.length > 1 ? messageParts[1] : "";
            String[] rawClusters = payload.replace(" ", "").replace(",", "").split(",");

            List<String> clusters = Arrays.stream(rawClusters)
                    .filter(part -> !part.isEmpty())
                    .collect(Collectors.toList());

            pendingPromises.stream()
                    .filter(promise -> "cluster-state".equals(promise.getCommand()))
                    .forEach(promise -> {promise.getPromise().complete(clusters);});
        }
    }
    public static void keys(String command, String[] messageParts, List<PendingPromise> pendingPromises) {
        if ("keys".equals(command)) {
            String payload = messageParts.length > 1 ? messageParts[1] : "";
            String[] rawParts = payload.split(",");

            List<String> keys = Arrays.stream(rawParts)
                    .filter(part -> !part.isEmpty())
                    .collect(Collectors.toList());

            pendingPromises.stream()
                    .filter(promise -> "keys".equals(promise.getCommand()))
                    .forEach(promise -> {
                        promise.getPromise().complete(keys);
                    });
        }
    }
    public static void gettingValues(String command, String[] messageParts, List<PendingPromise> pendingPromises) {
        if ("value-version".equals(command)) {
            String payload = messageParts.length > 1 ? messageParts[1] : "";
            String[] parts = payload.split("\\s+", 2);
            String key = parts[1];
            int version = parts.length > 1 ? Integer.parseInt(parts[0]) : -1;
            pendingPromises.stream()
                    .filter(promise -> promise.getCommand().equals("get-safe"))
                    .forEach(promise -> {
                        Object value = new Value(version, key);
                        promise.getPromise().complete(value);
                    });
        }
    }
    public static void watchingValues(String command, String[] messageParts, List<PendingPromise> pendingPromises, ExecuteAllWatchersCallback executeAllWatchers) {
        if ("changed".equals(command)) {
            String payloads = messageParts.length > 1 ? messageParts[1] : "";
            String[] parts = payloads.split("\\s+");
            String value = parts[(int) (Arrays.stream(parts).count() - 1)];
            pendingPromises.stream()
                    .filter(promise -> promise.getCommand().equals("watch-sent"))
                    .forEach(promise -> {
                        executeAllWatchers.execute(promise.getKey(), value);
                    });
        }
    }
}

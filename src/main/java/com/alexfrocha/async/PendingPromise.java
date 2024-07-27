package com.alexfrocha.async;

import java.util.concurrent.CompletableFuture;
// I would change it to something more Java-like
public class PendingPromise {
    private String key;
    private String command;
    private CompletableFuture<Object> promise;

    public PendingPromise(String key, String command) {
        this.key = key;
        this.command = command;
        this.promise = new CompletableFuture<>();
    }

    public String getKey() {
        return key;
    }

    public String getCommand() {
        return command;
    }

    public CompletableFuture<Object> getPromise() {
        return promise;
    }
}

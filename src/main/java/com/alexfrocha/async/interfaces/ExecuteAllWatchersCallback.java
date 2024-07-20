package com.alexfrocha.async.interfaces;

@FunctionalInterface
public interface ExecuteAllWatchersCallback {
    void execute(String key, Object data);
}

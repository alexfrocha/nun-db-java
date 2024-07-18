package com.alexfrocha.async.interfaces;

@FunctionalInterface
public interface Watcher {
    void apply(Object data);
}

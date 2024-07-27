package com.alexfrocha;

import com.alexfrocha.data.LocalValue;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class LocalStorage {
    private static final Logger logger = Logger.getLogger(LocalStorage.class.getName());

    public static void setItem(String key, LocalValue value) {
        String jsonValue = new Gson().toJson(value);
        try {
            Files.write(Paths.get(key + ".json"), jsonValue.getBytes());
        } catch (IOException e) {
            logger.severe("Error on trying to store local value: " + e.getMessage());
        }
    }

    public static LocalValue getItem(String key) {
        try {
            String jsonValue = new String(Files.readAllBytes(Paths.get(key + ".json")));
            return new Gson().fromJson(jsonValue, LocalValue.class);
        } catch (IOException e) {
            logger.severe("Error on trying to get local value: " + e.getMessage());
            // Better to fail than to lie
            return null;
        }
    }

}

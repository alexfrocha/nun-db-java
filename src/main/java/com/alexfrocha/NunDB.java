package com.alexfrocha;

import com.google.gson.Gson;

import javax.websocket.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ClientEndpoint
public class NunDB {

    public static final int RECONNECT_TIME = 10;
    private static final String EMPTY = "<Empty>";

    private Logger logger = Logger.getLogger(NunDB.class.getName());

    private Session session;
    private String databaseURL;
    private String databaseName;
    private String databaseToken;
    private String user;
    private String password;

    private List<Long> ids = new ArrayList<>();
    private List<PendingPromise> pendingPromises = new ArrayList<>();
    private CompletableFuture<Void> connectionPromise;
    private Boolean shouldReconnect = false;


    public NunDB(String databaseURL, String user, String password) {
        this.databaseURL = databaseURL;
        this.user = user;
        this.password = password;
        this.connect();
    }

    public NunDB(String databaseURL, String databaseName, String databaseToken, String user, String password) {
        this.databaseURL = databaseURL;
        this.databaseName = databaseName;
        this.databaseToken = databaseToken;
        this.user = user;
        this.password = password;
        this.connect();
    }

    public void connect() {
        this.connectionPromise = new CompletableFuture<>();
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(this.databaseURL));
            this.auth(this.user, this.password);
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    private PendingPromise createPendingPromise(String key, String command) {
        PendingPromise pendingPromise = new PendingPromise(key, command);
        this.pendingPromises.add(pendingPromise);
        return pendingPromise;
    }

    private void checkIfConnectionIsReady() {
        if(this.session == null) {
            logger.severe("CONNECTION IS NOT READY");
            throw new IllegalStateException("Connection is not ready");
        }
    }

    // WEBSOCKET HANDLER

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.setupEvents();
        logger.info("WebSocket ID: " + session.getId());
        this.connectionPromise.complete(null);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if (this.session.isOpen()) {
            logger.severe(("WebSocket error: " + throwable.getMessage()));
        } else {
            this.reConnect();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        this.session = null;
        logger.severe("Closed Websocket cause? " + closeReason);
        this.reConnect();
    }

    private void setupEvents() {
        this.session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                messageHandler(message);
            }
        });

    }

    private void messageHandler(String message) {
        logger.info("Message received: " + message);
        String[] messageParts = message.split("\\s(.+)|\n");
        String command = messageParts[0];
        String value = messageParts.length > 1 ? messageParts[1] : null;
        String commandMethodName = commandToFunction(command);
        try {
            Method method = this.getClass().getDeclaredMethod(commandMethodName, String.class);
            method.invoke(this, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.severe(commandMethodName + " Handler not implemented");
        }

    }

    private String commandToFunction(String command) {
        return Arrays.stream(command.trim().split("-/\\s"))
                .map(c -> c.substring(0, 1).toUpperCase() + c.substring(1))
                .collect(Collectors.joining(""));
    }

    private String objToValue(Object obj) {
        return obj.toString().replaceAll("\\s", "^");
    }

    private Object valueToObject(String value) {
        return (value != null && !value.equals(EMPTY)) ? new Gson().fromJson(value.replace("^", " "), Object.class) : null;
    }

    //

    public void goOffline() {
        this.shouldReconnect = false;
        try {
            this.session.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void goOnline() {
        this.shouldReconnect = true;
        this.connect();
    }

    private void reConnect() {
        if (this.shouldReconnect) {
            try {
                Thread.sleep(RECONNECT_TIME);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            this.connect();
        }
    }

    public CompletableFuture<Void> createDb(String name, String token) {
        checkIfConnectionIsReady();
        return this.connectionPromise.thenCompose(v -> {
            this.session.getAsyncRemote().sendText("create-db " + name + " " + token);
            logger.info( name + " DB CREATED");
            this.databaseName = name;
            this.databaseToken = token;
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> auth(String user, String password) {
        checkIfConnectionIsReady();
        return this.connectionPromise.thenCompose(v -> {
            this.session.getAsyncRemote().sendText("auth " + user + " " + password);
            logger.info("LOGGED [" + user + ", " + password + "]");
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Object> useDb(String database, String token) {
        checkIfConnectionIsReady();
        return this.connectionPromise.thenCompose(v -> {
           String command = "use-db " + database + " " + token;
           this.session.getAsyncRemote().sendText(command);
           this.databaseName = database;
           this.databaseToken = token;
           PendingPromise pendingPromise = this.createPendingPromise(this.databaseName, "use-db");
           return pendingPromise.getPromise();
        });
    }

}
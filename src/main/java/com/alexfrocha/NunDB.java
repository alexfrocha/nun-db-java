package com.alexfrocha;

import javax.websocket.*;
import java.awt.*;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@ClientEndpoint
public class NunDB {

    public static final int RECONNECT_TIME = 10;

    private Logger logger = Logger.getLogger(NunDB.class.getName());

    private Session session;
    private String databaseURL;
    private String databaseName;
    private String databaseToken;
    private String user;
    private String password;

    private CompletableFuture<Void> connectionPromise;
    private Boolean shouldReconnect = false;


    public NunDB(String databaseURL, String user, String password) {
        this.databaseURL = databaseURL;
        this.user = user;
        this.password = password;
        this.connect();
    }

    public void connect() {
        this.connectionPromise = new CompletableFuture<>();
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            Session connection = container.connectToServer(this, new URI(this.databaseURL));
            this.auth(this.user, this.password);
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }


    // WEBSOCKET HANDLER

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
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
        return this.connectionPromise.thenCompose(v -> {
            this.session.getAsyncRemote().sendText("create-db " + name + " " + token);
            logger.info( name + " DB CREATED");
            return CompletableFuture.completedFuture(null);
        });
    }

    private CompletableFuture<Void> auth(String user, String password) {
        return this.connectionPromise.thenCompose(v -> {
            this.session.getAsyncRemote().sendText("auth " + user + " " + password);
            logger.info("LOGGED [" + user + ", " + password + "]");
            return CompletableFuture.completedFuture(null);
        });
    }

}
package com.alexfrocha;

import com.alexfrocha.async.PendingPromise;
import com.alexfrocha.data.LocalValue;
import com.alexfrocha.data.Value;
import com.google.gson.Gson;

import javax.websocket.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ClientEndpoint
public class NunDB {
    public static final int RECONNECT_TIME = 10;
    private static final String EMPTY = "<Empty>";
    private static final boolean shouldStoreLocal = true;
    private static final String LAST_SERVER_KEY = "nundb_$$last_server_";
    private static final Map<String, LocalValue> memoryDatabase = new HashMap<>();

    private Logger logger = Logger.getLogger(NunDB.class.getName());

    private Session session;
    private String databaseURL;
    private String databaseName;
    private String databaseToken;
    private String user;
    private String password;

    private int messages = 0;
    private long start = System.currentTimeMillis();
    private List<Long> ids = new ArrayList<>();
    private List<PendingPromise> pendingPromises = new ArrayList<>();
    public CompletableFuture<Void> connectionPromise;
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
//        logger.info("Message received: " + message);
        String[] messageParts = message.split("\\s+", 2); // Divide a mensagem em duas partes no primeiro espaço em branco
        String command = messageParts[0];

        if ("value-version".equals(command)) {
            String payload = messageParts.length > 1 ? messageParts[1] : "";
            String[] parts = payload.split("\\s+", 2);
            String key = parts[1]; // Chave recebida
            int version = parts.length > 1 ? Integer.parseInt(parts[0]) : -1; // Versão recebida

            // Encontrar a Promise correspondente para completar
            pendingPromises.stream()
                    .filter(promise -> promise.getCommand().equals("get-safe") )
                    .forEach(promise -> {
                        Object value = new Value(-1, key); // Criar o objeto de valor apropriado
                        promise.getPromise().complete(value); // Completar a Promise com o valor recebido
                    });

        }
    }

    private String commandToFunction(String command) {
        return Arrays.stream(command.trim().split("-/\\s"))
                .map(c -> c.substring(0, 1).toUpperCase() + c.substring(1))
                .collect(Collectors.joining(""));
    }

    private void resolvePendingValue(String key, int version) {
        LocalValue localValue = memoryDatabase.get(key);
        if (localValue != null && localValue.isPending() && localValue.getVersion() == version) {
            this.storeLocalValue(key, new LocalValue(localValue.getValue(), version, false));
        }
    }

    private void storeLocalValue(String key, LocalValue value) {
        if(shouldStoreLocal) {
            LocalStorage.setItem(key, value);
        }
        if(shouldStoreLocal && value != null && !value.isPending()) {
            LocalStorage.setItem(LAST_SERVER_KEY + key, value);
        }
        memoryDatabase.put(key, value);
    }

    private LocalValue getLocalValue(String key) {
        if (shouldStoreLocal) {
            return LocalStorage.getItem(key);
        }
        return null;
    }

    private String objToValue(Object obj) {
        if (obj instanceof Value) {
            return ((Value) obj).toString();
        }
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

    private long nextMessageId() {
        this.messages += 1;
        return this.start + this.messages;
    }

    public CompletableFuture<Object> getValueSafe(String key) {
        CompletableFuture<Object> resultPromise = new CompletableFuture<>();
        // Criar e registrar uma nova promise para este get-safe
        PendingPromise pendingPromise = createPendingPromise(key, "get-safe");
        pendingPromises.add(pendingPromise);

        // Enviar comando para o servidor
        this.connectionPromise.thenAccept(v -> {
            this.session.getAsyncRemote().sendText("get-safe " + key);
        });

        // Aguardar a conclusão da promise correspondente no messageHandler
        pendingPromise.getPromise().thenAccept(value -> {
            resolvePendingValue(key, -1); // Ajustar o valor de versão conforme necessário
            resultPromise.complete(value); // Completar a promise do resultado com o valor recebido
        });

        // Criar e registrar uma nova promise para confirmar o envio do get-safe
        PendingPromise pendingPromiseAck = createPendingPromise(key, "get-safe-sent");
        pendingPromises.add(pendingPromiseAck);

        // Aguardar a conclusão da promise correspondente no messageHandler
        pendingPromiseAck.getPromise().thenRun(() -> {
            // Apenas para confirmar que a mensagem foi enviada
            logger.info("get-safe message sent for key: " + key);
        });

        // Combinar ambas as promises para esperar a conclusão de ambas antes de retornar
        CompletableFuture.allOf(pendingPromise.getPromise(), pendingPromiseAck.getPromise())
                .thenApply(values -> resultPromise.join());

        return resultPromise;
    }


    public CompletableFuture<Object> get(String key) {
        return this.getValueSafe(key);
    }

    public CompletableFuture<List<String>> keys(String prefix) {
        checkIfConnectionIsReady();
        return this.connectionPromise.thenCompose(v -> {
            this.session.getAsyncRemote().sendText("keys " + prefix);
            PendingPromise pendingPromise = this.createPendingPromise(prefix, "keys");
            PendingPromise pendingPromiseAck = this.createPendingPromise(prefix, "keys-sent");
            return CompletableFuture.allOf(pendingPromise.getPromise(), pendingPromiseAck.getPromise()).thenApply(values -> {
                Object result = pendingPromise.getPromise().join();
                if (result instanceof List) {
                    return (List<String>) result;
                } else {
                    throw new IllegalStateException("Expected List<String> but got " + result.getClass().getName());
                }
            });
        });
    }



    public CompletableFuture<Void> set(String name, String value) {
        return this.setValue(name, value);
    }

    public CompletableFuture<Void> setValue(String name, String value) {
        checkIfConnectionIsReady();
        return this.setValueSafe(name, value, -1, false);
    }

    public CompletableFuture<Void> setValueSafe(String name, String value, int version, boolean basicType) {
        checkIfConnectionIsReady();
        LocalValue localValue = this.getLocalValue(name);
        Value objValue = new Value(this.nextMessageId(), value);
        int ver = localValue == null ? version : localValue.getVersion();
        this.storeLocalValue(name, new LocalValue(objValue, ver, true));
        this.ids.add(objValue.getId());
        return this.connectionPromise.thenCompose(v -> {
            String command = "set-safe " + name + " " + ver + " " + (basicType ? value : objToValue(objValue));
            this.session.getAsyncRemote().sendText(command);
            PendingPromise pendingPromise = this.createPendingPromise(name, "set");
            return pendingPromise.getPromise().thenApply(res -> {
                this.resolvePendingValue(name, ver);
                return null;
            });
        });
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

    public CompletableFuture<Object> useDb(String name, String token) {
        checkIfConnectionIsReady();
        return this.connectionPromise.thenCompose(v -> {
           String command = "use-db " + name + " " + token;
           this.session.getAsyncRemote().sendText(command);
           this.databaseName = name;
           this.databaseToken = token;
           PendingPromise pendingPromise = this.createPendingPromise(this.databaseName, "use-db");
           return pendingPromise.getPromise();
        });
    }

}
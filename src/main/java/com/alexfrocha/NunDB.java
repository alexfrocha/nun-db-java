package com.alexfrocha;

import com.alexfrocha.async.PendingPromise;
import com.alexfrocha.async.interfaces.Watcher;
import com.alexfrocha.data.LocalValue;
import com.alexfrocha.data.Value;
import com.alexfrocha.enums.Permissions;
import com.alexfrocha.handlers.ResponseHandler;
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
    private boolean isArbiter = false;
    private List<Long> ids = new ArrayList<>();
    private List<PendingPromise> pendingPromises = new ArrayList<>();
    private Boolean shouldReconnect = false;
    private Map<String, List<Watcher>> watchers = new HashMap<>();
    private CompletableFuture<Void> connectionPromise;

    private boolean shouldShowLogs = false;

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
        this.useDb(this.databaseName, this.databaseToken);
        this.connect();
    }

    private void connect() {
        this.connectionPromise = new CompletableFuture<>();
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(this.databaseURL));
            this.auth(this.user, this.password);
        } catch (Exception e) {
            // danger
            logger.severe(e.getMessage());
        }
    }

    private void sendCommand(String command) {
        if (command == null) {
            logger.severe("INSERT A COMMAND! NOT A NULL");
            // Throw exception
            return;
        }
        this.session.getAsyncRemote().sendText(command);
    }

    public void showLogs(boolean status) {
        this.shouldShowLogs = status;
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

    private void checkIfDatabaseIsCreated() {
        if(this.databaseName == null && this.databaseToken == null) {
            logger.severe("Connect to a DB with useDb(name, token)");
            throw new IllegalStateException("Connect to a DB with useDb(name, token)");
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
            throw new IllegalStateException(throwable.getMessage());
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

        if(shouldShowLogs && !message.startsWith("ok")) logger.info("received message: " + message);

        String[] messageParts = message.split("\\s+", 2);
        String command = messageParts[0];

        ResponseHandler.invalidAuth(message);
        ResponseHandler.noValidDatabaseName(message);
        ResponseHandler.noDatabaseSelected(message);

        ResponseHandler.allDatabases(command, messageParts, pendingPromises);
        ResponseHandler.clusterState(command, messageParts, pendingPromises);
        ResponseHandler.keys(command, messageParts, pendingPromises);
        ResponseHandler.gettingValues(command, messageParts, pendingPromises);
        ResponseHandler.watchingValues(command, messageParts, pendingPromises, this::executeAllWatchers);
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

    public void goOffline() {
        this.shouldReconnect = false;
        try {
            this.session.close();
        } catch (Exception e) {
            // Change all to logger
            System.out.println(e.getMessage());
        }
    }

    public void goOnline() {
        this.shouldReconnect = true;
        this.connect();
    }

    public CompletableFuture<Void> addWatch(String name, Watcher cb) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.connectionPromise.thenCompose(v -> {
            this.sendCommand("watch " + name);
            this.createPendingPromise(name, "watch-sent");
            this.watchers.computeIfAbsent(name, k -> new ArrayList<>()).add(cb);
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> removeAllWatchers() {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.connectionPromise.thenCompose(v -> {
            this.sendCommand("unwatch-all");
            this.watchers.clear();
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> removeWatcher(String name) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.connectionPromise.thenCompose(v -> {
            this.sendCommand("unwatch " + name);
            this.watchers.remove(name);
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> increment(String name, Number value) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.connectionPromise.thenCompose(v -> {
            this.sendCommand("increment " + name + " " + value);
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> remove(String key) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.connectionPromise.thenCompose(v -> {
            this.sendCommand("remove " + key);
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> showWatchers() {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.connectionPromise.thenCompose(v -> {
            System.out.println(this.watchers);
            return CompletableFuture.completedFuture(null);
        });
    }

    private void executeAllWatchers(String key, Object data) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        List<Watcher> watchersList = this.watchers.get(key);
        if (watchersList != null) {
            for (Watcher cb : watchersList) {
                cb.apply(data);
            }
        }
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
        checkIfConnectionIsReady();
        CompletableFuture<Object> resultPromise = new CompletableFuture<>();
        PendingPromise pendingPromise = this.createPendingPromise(key, "get-safe");
        pendingPromises.add(pendingPromise);

        this.connectionPromise.thenAccept(v -> {
            this.sendCommand("get-safe " + key);
        });

        pendingPromise.getPromise().thenAccept(value -> {
            resolvePendingValue(key, -1);
            resultPromise.complete(value);
        });

        PendingPromise pendingPromiseAck = this.createPendingPromise(key, "get-safe-sent");
        pendingPromises.add(pendingPromiseAck);

        pendingPromiseAck.getPromise().thenRun(() -> {
            if (this.shouldShowLogs)
                logger.info("get-safe message sent for key: " + key);
        });

        CompletableFuture.allOf(pendingPromise.getPromise(), pendingPromiseAck.getPromise())
                .thenApply(values -> resultPromise.join());

        return resultPromise;
    }

    public CompletableFuture<Void> createUser(String username, String password) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.connectionPromise.thenCompose(v -> {
            this.sendCommand("create-user " + username + " " + password);
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> setPermissions(String username, String payload) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.connectionPromise.thenCompose(v -> {
            this.sendCommand("set-permissions " + payload);
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> setPermissions(String username, String key, Permissions... permissions) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.connectionPromise.thenCompose(v -> {
            List<String> permissionsValues = Arrays.stream(permissions).map(e -> e.getValue()).collect(Collectors.toList());
            String command = "set-permissions " + username + " " + String.join("", permissionsValues) + " " + key;
            this.sendCommand(command);
            return CompletableFuture.completedFuture(null);
        });
    }


    public CompletableFuture<Object> get(String key) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.getValueSafe(key);
    }

    public CompletableFuture<List<String>> getAllDatabases() {
        checkIfConnectionIsReady();
        CompletableFuture<List<String>> resultPromise = new CompletableFuture<>();
        PendingPromise pendingPromise = this.createPendingPromise("", "dbs-list");
        this.sendCommand("debug list-dbs");
        pendingPromise.getPromise().thenAccept(result -> {
            resultPromise.complete((List<String>) result);
        });
        return resultPromise;
    }

    public CompletableFuture<List<String>> allKeys() {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        CompletableFuture<List<String>> resultPromise = new CompletableFuture<>();
        PendingPromise pendingPromise = this.createPendingPromise("", "keys");
        this.sendCommand("keys ");
        pendingPromise.getPromise().thenAccept(result -> {
            resultPromise.complete((List<String>) result);
        });
        return resultPromise;
    }

    public CompletableFuture<List<String>> keysStartingWith(String prefix) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        CompletableFuture<List<String>> resultPromise = new CompletableFuture<>();
        PendingPromise pendingPromise = this.createPendingPromise(prefix, "keys");
        this.sendCommand("keys " + prefix + "*");
        pendingPromise.getPromise().thenAccept(result -> {
            resultPromise.complete((List<String>) result);
        });
        return resultPromise;
    }

    public CompletableFuture<List<String>> keysEndingWith(String suffix) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        CompletableFuture<List<String>> resultPromise = new CompletableFuture<>();
        PendingPromise pendingPromise = this.createPendingPromise(suffix, "keys");
        this.sendCommand("keys *" + suffix);
        pendingPromise.getPromise().thenAccept(result -> {
            resultPromise.complete((List<String>) result);
        });
        return resultPromise;
    }

    public CompletableFuture<List<String>> keysContains(String supposedText) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        CompletableFuture<List<String>> resultPromise = new CompletableFuture<>();
        PendingPromise pendingPromise = this.createPendingPromise(supposedText, "keys");
        this.sendCommand("keys " + supposedText);
        pendingPromise.getPromise().thenAccept(result -> {
            resultPromise.complete((List<String>) result);
        });
        return resultPromise;
    }


    public CompletableFuture<Object> getClusterState() {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        CompletableFuture<Object> resultPromise = new CompletableFuture<>();
        PendingPromise pendingPromise = this.createPendingPromise("", "cluster-state");
        pendingPromises.add(pendingPromise);
        this.sendCommand("cluster-state");
        pendingPromise.getPromise().thenAccept(result -> {
            resultPromise.complete(result);
        });
        return resultPromise;
    }



    public CompletableFuture<Void> set(String name, String value) {
        return this.setValue(name, value);
    }

    public CompletableFuture<Void> setValue(String name, String value) {
        return this.setValueSafe(name, value, -1, false);
    }

    public CompletableFuture<Void> setValueSafe(String name, String value, int version, boolean basicType) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        LocalValue localValue = this.getLocalValue(name);
        Value objValue = new Value(this.nextMessageId(), value);
        int ver = localValue == null ? version : localValue.getVersion();
        this.storeLocalValue(name, new LocalValue(objValue, ver, true));
        this.ids.add(objValue.getId());
        String command = "set-safe " + name + " " + ver + " " + (basicType ? value : objToValue(objValue));
        this.sendCommand(command);
        PendingPromise pendingPromise = this.createPendingPromise(name, "set");
        return pendingPromise.getPromise().thenApply(res -> {
            this.resolvePendingValue(name, ver);
            return null;
        });
    }

    public CompletableFuture<Void> createDb(String name, String token) {
        checkIfConnectionIsReady();
        List<String> databases = getAllDatabases().join();
        return this.connectionPromise.thenCompose(v -> {
            if(!databases.contains(name)) {
                this.sendCommand("create-db " + name + " " + token);
                logger.info( name + " DB CREATED");
                this.databaseName = name;
                this.databaseToken = token;
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    private CompletableFuture<Void> auth(String user, String password) {
        checkIfConnectionIsReady();
        return this.connectionPromise.thenCompose(v -> {
            this.sendCommand("auth " + user + " " + password);
            logger.info("LOGGED [**********, ********]");
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Object> useDb(String name, String token) {
        checkIfConnectionIsReady();
        return this.connectionPromise.thenCompose(v -> {
           String command = "use-db " + name + " " + token;
           this.sendCommand(command);
           this.databaseName = name;
           this.databaseToken = token;
           PendingPromise pendingPromise = this.createPendingPromise(this.databaseName, "use-db");
           return pendingPromise.getPromise();
        });
    }

    public CompletableFuture<Void> snapshot(boolean reclaimSpace, String... databases) {
        checkIfConnectionIsReady();
        return this.connectionPromise.thenCompose(v -> {
            this.sendCommand("snapshot " + reclaimSpace + " " + String.join("|", databases));
            return CompletableFuture.completedFuture(null);
        });
    }

    public CompletableFuture<Void> snapshot(boolean reclaimSpace) {
        checkIfConnectionIsReady();
        checkIfDatabaseIsCreated();
        return this.connectionPromise.thenCompose(v -> {
            this.sendCommand("snapshot " + reclaimSpace + " " + this.databaseName);
            return CompletableFuture.completedFuture(null);
        });


    }

}

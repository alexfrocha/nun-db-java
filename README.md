## Nun DB Java Client
Just to make it clear, this is not a official client of nun db, is just a fan-made client =)


I did not upload on maven/gradle for now, but i'll do, i promise.

Starting the instance of the client:
```java
NunDB db = new NunDB("ws://ws.nundb.org", "name", "password");
db.createDb("databaseName", "databaseToken") // if you don't have a database
db.useDb("databaseName", "databaseToken");
```
Working with some data:
```java
db.set("test", "123");
Object test = db.get("test").join(); // 123
db.remove("test"); // 'test' deleted
```
Working with realtime data:
```java
db.addWatch("test", newValue -> {
    System.out.println("new value of 'test': " + newValue);
});

db.set("test", "12"); // new value of 'test': 12
db.set("test", "32"); // new value of 'test': 32
db.set("test", "45"); // new value of 'test': 45
```
A elegant way to do that too:
```java
Watcher print = e -> {
    System.out.println("new value of 'test': " + e);
};

db.addWatch("test", print);
db.set("test", "12"); // new value of 'test': 12
db.set("test", "32"); // new value of 'test': 32
db.set("test", "45"); // new value of 'test': 45
```
How to stop watching?
```java
db.removeWatcher("test"); // for a specific key
db.removeAllWatchers(); // for all
```
How to get any key that u want:
```java
db.allKeys(); // [test]
db.keysContains("es"); // [test]
db.keysEndingWith("est"); // [test]
db.keysStartingWith("e"); // []
```
How to get all database available:
```java
db.getAllDatabases(); // [$admin, $databaseName]
```
How to increment a value?
```java
db.set("age", "17");
if(birthday) {
  db.increment("age", 1);
  db.get("age").join(); // 18
}
```
How to create a user?
```java
db.createUser("joao", "123")
```
Im still confused what this do, but i implemented:
```java
db.snapshot(true, "db1", "db2", "db3", "db4", "db5", "db6") // countless databases if u want to
db.snapshot(true) // when just boolean is given, the snapshot take the actual database in use
```
### [WARNING]: I'm still working on Permissions, so dont occurs the permission verification per method yet
How to set permissions of a user?
```java
db.setPermissions("jorge", "test", Permissions.WRITE, Permissions.READ); // more semantic way, but only can do for one key
db.setPermissions("jorge", "rw test"); // mr. robot way, but u can do like a lot of keys, ex: "rw test|ri age"
```
Struggling with some problem? try this and check it:
```java
db.showLogs(true)
```

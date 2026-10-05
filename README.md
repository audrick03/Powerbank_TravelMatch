tangina ni Gian Patol.

## User accounts

The application stores accounts in `travelmatch.db` in the working directory.
The SQLite JDBC driver is declared in `pom.xml`; build and run the application
with Maven:

```sh
mvn compile
mvn exec:java -Dexec.mainClass=Main
```

The repository creates its `users` table on startup. Demo accounts are
`admin` / `admin123` and `user` / `user123`. New account passwords are stored
as salted PBKDF2 hashes.

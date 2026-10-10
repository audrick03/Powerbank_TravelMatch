## User accounts

The application stores accounts in `travelmatch.db` in the working directory.
The SQLite JDBC driver is declared in `pom.xml`; build and run the application
with Maven:

```sh
mvn compile exec:java
```

The repository creates its `users` table on startup. Demo accounts are
`admin` / `admin123` and `user` / `user123`. New account passwords are stored
as salted PBKDF2 hashes.

## How to download Maven

Windows - Open PowerShell or Command Prompt as an Administrator and run:

```sh
winget install Apache.Maven
```

Mac - Open Terminal and run:
```sh
brew install maven
```

Or download manually using this link: [Apache Maven Project](https://maven.apache.org/download.cgi)

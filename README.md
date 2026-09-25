# RetailEdge Cart Abandonment Recovery

This project is a Spring Boot application for tracking abandoned carts, scanning for recovery opportunities, and managing stock updates.

## Prerequisites

Before running the project locally, make sure you have the following installed:

- Java 17+
- Maven 3.9+
- Docker Desktop or Docker Engine
- Git

## 1. Start PostgreSQL with Docker

From the project root, run:

```bash
docker compose up -d db
```

This starts a PostgreSQL container with:

- Database: `retailedge`
- Username: `retailedge`
- Password: `retailedge`
- Port: `5432`

The database initialization scripts in `db/init` will run automatically when the container is created.

## 2. Configure environment variables

The application reads database and JWT settings from environment variables. Set them before starting the app.



### Windows PowerShell

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/retailedge"
$env:DATABASE_USERNAME = "retailedge"
$env:DATABASE_PASSWORD = "retailedge"
$env:JWT_ISSUER_URI = "https://your-issuer.example.com"
$env:JWT_JWK_SET_URI = "https://your-issuer.example.com/.well-known/jwks.json"
$env:JWT_AUDIENCE = "retailedge-api"
$env:PORT = "8080"
```

> If you are using a local or test JWT provider, replace the issuer and JWK values with your actual values.

## 3. Build the application

To create a deployable JAR file, run:

```bash
mvn clean package
```

This creates the packaged application in:

```text
target/retailedge-cart-abandonment-recovery-0.0.1-SNAPSHOT.jar
```

You can also run the JAR directly with:

```bash
java -jar target/retailedge-cart-abandonment-recovery-0.0.1-SNAPSHOT.jar
```

## 4. Run the application

From the project root:

```bash
mvn spring-boot:run
```

The application will start on:

```text
http://localhost:8080
```

## 5. Health check

A public health endpoint is available:

```bash
curl http://localhost:8080/api/v1/health
```

## 6. Run tests

```bash
docker compose up -d db
mvn test
```

## Project structure

```text
.
├── db/
│   └── init/
│       └── init.sql
├── src/
│   ├── main/
│   └── test/
├── docker-compose.yml
├── pom.xml
├── README.md
└── .gitignore
```

## Troubleshooting

### Docker fails to start

Make sure Docker Desktop is running and your machine supports Docker.

### Application fails to start with JWT errors

Check that the following environment variables are set correctly:

- `JWT_ISSUER_URI`
- `JWT_JWK_SET_URI`
- `JWT_AUDIENCE`

### Database connection errors

Make sure PostgreSQL is running and the values match:

- Host: `localhost`
- Port: `5432`
- Database: `retailedge`
- Username: `retailedge`
- Password: `retailedge`

## Notes

- The project is configured for Spring Boot 3 and Java 17.
- Database schema is initialized through `db/init/init.sql`.
- The app uses PostgreSQL in local development and H2 in test profile configuration.

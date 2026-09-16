# Database

The `db` directory contains database-related resources for StudyQueue.

StudyQueue uses **PostgreSQL** for persistent application data and **Flyway** to manage versioned schema migrations.

## Purpose

The database stores the persistent state required by the StudyQueue learning system.

This includes data such as:

* concepts,
* prerequisite relationships,
* questions,
* question metadata,
* learner attempts,
* performance statistics,
* and concept progress.

The database supports the application but does not define StudyQueue's learning rules. Those rules belong in the domain layer.

## Database Technology

StudyQueue currently uses:

* **PostgreSQL 17**
* **Docker Compose**
* **Flyway**

PostgreSQL provides the primary relational datastore.

Docker Compose provides a reproducible local database environment.

Flyway manages schema changes through ordered migration files.

## Local Development

The PostgreSQL development database is started from the project root using:

```bash
docker compose up -d
```

To verify that the container is running:

```bash
docker compose ps
```

Database connection values are supplied through environment variables.

A local `.env` file may be used for development, based on the committed `.env.example` template.

Example:

```env
POSTGRES_DB=studyqueue
POSTGRES_USER=studyqueue
POSTGRES_PASSWORD=your_local_password
```

The real `.env` file should not be committed to Git.

## Database Access

The Spring Boot application connects to PostgreSQL using the configured datasource settings.

At a high level:

```text
Spring Boot
    ↓
Repository / Persistence Layer
    ↓
PostgreSQL
```

Application and domain code should not contain direct database-specific logic unless it

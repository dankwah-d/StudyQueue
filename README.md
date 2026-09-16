# StudyQueue

StudyQueue is an adaptive learning application that organizes course material as a graph of concepts and prerequisite relationships.

Instead of presenting learners with a fixed sequence of questions, StudyQueue tracks performance by concept and uses that information to determine what material should be studied next.

The initial implementation focuses on **Operating Systems**, but the architecture is intended to support additional subjects and learning domains.

## Core Workflow

StudyQueue is built around a continuous learning loop:

```text
Question
   ↓
Attempt
   ↓
Evaluation
   ↓
Statistics
   ↓
Concept State
   ↓
Selection Strategy
   ↓
Next Question
```

Each learner interaction produces evidence that updates the system's understanding of the learner's progress.

That updated state is then used to choose the next appropriate question or concept.

## Concept Graph

Course material is modeled as a graph.

* **Nodes** represent concepts or skills.
* **Edges** represent prerequisite relationships.
* **Questions** are associated with concepts.
* **Learner progress** is tracked at the concept level.

For example:

```text
Process Concept
      |
      v
Process Scheduling
      |
      +----------------+
      |                |
      v                v
Process Operations    IPC
                       |
                 +-----+-----+
                 |           |
                 v           v
            Shared Memory  Message Passing
```

This structure allows StudyQueue to determine which concepts are available, which concepts depend on earlier material, and which areas may require additional practice.

## Project Goals

StudyQueue is designed to:

* track learning progress by concept,
* represent prerequisite relationships explicitly,
* record and evaluate question attempts,
* calculate performance statistics,
* maintain learner state for individual concepts,
* adapt question selection based on learner performance,
* support interchangeable selection strategies,
* and visualize progress through a concept graph.

## Current Scope

The first major development milestone is a complete question-processing vertical slice:

```text
Question
→ Attempt
→ Evaluator
→ Statistics
→ Concept State
→ Selection Strategy
→ Next Question
```

The initial implementation uses multiple-choice questions and establishes the core data flow required for later adaptive-learning behavior.

Future iterations may include additional question types, mastery rules, difficulty modeling, study-session generation, and more advanced selection algorithms.

## Architecture

StudyQueue is organized into several major application areas:

```text
src/
├── app/
├── domain/
└── infrastructure/
```

### `domain`

Contains the core learning model and business rules.

Examples include:

* questions,
* attempts,
* concepts,
* prerequisite relationships,
* evaluation,
* statistics,
* concept state,
* and selection strategies.

### `app`

Contains application workflows and use-case coordination.

This layer connects domain components to perform operations such as submitting an answer, updating learner progress, and selecting the next question.

### `infrastructure`

Contains technical implementations such as:

* database access,
* persistence,
* repository implementations,
* configuration,
* and external integrations.

More detailed architecture documentation is maintained under `docs/architecture`.

## Technology Stack

StudyQueue currently uses:

* **Java**
* **Spring Boot**
* **PostgreSQL**
* **Docker Compose**
* **Flyway**
* **Maven**
* **Git / GitHub**

### Spring Boot

Provides the backend application framework and application services.

### PostgreSQL

Stores persistent StudyQueue data, including concepts, questions, prerequisite relationships, attempts, and learner progress.

### Docker Compose

Provides a reproducible local PostgreSQL development environment.

### Flyway

Manages database schema migrations so database changes can be versioned alongside the application source code.

## Repository Structure

```text
StudyQueue/
├── db/
├── docs/
│   ├── api/
│   └── architecture/
├── src/
│   ├── app/
│   ├── domain/
│   └── infrastructure/
├── compose.yaml
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .env.example
└── README.md
```

### `db/`

Contains database-related resources and migration support.

### `docs/`

Contains project documentation, including architecture descriptions, API documentation, diagrams, and design decisions.

### `src/`

Contains the StudyQueue application source code.

## Initial Learning Domain

Operating Systems is the first subject being modeled in StudyQueue.

Example concepts include:

* processes,
* process scheduling,
* process operations,
* interprocess communication,
* shared memory,
* message passing,
* threads,
* synchronization,
* deadlock,
* memory management,
* paging,
* virtual memory,
* and storage systems.

Operating Systems serves as the first test domain for the StudyQueue architecture rather than a permanent limitation of the system.

## Development Approach

StudyQueue is being developed incrementally through vertical slices.

The current development path is approximately:

```text
Question Storage
      ↓
Question Retrieval
      ↓
Attempt Recording
      ↓
Evaluation
      ↓
Statistics
      ↓
Concept State
      ↓
Selection Strategy
      ↓
Concept Graph Navigation
      ↓
Progress Visualization
```

Each stage is intended to produce a working and testable part of the system before additional complexity is introduced.

## Current Status

StudyQueue is currently under active development.

The project currently includes:

* Spring Boot application setup,
* PostgreSQL,
* Docker Compose,
* database connectivity,
* Flyway migrations,
* repository organization,
* and the initial system architecture.

Current work is focused on building the persistent domain model and completing the first end-to-end question-processing workflow.

## Documentation

Additional documentation is available in the `docs/` directory.

The root README provides a high-level overview of the project. More detailed architecture, subsystem, database, and API documentation is maintained closer to the relevant parts of the repository.



<img width="1536" height="942" alt="image" src="https://github.com/user-attachments/assets/ee1a36ee-783a-49ee-b854-cf8a6955ed3f" />

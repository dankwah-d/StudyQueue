# Domain

The `domain` package contains the core learning model and business rules of StudyQueue.

It represents the concepts that define how StudyQueue works independently of databases, APIs, user interfaces, or other infrastructure.

## Responsibilities

The domain layer is responsible for modeling:

* questions,
* attempts,
* concepts,
* prerequisite relationships,
* evaluation,
* learner statistics,
* concept state,
* and question-selection strategies.

The domain should contain the rules required to determine how learner activity affects StudyQueue's learning model.

## Key Components

Current and planned domain components include:

```text
Question
Attempt
Evaluator
Statistics
Concept
Concept State
Selection Strategy
Concept Graph
```

These components form the core StudyQueue learning pipeline:

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
```

## Dependencies

The domain layer should remain as independent as practical from implementation technologies.

Domain logic should not require knowledge of:

* PostgreSQL,
* HTTP,
* REST controllers,
* Docker,
* database connection details,
* or user-interface frameworks.

Other layers may depend on the domain.

The domain should avoid depending on those layers.

## Boundaries

Code belongs in the domain layer when it represents a StudyQueue concept or learning rule.

Database implementations, API controllers, framework configuration, and other technical integration code belong elsewhere.

## Current Status

Development is currently focused on establishing the domain objects required for the first complete question-processing pipeline.

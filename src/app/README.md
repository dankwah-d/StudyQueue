# Application

The `app` package contains StudyQueue's application workflows and use cases.

Its purpose is to coordinate domain components to perform complete operations requested by the rest of the system.

## Responsibilities

The application layer coordinates workflows such as:

```text
Submit Answer
      ↓
Create Attempt
      ↓
Evaluate Attempt
      ↓
Update Statistics
      ↓
Update Concept State
      ↓
Select Next Question
```

It determines the order in which domain operations occur without containing the underlying learning rules itself.

## Key Components

Application-level components may include:

* use-case services,
* command handlers,
* query handlers,
* workflow coordinators,
* application services,
* and interfaces required to access persistence or external systems.

Example use cases include:

* retrieving a question,
* submitting an answer,
* recording an attempt,
* calculating updated learner state,
* and requesting the next question.

## Dependencies

The application layer may depend on the domain layer.

When persistence or external functionality is required, the application layer should preferably depend on interfaces rather than infrastructure implementations.

```text
Application
     |
     v
   Domain

Application
     |
     v
Interface / Port
     |
     v
Infrastructure Implementation
```

## Boundaries

Core learning rules belong in the domain layer.

Database implementations, framework configuration, and other technical details belong in the infrastructure layer.

The application layer primarily coordinates these components.

## Current Status

The initial application workflow is centered on the question-processing vertical slice from question retrieval through next-question selection.

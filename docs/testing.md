# Running tests

Frontend checks need Node.js:

```powershell
node --test tests/concept-stats.test.cjs tests/practice-session.test.cjs
```

Integration tests use the `test` Spring profile and a dedicated PostgreSQL database on loopback port 5434. They do not use the development database on port 5433. Start Docker Desktop, then run:

```powershell
docker compose -p studyqueue-tests -f compose.test.yaml up -d --wait
.\mvnw.cmd test
docker compose -p studyqueue-tests -f compose.test.yaml down
```

The test database uses temporary in-memory container storage. Stopping the service discards its data. Flyway initializes it on startup. Do not override test datasource settings with development or production credentials.

Checks that do not need PostgreSQL:

```powershell
.\mvnw.cmd '-Dtest=ConceptStateTest,StatsControllerTest,AnswerOptionsTest' test
```

# Answer retries

`POST /api/questions/{id}/answer` accepts an optional UUID `submissionId` alongside `optionId`. Reuse the same UUID and option for retries of one submission; the API returns its stored evaluation without inserting another attempt. Reusing a UUID for another question or option returns HTTP 409. Use a new UUID for intentional further practice. Clients omitting the UUID retain the legacy behavior of recording each request as a new attempt.

The practice page retains the UUID and original choice after an ambiguous failure, disables option changes, and retries the same payload. It clears the pending payload after a definite client rejection or when loading a new question. The page does not persist pending submissions across reloads.

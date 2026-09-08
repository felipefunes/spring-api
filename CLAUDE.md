# CLAUDE.md

Shared project context for anyone (human or Claude Code) working in this repo.

## What this is

Orbital Watcher: a Spring Boot API that reports the ISS's current position enriched with reverse-geocoded location and current weather at that location, fetched concurrently from external APIs.

## Stack

Java 21, Spring Boot, Maven (`mvnw`), virtual threads enabled for concurrent external calls.

## Commands

- `./mvnw spring-boot:run` — run locally
- `./mvnw test` — run tests
- `./mvnw clean package` — build

## Conventions

- Gitflow: no direct pushes to `main`; all changes via PR.
- Commits/PRs authored as Felipe Funes (felipefunes@gmail.com).
- Public repo: MIT-licensed; docs/comments in English.
- Secrets (e.g. `OPENWEATHER_API_KEY`) are read from environment variables (see `application.yml`), never hardcoded.

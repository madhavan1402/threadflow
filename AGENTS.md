
# ThreadFlow - Agent Instructions

## Product
Build a production order-tracking platform for a knitwear
exporter in Tiruppur.

## Technology
- Frontend: React, TypeScript, Vite
- Backend: Java 21, Spring Boot
- Database: MySQL 8
- ORM: Spring Data JPA
- Migrations: Flyway
- Authentication: Spring Security and JWT
- Offline: IndexedDB and sync queue

## Product rules
- Follow the client brief. Do not invent integrations.
- Build complete working vertical features.
- Never use mock data as a substitute for backend functionality.
- Seed realistic sample orders from the client brief.
- Preserve original Tamil notes.
- AI classifications must be editable.
- Never expose one buyer's data to another buyer.
- Record every ex-factory date change.
- Never tell buyers about unconfirmed delay warnings.

## Frontend quality
- Create a distinctive, professional manufacturing UI.
- Follow the approved design system.
- Mobile-first supervisor experience.
- Desktop-first merchandiser workspace.
- Responsive read-only buyer portal.
- Support loading, empty, error and success states.
- No dead buttons or decorative controls.
- Avoid generic dashboard templates.
- Accessible contrast, labels and keyboard navigation.

## Backend quality
- Validate all external inputs.
- Enforce role and buyer-level access on the server.
- Use transactions for critical updates.
- Use database migrations.
- Avoid exposing sensitive information in logs.
- Write tests for all important business rules.

## Workflow
1. Inspect the existing code before editing.
2. State the implementation plan.
3. Implement one vertical slice at a time.
4. Run relevant tests and builds.
5. Fix failures before marking work complete.
6. Report changed files, tests and known limitations.
7. Commit completed work with meaningful messages.

Never claim a feature works without testing it.

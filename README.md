# MISH APP — Frontend

![MISH_LOGO](src/main/webapp/icons/MISH_icon.ico "MISH APP Logo")

[![Java Tests](https://github.com/zlesak/threejsproofofconcept/actions/workflows/java-tests.yml/badge.svg?branch=main)](https://github.com/zlesak/threejsproofofconcept/actions/workflows/java-tests.yml)
[![Vitest](https://github.com/zlesak/threejsproofofconcept/actions/workflows/vitest.yml/badge.svg?branch=main)](https://github.com/zlesak/threejsproofofconcept/actions/workflows/vitest.yml)

## Description

This repository contains the frontend part of the MISH APP application that has been created as a part of a master thesis at the University of Hradec Kralove.  
Provides a web-based user interface for interaction with anatomical 3D models.
Code in this repository is based on Vaadin framework for the UI part and Three.js for 3D rendering.  
The backend part is located in a separate repository: https://github.com/Foglas/mishprototype.

## Running the application

To run the application with back end in action, there has been made a separate repository MISH SCRIPTS making it easy to launch both front end and back end.  
More information can be found in the MISH SCRIPTS repository.  
Link to MISH SCRIPTS repository: https://github.com/zlesak/MISH_SCRIPTS
## Screenshots

### Desktop

![Main page — desktop](docs/screenshots/main-pc.png)

![3D model viewer — desktop](docs/screenshots/model-pc.png)

![Chapter detail — desktop](docs/screenshots/chapter-pc.png)

![Quiz detail — desktop](docs/screenshots/quiz-pc.png)

### Mobile

| Main page | 3D Model viewer | Chapter detail | Quiz detail |
|-----------|-----------------|----------------|-------------|
| ![Main page — mobile](docs/screenshots/main-mobile.png) | ![3D model viewer — mobile](docs/screenshots/model-mobile.png) | ![Chapter detail — mobile](docs/screenshots/chapter-mobile.png) | ![Quiz detail — mobile](docs/screenshots/quiz-mobile.png) |

## Testing

### Unit and component tests (Vitest)

```bash
npm run test
```

### E2E tests (Playwright)

```bash
npx playwright test
```

### Three.js canvas performance tests

```bash
npx playwright test e2e/threejs-canvas-perf.spec.ts
```

Results are written to `test-results/threejs-perf-results.json` after each run.

### Java backend tests (Maven)

```bash
./mvnw test
```

## Project structure

```
src/
├── main/
│   ├── java/cz/uhk/zlesak/threejslearningapp/
│   │   ├── api/
│   │   │   ├── clients/              # REST API clients (chapter, model, quiz, quiz result, documentation)
│   │   │   └── contracts/            # API definitions
│   │   ├── common/                   # Shared utilities
│   │   ├── components/               # Reusable Vaadin UI components
│   │   │   ├── buttons/              # Action buttons
│   │   │   ├── commonComponents/     # Shared commmon components
│   │   │   ├── containers/           # Composite layout containers (model, quiz, chapter, upload, …)
│   │   │   ├── dialogs/              # Confirmation and entity-list dialogs
│   │   │   ├── editors/              # Text (editor.js) and quiz question editors
│   │   │   ├── forms/                # Form components for create/edit flows
│   │   │   ├── inputs/               # Selects, file inputs, filters and text fields
│   │   │   ├── listItems/            # Entity card/list-item components for chapters, models and quizzes
│   │   │   ├── notifications/        # Toast notifications (success, error, warning, info, cookies)
│   │   │   ├── quizComponents/       # Quiz renderers and question-type UI components
│   │   │   └── scrollers/            # Scrollable wrappers
│   │   ├── controllers/              # View controllers (logout)
│   │   ├── domain/                   # Domain model
│   │   │   ├── common/               # Shared interfaces and base types
│   │   │   ├── chapter/              # Chapter and sub-chapter entities and filters
│   │   │   ├── documentation/        # Documentation entry entities and index
│   │   │   ├── model/                # 3D model entities and value objects
│   │   │   ├── parsers/              # Data parsers (model listing, texture listing)
│   │   │   ├── quiz/                 # Quiz, question and answer entities
│   │   │   └── texture/              # Texture and area value objects
│   │   ├── events/                   # Application events
│   │   │   ├── chapter/              # Chapter selection events
│   │   │   ├── file/                 # File upload/remove events
│   │   │   ├── model/                # Model selection events
│   │   │   ├── quiz/                 # Quiz and answer events
│   │   │   └── threejs/              # Three.js action events (show, remove, switch texture, …)
│   │   ├── exceptions/               # Custom exception classes
│   │   ├── i18n/                     # Internationalisation (CustomI18NProvider, I18nAware interface)
│   │   ├── security/                 # Security configuration
│   │   ├── services/                 # Domain services (chapter, model, quiz, quiz result, documentation)
│   │   └── views/                    # Vaadin views
│   │       ├── abstractViews/        # Abstract base views (listing, entity, chapter, model, quiz)
│   │       ├── administration/       # Administration centre view
│   │       ├── chapter/              # Chapter detail and create/edit views
│   │       ├── documentation/        # Documentation view
│   │       ├── error/                # Error pages
│   │       ├── model/                # 3D model viewer and create/edit views
│   │       └── quizes/               # Quiz list, detail, play and result views
│   ├── frontend/
│   │   ├── js/
│   │   │   ├── editorjs/             # Editor.js integration
│   │   │   └── threejs/              # Three.js integration
│   │   ├── themes/                   # CSS themes and styles
│   │   └── types/                    # TypeScript type definitions for JS libraries
│   ├── resources/                    # Spring application configuration, doc and i18n text files
│   └── webapp/                       # Static web assets
└── test/
    ├── java/                         # JUnit / Karibu / Spring tests
    └── resources/                    # Test fixtures and configuration
e2e/                                  # Playwright E2E and performance tests
```


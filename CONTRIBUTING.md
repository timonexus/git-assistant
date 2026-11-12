# Contributing to Git Assistant

Thank you for your interest in contributing to Git Assistant!

Contributions of all kinds are welcome, including bug reports, feature
requests, documentation improvements and code contributions.

## Getting Started

### Requirements

Before contributing, make sure you have installed:

- Java JDK 25 or newer
- Apache Maven 3.9 or newer
- Git
- Ollama (only required for manual AI testing)

### Setup

1. Fork the repository.
2. Clone your fork:

   ```bash
   git clone https://github.com/YOUR_USERNAME/git-assistant.git
````

3. Navigate into the project:
   ```
   cd git-assistant
   ```
4. Verify the project:
   ```
   mvn clean verify
   ```

## Development Guidelines

Please follow these principles:

- Keep changes focused and easy to understand.
- Use clear and descriptive names.
- Avoid unnecessary dependencies.
- Handle errors explicitly.
- Add or update tests when changing functionality.
- Do not include unrelated changes.

## Code Style

- Follow standard Java naming conventions.
- Use meaningful class, method and variable names.
- Keep methods reasonably small.
- Prefer readable code over unnecessary complexity.

## Testing

Before submitting a pull request, run:

```
mvn clean verify
```

All tests must pass.

Tests should not depend on external services being available.\
Use mocks or local test servers where appropriate.

## Commit Messages

Use Conventional Commits:

```
type(scope): description
```

Examples:

```
feat: add configuration support
fix: handle empty Git diff
test: add Ollama service tests
docs: improve installation instructions
```

## Pull Requests

1. Create a feature branch.
2. Implement your changes.
3. Run the tests.
4. Commit your changes.
5. Push your branch.
6. Open a pull request.

Please provide a clear description of your changes and link related issues.

## Reporting Bugs

Use the Bug Report issue template.

## Suggesting Features

Use the Feature Request issue template.

Thank you for helping improve Git Assistant!

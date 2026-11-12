
# Git Assistant

**AI-powered Git commit messages, right from your terminal.**

Git Assistant analyzes your Git changes and uses locally running Ollama models to generate meaningful, professional commit messages. Review the suggestion, confirm it, and let Git Assistant create the commit for you.

## Features

- **AI-powered commit messages** – Generate commit messages based on actual code changes.
- **Local AI with Ollama** – Use locally running language models instead of relying on a cloud AI service.
- **File selection** – Choose which changed files should be included in the analysis.
- **Conventional Commits** – Generate structured commit messages following the Conventional Commits convention.
- **Human approval** – Review and approve every suggested commit before it is created.
- **Command-line interface** – Work directly from your terminal.

## Requirements

Before using Git Assistant, make sure you have:

- Java 25 or later
- Git
- [Ollama](https://ollama.com/)
- Maven (for building from source)

## AI Model

Git Assistant currently uses:

- **Runtime:** Ollama
- **Default model:** `qwen3:4b`
- **API endpoint:** `http://localhost:11434/api/chat`

Make sure Ollama is running and the model is available:

```bash
ollama pull qwen3:4b
```

You can test the model with:

```bash
ollama run qwen3:4b
```

## Usage

Generate a commit message:

```bash
git-assistant commit
```

Display the available commands:

```bash
git-assistant help
```

Git Assistant will:

1. Detect changes in the current Git repository.
2. Display the changed files.
3. Let you select the files to analyze.
4. Send the selected changes to the configured Ollama model.
5. Generate a commit message and explanation.
6. Ask for your confirmation.
7. Create the Git commit after approval.

## Building from Source

Clone the repository:

```bash
git clone https://github.com/timonexus/git-assistant.git
cd git-assistant
```

Build and run the tests:

```bash
mvn clean verify
```

## Configuration

The current version uses the default Ollama model `qwen3:4b` and the local Ollama API endpoint.

More configuration options are planned for future releases.

## Project Status

Git Assistant is currently under active development.

The core commit-generation workflow is implemented. Installation improvements, additional configuration options, expanded test coverage and further usability enhancements are planned.

Feedback, bug reports and feature suggestions are welcome.

## Contributing

Contributions are welcome!

If you encounter a bug or have an idea for an improvement, feel free to open an issue or submit a pull request.

Please keep contributions focused, tested and consistent with the existing code style.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

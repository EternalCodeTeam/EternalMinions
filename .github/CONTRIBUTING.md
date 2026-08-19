# Contributing to EternalMinions

Thanks for your interest in improving EternalMinions!

> [!NOTE]
> EternalMinions is an early-stage prototype under active development. APIs and internals may change without notice — check open issues and pull requests before starting significant work.

## Building the project

The project uses Gradle (wrapper included) and targets Java 21.

```bash
./gradlew build
```

This compiles all modules, runs the tests, and builds the plugin jar into `eternalminions-plugin/build/libs/`.

## Running tests

```bash
./gradlew test
```

## Running a dev server

```bash
./gradlew :eternalminions-plugin:runServer
```

This downloads a Paper server and launches it with the plugin loaded.

## Submitting a pull request

1. Fork the repository and branch off `develop`.
2. Make your changes and make sure `./gradlew build` passes.
3. Open a pull request against `develop` describing what changed and why.
4. Keep pull requests focused — unrelated changes should go in a separate PR.

By contributing, you agree that your contributions will be licensed under the project's [GPL-3.0 license](../LICENSE). Please also follow our [Code of Conduct](CODE_OF_CONDUCT.md).

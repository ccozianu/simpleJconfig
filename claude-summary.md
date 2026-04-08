# Project Summary

A small Java library for supplying typed configuration values (URLs, credentials, tunables, etc.) without the overhead of heavyweight frameworks like Spring or Guice.

## Core idea

The developer declares a **pair of matching interfaces**:

1. A **config interface** with typed getter methods (`jdbcUrl()`, `username()`, `maxOpenConnections()`, ...).
2. A matching **builder interface** with corresponding setter methods returning the builder, plus a `done()` method that returns the config.

The library implements both interfaces reflectively via `ReflectiveConfigurator.configBuilderFor(ConfigIface.class, BuilderIface.class)`, so the user never writes boilerplate implementations.

## Usage highlights

- Build configurations fluently and store them as static constants per environment (PROD, DEV, etc.).
- An optional `cloneBuilder()` method on the config interface lets you derive a new config from an existing one by overriding only a few fields (e.g., a standby DB config cloned from prod).
- Planned/extra features mentioned: default values and transform functions (e.g., to avoid storing passwords in clear text).

## Motivation

The author's stance is that typical configuration solutions (properties/XML/JSON/YAML files combined with DI frameworks) tend to create more problems than they solve. This package aims to keep configuration as plain, typed Java objects supplied via constructors — no DI container required.

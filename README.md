[<img src="https://aws1.discourse-cdn.com/standard14/uploads/oktadev/original/1X/0c6402653dfb70edc661d4976a43a46f33e5e919.png" align="right" width="256px"/>](https://devforum.okta.com/)
[![Maven Central](https://img.shields.io/maven-central/v/com.okta/okta-maven-plugin.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.okta%22%20a%3A%22okta-maven-plugin%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Okta Maven Plugin
=================

Okta's Maven Plugin will help you get started with Okta without ever leaving your console.

## Prerequisite

- Java 8

## Release status

This library uses semantic versioning.

| Version | Status                    |
| ------- | ------------------------- |
| 0.x.0 | :warning: Beta |

## Usage

The basic usage is simply:

```bash
mvn com.okta:okta-maven-plugin:setup
```

This will prompt you for required information and setup a new OIDC application for you.

For more complete information see the [complete plugin documentation](https://oktadeveloper.github.io/okta-maven-plugin)

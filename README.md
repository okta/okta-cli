[<img src="https://aws1.discourse-cdn.com/standard14/uploads/oktadev/original/1X/0c6402653dfb70edc661d4976a43a46f33e5e919.png" align="right" width="256px"/>](https://devforum.okta.com/)
<!-- Uncomment after first release -->
<!--
[![Maven Central](https://img.shields.io/maven-central/v/com.okta.sdk/okta-sdk-api.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.okta.sdk%22%20a%3A%22okta-sdk-api%22)
-->
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
| 0.x.0-SNAPSHOT | :warning: Beta, snapshot only |

## Usage

The basic usage is simply:

```bash
mvn com.okta:okta-maven-plugin:setup
```

This will prompt you for required information and setup a new OIDC application for you.

For more complete information see the [complete plugin documentation](https://oktadeveloper.github.io/okta-maven-plugin)


You MUST include the Sonatype OSSRH Snapshot repository (until we cut our first release), in your `settings.xml` or your `pom.xml`:

```xml
<pluginRepositories>
    <pluginRepository>
        <id>ossrh</id>
        <releases><enabled>false</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </pluginRepository>
</pluginRepositories>
```

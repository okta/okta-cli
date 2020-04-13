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
| 0.2.0 | GA |

## Usage

The basic usage is simply:

```bash
mvn com.okta:okta-maven-plugin:setup
```

This will prompt you for required information and setup a new OIDC application for you.

For more complete information see the [complete plugin documentation](https://oktadeveloper.github.io/okta-maven-plugin)

## Spring Boot Quick start

Create a new Spring Boot project
```bash
curl https://start.spring.io/starter.tgz -d dependencies=web,okta \
  -d baseDir=okta-spring-security-example-app | tar -xzvf -
cd okta-spring-security-example-app
```

Run the Okta Maven Plugin to Register a new account and configure your new Spring OIDC application
```bash
./mvnw com.okta:okta-maven-plugin:setup
```

Add a simple REST controller, for example replace your `src/main/java/com/example/demo/DemoApplication.java` with:

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@GetMapping("/")
	String hello(@AuthenticationPrincipal OidcUser user) {
		return String.format("Welcome, %s", user.getFullName());
	}
}
```

Start your Spring Boot application:

```bash
./mvnw spring-boot:run
```

Now just browse to: `http://localhost:8080/` you will be prompted to login.

Check your email to for your new account details!

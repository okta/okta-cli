[<img src="https://aws1.discourse-cdn.com/standard14/uploads/oktadev/original/1X/0c6402653dfb70edc661d4976a43a46f33e5e919.png" align="right" width="256px"/>](https://devforum.okta.com/)
[![Maven Central](https://img.shields.io/maven-central/v/com.okta/okta-maven-plugin.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.okta%22%20a%3A%22okta-maven-plugin%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Okta CLI
========

The Okta CLI is the easiest way to get started with Okta! 

If you are a Java developer using Apache Maven, also checkout the [Okta Maven Plugin](maven-plugin/README.md). 

## Installation

**macOS (via [Homebrew](https://brew.sh/)):**

```bash
brew cask install oktadeveloper/tap/okta
```

**Linux (via Flatpak)**

```bash
# Add Flathub repo
flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo

# install the package
flatpak install com.okta.developer.CLI

# add this to your appropriate dot file
alias okta="flatpak run com.okta.developer.CLI"
```

**Linux and macOS:**

```bash
curl https://raw.githubusercontent.com/oktadeveloper/okta-cli/master/cli/src/main/scripts/install.sh | sh
```

**Windows (via Chocolatey)**

```bash
choco install okta
```

**Manual Installs**

Download the binary for your OS from the [releases](https://github.com/oktadeveloper/okta-cli/releases) page. Manually install it to a directory on your PATH. 

## Usage

Run `okta --help` or `okta help <command>` for more details.

## Sign Up For a Free Okta Developer Account

Run `okta register` to sign up for a new account.  You will be prompted for your name and email address.  Account activation information will be emailed to you.

## Log Into an Existing Account

Configure the Okta CLI tool with an existing Okta account use `okta login` and follow the prompts.  

## Create an Okta Application (OAuth 2.0 / OIDC)

The Okta CLI tool can create Okta OAuth 2.0 / OIDC Applications for you with a few prompts. The following application types are supported:
- Web - Backend applications, Java, .Net, PHP, etc
- Single Page App (SPA) - Browser based applications
- Native App - desktop and mobile applications
- Service - Machine to Machine services

Run `okta apps create` and you will be prompted for the required information.

## Bash / Zsh Completion

If you have "bash-completion" installed run one of the following commands:

Linux

```sh
okta generate-completion > /etc/bash_completion.d/okta
```

Mac

```sh
okta generate-completion > /usr/local/etc/bash_completion.d/okta
```

If you are not using "bash-completion", copy the output of `okta generate-completion` to your desired location, and source the file from your `~/.bash_profile` script, for example:

```sh
okta generate-completion > ~/okta.bash
echo `. ~/okta.bash` >> ~/.bash_profile
```

For more details on using bash completion see the [Picocli documentation](https://picocli.info/autocomplete.html#_installing_completion_scripts_permanently_in_bashzsh).

## Building / Contributing

You'll need to use the [GraalVM]() to build this project. That's what supports the native build.

If you use [sdkman]() on Mac, it's pretty easy to get the right version installed:

```
sdk install java $(cat .java-version) && sdk use java $(cat .java-version)
gu install native-image
```

If you want to test against a locally running service, you'll need to run the cli with the following environment
variable:

```
OKTA_CLI_BASE_URL=http://localhost:8080/
```

**NOTE**: The backend service code is currently in a private repo.


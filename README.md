[<img src="https://aws1.discourse-cdn.com/standard14/uploads/oktadev/original/1X/0c6402653dfb70edc661d4976a43a46f33e5e919.png" align="right" width="256px"/>](https://devforum.okta.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Okta CLI
========

The Okta CLI is the easiest way to get started with Okta! 

If you are a Java developer using Apache Maven, also checkout the [Okta Maven Plugin](maven-plugin/README.md). 

## Installation 

Linux and Mac users:

```bash
curl https://raw.githubusercontent.com/oktadeveloper/okta-maven-plugin/cli/cli/src/main/scripts/install.sh | sh
```

Windows users will need to install the `okta.exe` file manually and place it on your path:

https://raw.githubusercontent.com/oktadeveloper/okta-maven-plugin/cli-test-dist/okta-cli-windows-latest-x86_64.exe.zip

## Usage

Run `okta --help` or `okta help <command>` for more details.

### Sign Up For a Free Okta Developer Account

Run `okta register` to sign up for a new account.  You will be prompted for your name and email address.  Account activation information will be emailed to you.

### Log Into an Existing Account

Configure the Okta CLI tool with an existing Okta account use `okta login` and follow the prompts.  

### Create an Okta Application (OAuth 2.0 / OIDC)

The Okta CLI tool can create Okta OAuth 2.0 / OIDC Applications for you with a few prompts. The following application types are supported:
- Web - Backend applications, Java, .Net, PHP, etc
- Single Page App (SPA) - Browser based applications
- Native App - desktop and mobile applications
- Service - Machine to Machine services

Run `okta apps create` and you will be prompted for the required information.

### Bash / Zsh Completion

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

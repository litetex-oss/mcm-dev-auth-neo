<!-- modrinth_exclude.start -->

[![Version](https://img.shields.io/modrinth/v/dev-auth-neo)](https://modrinth.com/mod/dev-auth-neo)
[![Build](https://img.shields.io/github/actions/workflow/status/litetex-oss/mcm-dev-auth-neo/check-build.yml?branch=dev)](https://github.com/litetex-oss/mcm-dev-auth-neo/actions/workflows/check-build.yml?query=branch%3Adev)

# DevAuth Neo (Fabric)

<!-- modrinth_exclude.end -->

Authenticate Minecraft accounts in development environments.

This is a "reincarnation" of [DevAuth](https://github.com/DJtheRedstoner/DevAuth) but improved, updated and available directly as a Fabric mod.

## Usage

### Adding the mod

<details><summary>via Maven Central (default)</summary>

Add the following to ``build.gradle``:
```groovy
dependencies {
    modImplementation 'net.litetex.mcm:dev-auth-neo:<version>'
    // Further documentation: https://wiki.fabricmc.net/documentation:fabric_loom
}
```

</details>

<details><summary>via Modrinth</summary>

Add the following to ``build.gradle``:
```groovy
repositories {
    // https://support.modrinth.com/en/articles/8801191-modrinth-maven
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        filter {
            includeGroup 'maven.modrinth'
        }
    }
}

dependencies {
    modImplementation 'maven.modrinth:dev-auth-neo:<version>'
    // Further documentation: https://wiki.fabricmc.net/documentation:fabric_loom
}
```

</details>

### Quickstart

IMPORTANT: **DevAuth is disabled by default**, in order to be unobtrusive.<br/>
The simplest way to enable it is by setting the corresponding system property: ``-Ddevauth.enabled=1``

* When launching for the first time you will be prompted to enter your credentials
    * You will also get hints on what to do in the console/logs when required
* After the authentication was successful the generated tokens will be stored in `microsoft_accounts.json`
    * Future logins will try to reuse these tokens
* The tokens are then delegated to the game and it starts as usual

### Configuration

<details><summary>The configuration is dynamically loaded from (sorted by highest priority)</summary>

* Environment variables 
    * prefixed with ``DEVAUTH_``
    * all properties are in UPPERCASE and use `_` (instead of `.`) as delimiter
* System properties
    * prefixed with ``devauth.``
* A configuration file located in ``~/.dev-auth-neo/config.json``

</details>

<details><summary>Full list of configuration options</summary>

#### General

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `enabled` | `bool` | `false` | |
| `account` | `String` | - | If not set:<br/>Will be prompted via the console |
| `account-type` | `String` | `microsoft` | Currently only Microsoft is supported |
| `force-handle-all-tokens-as-expired` | `bool` | `false` | Handles all existing tokens as if they have expired and refreshes them |
| `state-dir` | `String` | Automatically determined<br/>`~/.dev-auth-neo`) | The directory where the login information of the provider will be saved |
| `cache-profile-info-minutes` | `int` | `360`<br/>6h | How long should fetched profile information(uuid and name) be cached? |

#### Microsoft Grant-Flow options

_NOTE: All grant-flow options are additionally prefixed with `microsoft.oauth2.` <br/> For example `predefined-provider` must therefore be defined as `devauth.microsoft.oauth2.predefined-provider`_

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `grant-flow` | `String` | `auth-code-embedded` | See bellow for available flows |
| `client-id` | `String` | automatically determined | Designed as general purpose override for e.g. a custom provider |
| `scopes` | `List<String>` | automatically determined | |

#### `auth-code-embedded` Auth Code flow using an embedded browser

* This login mechanism is identical to the one used by Minecraft's launcher
    * Therefore selected by default
* This workflow downloads and installs a Chromium browser using CEF (Chromium Embedded Framework)
    * The download is around 150MB
    * It's installed by default into `~/.dev-auth-neo/jcef`

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `use-temporary-cache-dir` | `bool` | `true` | Creates and uses a temporary directory for the browser cache.<br/> If this is set to `false` all browser information (cookies, cache) and also possible logins will be persisted! |
| `redirect-uri` | `String` | automatically determined | Designed as general purpose override for e.g. a custom provider |

#### `auth-code-external` Auth Code flow using an external browser

* Same as `auth-code-embedded` however this can be done with the system browser
* Starts an integrated webserver to get the tokens at the end of the flow
* ⚠ This flow depends on 3rd party clients / launchers
    * These clients are totally dependent on Mojangs [explicit approval](https://aka.ms/mce-reviewappid) and can - although unlikely to happen - be blocked at any time for any reason by them
    * The 3rd party can change or delete their client without warning
    * The 3rd party might be able to see your login activity
    * Available 3rd parties:
        * [DevAuth](https://github.com/DJtheRedstoner/DevAuth) (default)

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `predefined-provider` | `String` | `devauth` | |
| `open-system-browser` | `bool` | `true` | Should the system browser be automatically opened with the sign in page? |
| `redirect-port` | `int` | automatically determined | Port used for the integrated webserver |
| `redirect-uri` | `String` | automatically determined | Designed as general purpose override for e.g. a custom provider |

#### `device-code` Device Code flow

* This flow generates a short code that needs to be verified online with the an account
* ⚠ This flow depends on 3rd party clients / launchers
    * These clients are totally dependent on Mojangs [explicit approval](https://aka.ms/mce-reviewappid) and can - although unlikely to happen - be blocked at any time for any reason by them
    * The 3rd party can change or delete their client without warning
    * The 3rd party might be able to see your login activity
    * Available 3rd parties:
        * [MultiMC](https://github.com/MultiMC/Launcher) (default)
        * [DevLogin](https://github.com/covers1624/DevLogin)
        * [Prism](https://github.com/PrismLauncher/PrismLauncher)

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `predefined-provider` | `String` | `multimc` | |

</details>

## FAQ

### How do I delete the login information?

Delete `%USERPROFILE%\.dev-auth-neo` (Windows) or `~/.dev-auth-neo` (Linux)

### I want to switch to another account

_What's the easiest way to do this?_

Set ``-Ddevauth.account=otherAccountName``

### Security?

The tokens are stored in a file on the disk in above specified folder.
As with all data on your PC, you as a user are responsible for keeping them secure.

You can revoke tokens in the [Microsoft account settings](https://account.live.com/consent/Manage).

<!-- modrinth_exclude.start -->

## Installation
[Installation guide for the latest release](https://github.com/litetex-oss/mcm-dev-auth-neo/releases/latest#Installation)

### Usage in other mods

Add the following to ``build.gradle``:
```groovy
dependencies {
    modImplementation 'net.litetex.mcm:dev-auth-neo:<version>'
    // Further documentation: https://wiki.fabricmc.net/documentation:fabric_loom
}
```

> [!NOTE]
> The contents are hosted on [Maven Central](https://repo.maven.apache.org/maven2/net/litetex/mcm/). You shouldn't have to change anything as this is the default maven repo.<br/>
> If this somehow shouldn't work you can also try [Modrinth Maven](https://support.modrinth.com/en/articles/8801191-modrinth-maven).

## Contributing
See the [contributing guide](./CONTRIBUTING.md) for detailed instructions on how to get started with our project.

<!-- modrinth_exclude.end -->

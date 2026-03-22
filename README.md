# Herald

## Description

Herald is a Minecraft server plugin that sends Discord webhook notifications when players join the server. Email notifications are also supported as a secondary option.

![screenshot of emails](./screenshots/mailhog-7-1-2025.PNG)

## Installation

### First Time Installation

1. Download the Herald plugin JAR file.
2. Place the JAR in the `plugins` folder of your server.
3. Restart your server.
4. Edit the generated `plugins/Herald/config.yml` to configure notification settings.

### Optional Integrations

Herald can send notifications through Discord webhooks, SMTP email, or both. See the [Configuration Guide](CONFIG.md) for setup details.

## Usage

### Documentation

- [User Guide](USER_GUIDE.md) – Getting started and common scenarios
- [Commands Reference](COMMANDS.md) – Complete list of all commands
- [Configuration Guide](CONFIG.md) – Detailed configuration options
- [Discord Testing](DISCORD_TESTING.md) – Testing Discord webhooks without a server
- [Changelog](CHANGELOG.md) – Release-by-release summary of changes

## Support

You can find the support Discord server [here](https://discord.gg/xXtuAQ2).

### Experiencing a bug?

Please fill out a bug report [here](https://github.com/Dans-Plugins/Herald/issues/new).

- [Known Bugs](https://github.com/Dans-Plugins/Herald/issues?q=is%3Aissue+is%3Aopen+label%3Abug)

## Contributing

- [CONTRIBUTING.md](CONTRIBUTING.md)

## Testing

### Unit Tests

Linux:

    ./gradlew clean test

Windows:

    .\gradlew.bat clean test

If you see `BUILD SUCCESSFUL`, the tests have passed.

## Development

### Test Server with Plugin Hot-Reloading

A Docker-based test server is available for development.

#### Setup

1. Copy `sample.env` to `.env` and configure as needed.
2. Build the plugin: `./gradlew build`
3. Start the test server: `./up.sh`

#### Reloading the Plugin

    ./reload-plugin.sh

#### Stopping the Test Server

    ./down.sh

## Authors and Acknowledgement

### Developers

| Name | Main Contributions |
|------|--------------------|
| Daniel McCoy Stephenson | Creator and lead developer |

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE) (GPL-3.0).

You are free to use, modify, and distribute this software, provided that:

- Source code is made available under the same license when distributed.
- Changes are documented and attributed.
- No additional restrictions are applied.

See the [LICENSE](LICENSE) file for the full text of the GPL-3.0 license.

## Project Status

This project is in active development.

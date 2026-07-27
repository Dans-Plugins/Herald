# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Changed

- Startup now warns when email notifications are partially configured, naming each missing or invalid key (`email-recipients`, `smtp.server`, `smtp.port`, `email.sender`) instead of logging `Email notifications enabled` and then failing on every player join.
- Startup validation now checks that `discord.webhook-url` is a syntactically valid `http`/`https` URL, so a webhook URL with the scheme left off is reported once at startup instead of failing on every player join.
- Startup validation now checks that every address in `email-recipients` and the `email.sender` address are parseable, naming the offending address, instead of failing on every player join.
- Startup now warns when no notification method is configured at all, so a default install no longer looks healthy while doing nothing.
- Discord webhook failures now include the error response body (e.g. `Invalid Webhook Token`) alongside the HTTP status code.
- Notifier failures are logged through the plugin logger with their stack trace instead of being printed to standard error.

### Fixed

- Email notification body now includes the server name (e.g. `Steve has joined MySurvivalServer at <date>`) instead of the generic `"the server"`.
- `server-name` is now cached at plugin load time rather than re-read from config on every player join event.
- `DiscordNotifier` no longer uses the deprecated `URL(String)` constructor, removing a compiler deprecation warning on every build.

## [1.0.0]

### Added

- Discord webhook notifications for player join events
- Configurable join-message templates with `{player}` and `{server}` placeholders
- Email (SMTP) notifications for player join events
- Configurable server name for notification messages
- Docker Compose setup for local mail-server testing
- Discord webhook test scripts (Bash and Python)

[Unreleased]: https://github.com/Dans-Plugins/Herald/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Dans-Plugins/Herald/releases/tag/v1.0.0

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

- `email.subject` and `email.body` config options, so the email subject line and body can be customised the same way `discord.join-messages` already could. Both support `{player}`, `{server}` and `{time}` placeholders and fall back to the built-in defaults when absent or empty.
- `email.enabled` config option, mirroring `discord.enabled`, so email notifications can be switched off without clearing the SMTP details, sender and recipient list. It defaults to `true`, leaving existing configurations behaving exactly as before, and turning it off is reported at startup so the log still explains why no mail is arriving.

### Changed

- Per-join log messages now name the notification channel (`Discord`, `email`) instead of the Java class name (`DiscordNotifier`, `EmailNotifier`), matching the vocabulary used by the rest of Herald's log output.
- The comment above the `smtp:` block in the default `config.yml` now describes that block instead of repeating the file name.
- Startup now warns when email notifications are partially configured, naming each missing or invalid key (`email-recipients`, `smtp.server`, `smtp.port`, `email.sender`) instead of logging `Email notifications enabled` and then failing on every player join.
- Startup validation now checks that `discord.webhook-url` is a syntactically valid `http`/`https` URL, so a webhook URL with the scheme left off is reported once at startup instead of failing on every player join. The warning explains why the URL is invalid without repeating the URL itself, so the webhook token never reaches the server log.
- Startup validation now checks that every address in `email-recipients` and the `email.sender` address are parseable, naming the offending address, instead of failing on every player join.
- Startup validation now checks that no entry in `discord.join-messages` is blank, naming the offending entry by its position in the list, so an empty template is reported once at startup instead of failing on the joins that happen to draw it.
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

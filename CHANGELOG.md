# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

- A `Dev Release` workflow, which republishes a rolling `dev` prerelease of `main` on every non-documentation push. This is what Dan's Plugin Manager's experimental channel installs from: `/dpm get herald --experimental` reads `releases/tags/dev`, so without it there is nothing for that command to download. The prerelease is unreleased, unreviewed code and is marked as such.

### Changed

- `smtp.use-tls: true` now requires STARTTLS instead of merely offering to use it. Only `mail.smtp.starttls.enable` was set before, which Jakarta Mail treats as advisory: a server that did not advertise STARTTLS was talked to in the clear, credentials included, while `smtp.use-tls: true` sat in the config file. `mail.smtp.starttls.required` is now set alongside it, so such a server fails the send and is reported. An installation pointed at a relay with no STARTTLS support must set `smtp.use-tls` to `false` to keep sending, which is reported at startup when `smtp.username` is also set, since the credentials then travel unencrypted.
- The SMTP session is now built with connect, read and write timeouts (10, 30 and 30 seconds). All three default to infinite in Jakarta Mail, so a mail server that accepted a connection and then stopped answering used to park the sending task for as long as the socket stayed open, holding one task per join and logging nothing. The failure is now reported the way any other send failure is, matching the bounds the Discord side has always had.
- A Discord webhook message that is rate-limited (HTTP 429) is now waited out and sent again once, instead of being discarded and reported as a failure. The wait is taken from the `Retry-After` header, falling back to the `retry_after` field of the response body and then to one second, and is capped at ten seconds so that one pathological value cannot park the sending task; a wait longer than the cap, or a second rate limit, is reported as a failure the way it was before. The retry itself is reported as a warning naming how long it waits, rather than as the `SEVERE` line a transient, self-correcting condition used to produce.

## [2.0.0-SNAPSHOT-8-8-2026] – 2026-08-08

### Changed
- Herald is now developed AI-first. Day-to-day feature work, grooming, review and maintenance run through AI agents working directly against this repository, with the maintainers setting direction and approving what lands. The major version bump marks that change in how the project is built — it is not a break in behaviour, configuration or stored data, and existing installations can upgrade in place. Released as `2.0.0-SNAPSHOT-8-8-2026`: the AI-first line has not yet been verified in live operation, and the dated snapshot designation stays until it has.

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
- Startup validation now checks that no entry in `discord.join-messages` can produce a message longer than Discord's 2000-character limit, measured against the configured `server-name` and the longest player name a Minecraft Java Edition account allows, so an over-long template is reported once at startup instead of failing on the joins that happen to draw it.
- Startup now warns when no notification method is configured at all, so a default install no longer looks healthy while doing nothing.
- Discord webhook failures now include the error response body (e.g. `Invalid Webhook Token`) alongside the HTTP status code.
- Notifier failures are logged through the plugin logger with their stack trace instead of being printed to standard error.

### Fixed

- Email notification body now includes the server name (e.g. `Steve has joined MySurvivalServer at <date>`) instead of the generic `"the server"`.
- `server-name` is now cached at plugin load time rather than re-read from config on every player join event.
- A `server-name` holding nothing but whitespace now falls back to `Minecraft` instead of being sent verbatim, which rendered as bold markdown wrapped around nothing in Discord and as a gap in the email subject. Whitespace around a populated name is stripped for the same reason.
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

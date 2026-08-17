# Configuration Guide

All options are set in `plugins/Herald/config.yml`. The file is generated with sensible defaults on first run.

Options are listed in the same order they appear in the default configuration file.

## server-name

**Type:** string
**Default:** `""`
**Description:** The server name used in notification messages. Replaces the `{server}` placeholder in Discord join messages and in email notifications. If left empty, defaults to `"Minecraft"`.

A value holding nothing but whitespace counts as empty and falls back to `"Minecraft"` too, rather than being sent as a name made of spaces. Whitespace around a name that does hold something is stripped, so `"  My Server  "` and `"My Server"` behave identically.

**Example:**

```yaml
server-name: "My Awesome Server"
```

## discord.enabled

**Type:** boolean
**Default:** `false`
**Description:** Whether Discord webhook notifications are enabled.

**Example:**

```yaml
discord:
  enabled: true
```

## discord.webhook-url

**Type:** string
**Default:** `""`
**Description:** The Discord webhook URL for the channel where join notifications should be sent. Required when `discord.enabled` is `true`. Must be a syntactically valid `http` or `https` URL — a value with the scheme left off, such as `discord.com/api/webhooks/123456/abcdef`, is rejected. If the URL is missing or unusable, Herald logs a warning naming the problem at startup and skips Discord notifications.

**Example:**

```yaml
discord:
  webhook-url: "https://discord.com/api/webhooks/123456/abcdef"
```

## discord.join-messages

**Type:** list of strings
**Default:** 10 medieval-themed messages (see below)
**Description:** A list of message templates picked at random when a player joins. Supports `{player}` and `{server}` placeholders. If the list is empty or absent, Herald falls back to built-in defaults.

No entry may be blank or whitespace-only. Discord rejects an empty message, so a blank entry would fail only on the joins that happen to draw it; Herald therefore reports it at startup instead, naming its position in the list (counting from 1), and skips Discord notifications until it is fixed. To go back to the built-in defaults, remove the whole list rather than leaving blank entries in it.

No entry may produce a message longer than **2000 characters**, which is the most Discord accepts. Herald measures each entry at startup against the longest message it could produce — every `{server}` placeholder filled with the configured `server-name`, and every `{player}` placeholder filled with a 16-character player name, the longest a Minecraft Java Edition account allows — and reports any entry that would breach the limit, again naming its position in the list:

```
'discord.join-messages' entry 3 can produce a message of up to 2143 characters, but Discord accepts at most 2000
```

A long `server-name` counts towards this, so an entry that fits under one server name may not fit under another.

**Default messages:**

```yaml
discord:
  join-messages:
    - "⚔️ Hear ye, hear ye! **{player}** hath entered the realm of **{server}**! ⚔️"
    - "🏰 The gates of **{server}** open wide for **{player}**! Welcome, brave soul!"
    - "📜 By royal decree, **{player}** hath been granted passage into **{server}**!"
    - "🗡️ A new champion approaches! **{player}** rides into **{server}**!"
    - "🌟 The bards shall sing of this day! **{player}** hath arrived in **{server}**!"
    - "👑 All hail **{player}**, who now graces the lands of **{server}**!"
    - "🔥 The torches flicker as **{player}** strides into **{server}**!"
    - "🎺 Sound the trumpets! **{player}** hath joined the kingdom of **{server}**!"
    - "🛡️ The defenders of **{server}** welcome **{player}** to their ranks!"
    - "✨ By the stars above, **{player}** hath made their presence known in **{server}**!"
```

## email-recipients

**Type:** list of strings
**Default:** `[]`
**Description:** List of email addresses that will receive a notification when a player joins. This, `smtp.server`, and `email.sender` must all be configured — and `smtp.port` must be a valid port — for email notifications to be active; if any of them is missing or invalid, Herald logs a warning naming the offending key at startup and skips email notifications.

Every entry must be a parseable address. Herald checks each one at startup and names the offending address in the warning, so a typo such as `admin@` (no domain) is reported once at startup rather than on every player join. A display name is accepted, e.g. `Server Admin <admin@example.com>`.

**Example:**

```yaml
email-recipients:
  - "admin@example.com"
  - "owner@example.com"
```

## smtp.server

**Type:** string
**Default:** `""`
**Description:** The hostname or IP address of your SMTP server.

**Example:**

```yaml
smtp:
  server: "smtp.example.com"
```

## smtp.port

**Type:** integer
**Default:** `587`
**Description:** The port of your SMTP server. Use `587` for STARTTLS with `smtp.use-tls`, `465` for implicit TLS with `smtp.implicit-tls`, or `25` for plain SMTP with both of those set to `false`. Must be between `1` and `65535`; if it is set to a value outside that range, Herald logs a warning at startup and skips email notifications.

The port and the encryption mode have to agree, because a server on `465` negotiates TLS before the first command while one on `587` expects a plain connection that STARTTLS then upgrades. Herald warns at startup when the two conventional ports are paired with the wrong mode, rather than leaving a protocol error to be read off the first player join:

```
'smtp.port' is 465, which conventionally expects implicit TLS, but 'smtp.implicit-tls' is false. Servers on that port negotiate TLS before the first command, so the send is likely to fail. Set 'smtp.implicit-tls' to true and 'smtp.use-tls' to false, or use port 587 with 'smtp.use-tls'.
```

This is a warning and not a refusal, since a relay is free to offer either mode on any port; email notifications still load.

**Example:**

```yaml
smtp:
  port: 587
```

## smtp.username

**Type:** string
**Default:** `""`
**Description:** The username for SMTP authentication. Leave empty if your server does not require authentication.

**Example:**

```yaml
smtp:
  username: "user@example.com"
```

## smtp.password

**Type:** string
**Default:** `""`
**Description:** The password for SMTP authentication. Leave empty if your server does not require authentication.

**Example:**

```yaml
smtp:
  password: "secret"
```

## smtp.use-tls

**Type:** boolean
**Default:** `true`
**Description:** Whether STARTTLS is required when connecting to the SMTP server. Set to `true` when using port `587`. For a server that expects TLS from the first byte, on port `465`, use `smtp.implicit-tls` instead.

This is binding rather than best-effort: when it is `true`, a server that does not offer STARTTLS fails the send and the failure is logged, instead of the connection quietly falling back to plain text. Set it to `false` only for a server that genuinely has no STARTTLS support, and be aware of what that means — with `smtp.username` filled in, the SMTP username and password travel over that unencrypted connection along with every notification. Herald warns at startup when credentials are configured with neither encryption mode turned on:

```
'smtp.username' is set but neither 'smtp.use-tls' nor 'smtp.implicit-tls' is true, so the SMTP username and password are sent over an unencrypted connection, along with every notification. Set 'smtp.use-tls' to true, or 'smtp.implicit-tls' to true on a port that expects SMTPS, unless the server genuinely has no encryption support.
```

**Example:**

```yaml
smtp:
  use-tls: true
```

## smtp.implicit-tls

**Type:** boolean
**Default:** `false`
**Description:** Whether the connection to the SMTP server is encrypted before the first SMTP command, which is what servers on port `465` — SMTPS — expect. Set this to `true` and `smtp.use-tls` to `false` for such a server; several hosted mail providers publish `465` as their primary submission port.

The two encryption keys are alternatives rather than layers: `smtp.use-tls` starts a plain connection and upgrades it with STARTTLS, while `smtp.implicit-tls` completes the TLS handshake first and sends every command inside it. Setting both to `true` is a configuration error, and Herald reports it at startup and skips email notifications rather than silently picking one:

```
'smtp.use-tls' and 'smtp.implicit-tls' are both true, but they are alternatives: 'smtp.use-tls' upgrades a plain connection with STARTTLS, usually on port 587, while 'smtp.implicit-tls' negotiates TLS before the first command, usually on port 465. Set exactly one of them to true.
```

Leaving both `false` sends everything, credentials included, in plain text; see `smtp.use-tls` for what that costs.

**Example:**

```yaml
smtp:
  port: 465
  use-tls: false
  implicit-tls: true
```

## email.enabled

**Type:** boolean
**Default:** `true`
**Description:** Whether email notifications are considered at all. Setting this to `false` turns email off while leaving `email-recipients`, the `smtp` block and the rest of the `email` block in place, so a working setup does not have to be retyped to be switched back on. While it is `false`, no email is sent and no incomplete-configuration warning is logged; `Email notifications are disabled in config` is logged at startup instead, so the reason no mail is arriving can be found in the log.

Unlike `discord.enabled`, this defaults to `true`, so that a `config.yml` written before this key existed keeps sending the emails it sends today. A freshly generated `config.yml` still sends nothing, because `email-recipients`, `smtp.server` and `email.sender` are all empty.

**Example:**

```yaml
email:
  enabled: false
```

## email.sender

**Type:** string
**Default:** `""`
**Description:** The email address that appears as the sender of notification emails. Required when email notifications are used, alongside `email-recipients` and `smtp.server`. Must be a parseable address; Herald checks it at startup and names it in the warning if it is not. A display name is accepted, e.g. `Herald <herald@example.com>`.

**Example:**

```yaml
email:
  sender: "herald@example.com"
```

## email.subject

**Type:** string
**Default:** `"{player} joined {server} server"`
**Description:** The subject line of notification emails. Supports the `{player}`, `{server}` and `{time}` placeholders. `{time}` is replaced with the time the player joined, in the server's default time zone and locale, e.g. `Fri Aug 01 21:48:45 UTC 2026`. The format itself is not configurable. If the key is absent or empty, Herald falls back to the built-in default.

**Example:**

```yaml
email:
  subject: "[{server}] {player} is online"
```

## email.body

**Type:** string
**Default:** `"{player} has joined {server} at {time}"`
**Description:** The plain-text body of notification emails. Supports the same `{player}`, `{server}` and `{time}` placeholders as `email.subject`, and falls back to the built-in default when absent or empty. Remove `{time}` to send a body without a timestamp. The subject and body of a single notification are always filled from the same timestamp.

**Example:**

```yaml
email:
  body: "Greetings from {server}. {player} just logged in."
```

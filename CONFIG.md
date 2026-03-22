# Configuration Guide

All options are set in `plugins/Herald/config.yml`. The file is generated with sensible defaults on first run.

Options are listed in the same order they appear in the default configuration file.

## server-name

**Type:** string
**Default:** `""`
**Description:** The server name used in notification messages. Replaces the `{server}` placeholder in Discord join messages and in email notifications. If left empty, defaults to `"Minecraft"`.

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
**Description:** The Discord webhook URL for the channel where join notifications should be sent. Required when `discord.enabled` is `true`.

**Example:**

```yaml
discord:
  webhook-url: "https://discord.com/api/webhooks/123456/abcdef"
```

## discord.join-messages

**Type:** list of strings
**Default:** 10 medieval-themed messages (see below)
**Description:** A list of message templates picked at random when a player joins. Supports `{player}` and `{server}` placeholders. If the list is empty or absent, Herald falls back to built-in defaults.

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
**Description:** List of email addresses that will receive a notification when a player joins. Both this and `smtp.server` must be configured for email notifications to be active.

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
**Description:** The port of your SMTP server. Use `587` for TLS (STARTTLS) or `25` for plain SMTP.

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
**Description:** Whether to use STARTTLS when connecting to the SMTP server. Set to `true` when using port `587`.

**Example:**

```yaml
smtp:
  use-tls: true
```

## email.sender

**Type:** string
**Default:** `""`
**Description:** The email address that appears as the sender of notification emails.

**Example:**

```yaml
email:
  sender: "herald@example.com"
```

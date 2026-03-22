# User Guide

## Prerequisites

- A Minecraft server running Spigot or Paper 1.16+
- Java 21 or higher
- (Optional) A Discord server with a webhook URL for Discord notifications
- (Optional) An SMTP mail server for email notifications

## First Steps

1. Download the Herald plugin JAR file.
2. Place the JAR in your server's `plugins` folder.
3. Start (or restart) your server. Herald will generate a default `plugins/Herald/config.yml`.
4. Stop the server and open `plugins/Herald/config.yml` in a text editor.
5. Configure at least one notification method (Discord or email) — see [Configuration Guide](CONFIG.md).
6. Start the server again. Herald will begin sending notifications when players join.

## Common Scenarios

### Sending Discord Notifications on Player Join

1. In your Discord server go to **Server Settings → Integrations → Webhooks**.
2. Click **New Webhook**, choose a name and channel, and copy the webhook URL.
3. In `config.yml` set:
   ```yaml
   discord:
     enabled: true
     webhook-url: "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"
   ```
4. Optionally set `server-name` to customise the `{server}` placeholder.
5. Restart the server or reload the plugin.

### Customising Join Messages

Edit the `discord.join-messages` list in `config.yml`. Each entry is a template that supports `{player}` and `{server}` placeholders. A random message is chosen each time a player joins.

```yaml
discord:
  join-messages:
    - "Welcome {player} to {server}!"
    - "{player} has arrived at {server}!"
```

### Sending Email Notifications on Player Join

1. Set `smtp.server` and `smtp.port` to your SMTP server details.
2. Set `email.sender` to the sender address.
3. Add recipient addresses to `email-recipients`.
4. Restart the server.

See the [Configuration Guide](CONFIG.md) for full details on every option.

## Permissions

Herald does not register any custom permission nodes. All functionality is controlled through the configuration file and applies server-wide.

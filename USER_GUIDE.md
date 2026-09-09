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

### Customising Discord Join Messages

Edit the `discord.join-messages` list in `config.yml`. Each entry is a template that supports `{player}` and `{server}` placeholders. A random message is chosen each time a player joins. No entry may be left blank, and none may grow past 2000 characters once its placeholders are filled in — Discord will not accept a message that long, so Herald reports the offending entry at startup.

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
4. Leave `email.enabled` set to `true`.
5. Leave `smtp.use-tls` set to `true` unless your server has no STARTTLS support. It is required rather than attempted, so a server that cannot do STARTTLS reports a failure instead of sending in the clear.
6. If your provider publishes port `465` instead, set `smtp.port` to `465`, `smtp.implicit-tls` to `true` and `smtp.use-tls` to `false`. That port encrypts the connection before the first command rather than upgrading it partway through, so exactly one of the two keys applies to any given server.
7. Leave `smtp.verify-server-identity` set to `true`, so that whichever encryption mode you chose also confirms the certificate belongs to the host in `smtp.server`. Turn it off only for a relay dialled by IP address, or one presenting a certificate for a different name.
8. Restart the server.

### Turning a Notification Channel Off

Each channel has its own switch, so neither has to be dismantled to be silenced:

```yaml
discord:
  enabled: false
email:
  enabled: false
```

The webhook URL, SMTP details, recipients and message templates are all left untouched, and setting the switch back to `true` restores the channel as it was.

### Customising Email Messages

Edit `email.subject` and `email.body` in `config.yml`. Both support the `{player}` and `{server}` placeholders, plus `{time}` for the moment the player joined. Leave a key empty to keep the built-in default.

```yaml
email:
  subject: "[{server}] {player} is online"
  body: "Greetings from {server}. {player} just logged in."
```

To send a body without a timestamp, simply leave `{time}` out of it.

See the [Configuration Guide](CONFIG.md) for full details on every option.

### Checking That Your Configuration Was Accepted

Herald reports what it loaded in the server log at startup:

- `Discord notifications enabled` / `Email notifications enabled` — that integration is active.
- `Email notifications are disabled in config` — `email.enabled` is `false`. Because that key defaults to `true`, this line only appears when it has been turned off deliberately. Setting `discord.enabled` to `false` logs nothing, since that is the shipped default.
- `Discord notifications are enabled in config, but the configuration is incomplete: ...` — `discord.enabled` is `true` but a required key is missing or unusable; the message names the key. A `discord.webhook-url` that is not a valid `http`/`https` URL is reported here, not on the first player join, as is a blank entry in `discord.join-messages`, or one long enough to breach Discord's 2000-character message limit once filled in; either is named by its position in the list.
- `Email configuration is incomplete: ...` — some email settings are filled in but not all, `smtp.port` is outside the valid range, or an address in `email-recipients` or `email.sender` cannot be parsed; the message names each missing or invalid key, and quotes the offending address where one is at fault.
- `No notification methods are configured, so Herald will not send any notifications.` — nothing is set up yet, which is the state of a freshly generated `config.yml`.
- `'smtp.username' is set but neither 'smtp.use-tls' nor 'smtp.implicit-tls' is true, ...` — email is configured and will be sent, but the SMTP credentials and every notification travel unencrypted. Turning on whichever encryption mode the server offers clears it.
- `'smtp.use-tls' and 'smtp.implicit-tls' are both true, ...` — the two encryption modes are alternatives, so email notifications are skipped until exactly one of them is set to `true`.
- `'smtp.port' is 465, which conventionally expects implicit TLS, ...` — or the reverse, implicit TLS on port `587`. Email notifications still load, because a relay may offer either mode on any port, but a send that fails with a protocol error after this warning is explained by it.
- `'smtp.verify-server-identity' is false, ...` — email is configured and will be sent over an encrypted connection, but the certificate presented is no longer checked against `smtp.server`. Setting the key back to `true` clears it.

If a notification fails to send later, Herald logs the failure with the reason and names the channel it was sent through (`Discord` or `email`) — for Discord, the reason includes the error message the webhook returned.

Some email failures are worth recognising by sight:

- A failure mentioning STARTTLS means `smtp.use-tls` is `true` but the server did not offer STARTTLS. Either point `smtp.server` and `smtp.port` at a port that does — usually `587` — or, if the server truly cannot, set `smtp.use-tls` to `false` and accept that the connection is then unencrypted. On port `465` the answer is neither: that port wants `smtp.implicit-tls` instead.
- A failure mentioning an unrecognised command, or a handshake or SSL error, usually means the encryption mode and the port disagree — implicit TLS on a STARTTLS port, or the reverse. Herald warns about the two conventional ports at startup, so the startup log names it as well.
- A failure naming the certificate, or reporting that the hostname does not match, means `smtp.verify-server-identity` is `true` and the certificate the server presented was issued for some other name than the one in `smtp.server`. Point `smtp.server` at the name the certificate carries where you can; where you cannot — a relay reached by IP address, for instance — set `smtp.verify-server-identity` to `false` and accept that the encrypted connection no longer proves who answered it.
- A failure mentioning a connect timeout means no connection to `smtp.server` on `smtp.port` could be established within ten seconds — usually a wrong host or port, or a firewall dropping the packets.
- A failure mentioning a read timeout means the connection was established but the server stopped answering, and Herald gave up after thirty seconds rather than waiting indefinitely. Either way the failure is reported, instead of the notification silently holding a task for every join.

One Discord failure is reported as a warning rather than an error, because it clears on its own:

- `Discord rate-limited a join notification; retrying in ...` — Discord asked Herald to slow down, which happens when several players join within the same couple of seconds. Herald waits for as long as Discord asked and sends the message again, so nothing is lost and no action is needed. If the retry is rate-limited too, or Discord asks for a wait longer than ten seconds, that is reported as a failure instead and the message is not sent.

## Permissions

Herald does not register any custom permission nodes. All functionality is controlled through the configuration file and applies server-wide.

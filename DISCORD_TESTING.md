# Discord Webhook Testing Scripts

This directory contains scripts to test Discord webhook notifications without running a Minecraft server.

## Prerequisites

### Bash Script (`test-discord-webhook.sh`)
- Bash shell
- Java installed (any version with `javac` and `java` commands)

### Python Script (`test-discord-webhook.py`)
- Python 3.x installed

## Getting a Discord Webhook URL

1. Go to your Discord server settings
2. Navigate to **Integrations** → **Webhooks**
3. Click **New Webhook**
4. Configure the webhook:
   - Set a name (e.g., "Herald Bot")
   - Choose the channel where notifications should be sent
   - Copy the webhook URL
5. Keep this URL secure - don't share it publicly!

## Usage

### Bash Script

```bash
# Basic usage (uses default player and server names)
./test-discord-webhook.sh https://discord.com/api/webhooks/123456/abcdef

# With custom player name
./test-discord-webhook.sh https://discord.com/api/webhooks/123456/abcdef Steve

# With custom player and server names
./test-discord-webhook.sh https://discord.com/api/webhooks/123456/abcdef Steve "My Awesome Server"
```

### Python Script

```bash
# Basic usage (uses default player and server names)
python3 test-discord-webhook.py https://discord.com/api/webhooks/123456/abcdef

# With custom player name
python3 test-discord-webhook.py https://discord.com/api/webhooks/123456/abcdef Steve

# With custom player and server names
python3 test-discord-webhook.py https://discord.com/api/webhooks/123456/abcdef Steve "My Awesome Server"
```

## What the Scripts Do

These scripts verify that a webhook URL is reachable and correctly configured by:

1. Taking a Discord webhook URL as input
2. Formatting a simple test message: `**PlayerName** joined the **ServerName** server`
3. Sending the message to Discord via HTTP POST request
4. Reporting success or failure

This confirms the webhook plumbing works end-to-end, but the message text is a fixed test string — it does not match the plugin's actual join messages. The real plugin picks a random, medieval-themed template from `discord.join-messages` in `config.yml` (see the [Configuration Guide](CONFIG.md)) each time a player joins.

## Example Output

### Successful Test
```
========================================
Discord Webhook Test Script
========================================

Testing Discord webhook...
Webhook URL: https://discord.com/api/webhooks/123456/****
Player Name: Steve
Server Name: My Awesome Server

Sending test message...

Sending payload: {"content": "**Steve** joined the **My Awesome Server** server"}
Response code: 204
✓ SUCCESS: Message sent to Discord!
Check your Discord channel for the notification.

========================================
Test completed successfully!
========================================
```

### Failed Test
```
========================================
Discord Webhook Test Script
========================================

Testing Discord webhook...
Webhook URL: https://discord.com/api/webhooks/invalid/****
Player Name: Steve
Server Name: Minecraft

Sending test message...

✗ ERROR: Failed to send message to Discord
Error: Discord webhook returned error code: 404

========================================
Test failed!
========================================
```

## Troubleshooting

### "Command not found" Error
- **Bash script**: Make sure the script is executable: `chmod +x test-discord-webhook.sh`
- **Python script**: Make sure the script is executable: `chmod +x test-discord-webhook.py`
- **Java not found**: Install Java Development Kit (JDK)
- **Python not found**: Install Python 3

### "Error code: 404" or "Error code: 401"
- Your webhook URL is invalid or has been deleted
- Create a new webhook and try again

### "Error code: 429"
- You're being rate-limited by Discord
- Wait a few seconds and try again

### No Message Appears in Discord
- Check that you're looking at the correct channel
- Verify the webhook is configured for the right channel
- Make sure the webhook hasn't been deleted

## Security Note

**Never commit your actual webhook URL to version control!** These scripts mask the webhook token in output for security, but you should still keep your webhook URLs private.

## Integration with Herald Plugin

Once you've verified your webhook URL works with these scripts, you can configure it in your Herald plugin's `config.yml`:

```yaml
discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/123456/abcdef"
```

The plugin uses the same webhook delivery mechanism as these test scripts, but sends a randomly-selected, medieval-themed join message instead of the scripts' fixed test string. See `discord.join-messages` in the [Configuration Guide](CONFIG.md) to customize those messages.

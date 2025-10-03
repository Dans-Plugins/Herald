#!/usr/bin/env python3

"""
Discord Webhook Test Script
This script allows you to test Discord webhook notifications without running a Minecraft server
"""

import sys
import json
import urllib.request
import urllib.error

def send_discord_message(webhook_url, content):
    """Send a message to Discord via webhook"""
    if not webhook_url:
        raise ValueError("Discord webhook URL is not configured")
    
    # Create JSON payload
    payload = {
        "content": content
    }
    
    data = json.dumps(payload).encode('utf-8')
    
    print(f"Sending payload: {json.dumps(payload)}")
    
    # Create request
    req = urllib.request.Request(
        webhook_url,
        data=data,
        headers={'Content-Type': 'application/json'}
    )
    
    # Send request
    try:
        with urllib.request.urlopen(req) as response:
            status_code = response.getcode()
            print(f"Response code: {status_code}")
            
            if status_code < 200 or status_code >= 300:
                raise Exception(f"Discord webhook returned error code: {status_code}")
            
            return True
    except urllib.error.HTTPError as e:
        print(f"Response code: {e.code}")
        raise Exception(f"Discord webhook returned error code: {e.code}")

def mask_webhook_url(url):
    """Mask the webhook token for security"""
    if "/webhooks/" in url:
        parts = url.split("/webhooks/")
        if len(parts) > 1:
            token_parts = parts[1].split("/")
            if len(token_parts) > 1:
                return f"{parts[0]}/webhooks/{token_parts[0]}/****"
    return url

def main():
    # Colors for terminal output
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    NC = '\033[0m'  # No Color
    
    print(f"{BLUE}========================================{NC}")
    print(f"{BLUE}Discord Webhook Test Script (Python){NC}")
    print(f"{BLUE}========================================{NC}")
    print()
    
    # Check arguments
    if len(sys.argv) < 2:
        print(f"{RED}Error: Discord webhook URL is required{NC}")
        print()
        print("Usage: python3 test-discord-webhook.py <webhook-url> [player-name] [server-name]")
        print()
        print("Example:")
        print("  python3 test-discord-webhook.py https://discord.com/api/webhooks/123456/abcdef")
        print("  python3 test-discord-webhook.py https://discord.com/api/webhooks/123456/abcdef Steve")
        print('  python3 test-discord-webhook.py https://discord.com/api/webhooks/123456/abcdef Steve "My Awesome Server"')
        print()
        print(f"{YELLOW}To get a Discord webhook URL:{NC}")
        print("  1. Go to your Discord server settings")
        print("  2. Navigate to Integrations → Webhooks")
        print("  3. Click 'New Webhook'")
        print("  4. Configure the webhook and copy the URL")
        print()
        sys.exit(1)
    
    webhook_url = sys.argv[1]
    player_name = sys.argv[2] if len(sys.argv) > 2 else "TestPlayer"
    server_name = sys.argv[3] if len(sys.argv) > 3 else "Minecraft"
    
    print("Testing Discord webhook...")
    print(f"Webhook URL: {mask_webhook_url(webhook_url)}")
    print(f"Player Name: {player_name}")
    print(f"Server Name: {server_name}")
    print()
    
    # Format message like the plugin does
    message = f"**{player_name}** joined the **{server_name}** server"
    
    print(f"{YELLOW}Sending test message...{NC}")
    print()
    
    try:
        send_discord_message(webhook_url, message)
        print()
        print(f"{GREEN}✓ SUCCESS: Message sent to Discord!{NC}")
        print("Check your Discord channel for the notification.")
        print()
        print(f"{GREEN}========================================{NC}")
        print(f"{GREEN}Test completed successfully!{NC}")
        print(f"{GREEN}========================================{NC}")
        sys.exit(0)
    except Exception as e:
        print()
        print(f"{RED}✗ ERROR: Failed to send message to Discord{NC}")
        print(f"Error: {str(e)}")
        print()
        print(f"{RED}========================================{NC}")
        print(f"{RED}Test failed!{NC}")
        print(f"{RED}========================================{NC}")
        sys.exit(1)

if __name__ == "__main__":
    main()

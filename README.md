# Herald
Herald is a Minecraft server plugin that sends email notifications when players join the server.

![screenshot of emails](./screenshots/mailhog-7-1-2025.png)

## Features
- Email notification system for player logins
- Easy to configure and use
- Built-in mail server setup with Docker

## Requirements
- Minecraft server with version 1.16 or higher
- Java runtime environment
- Docker and Docker Compose (for mail server setup)

## Installation
1. Download the Herald plugin JAR file
2. Place the JAR file in your server's `plugins` folder
3. Restart your server or use a plugin manager to load the plugin
4. Configure the plugin settings as needed

## Email Server Setup
Herald requires an email server to send notifications. We've provided a Docker Compose setup that makes this easy.

### Quick Start
1. Make sure Docker and Docker Compose are installed on your system
2. Use the included `compose.yml` file to start the mail services:

    ```shell script
    docker compose up -d
    ```

3. Configure your Herald plugin to use the mail server (see configuration section below)
4. Access the MailHog web interface at http://localhost:8025 to view all sent emails

### Mail Server Architecture
This setup creates two mail-related services:

- **mailserver**: A Postfix mail server that accepts emails from your Herald plugin
- **mailhog**: A mail catcher that captures all outgoing emails for easy viewing

All emails sent to the mail server are relayed to MailHog, where you can view them in a convenient web interface.

## Configuration
After first run, a configuration file will be created that you can modify to set up your email notification preferences.

### Herald Plugin Configuration
Update your Herald `config.yml` to use the mail server:

```yaml
# Herald Configuration

# Email Recipients
# List of email addresses to notify when a player joins
email-recipients:
  - "admin@example.com"
  - "moderator@example.com"

# SMTP Configuration
smtp:
  server: "mailserver"   # Use "localhost" if Herald is on the same machine
  port: 25               # Standard SMTP port
  username: ""           # No authentication needed
  password: ""
  use-tls: false

email:
  sender: "minecraft@minecraft-mail.local"
```

Make sure to replace "mailserver" with your server's IP address if your Minecraft server is not running in the same Docker network.

## Testing
The Docker Compose setup includes a test Minecraft server that can be used to test the Herald plugin:

```yaml
services:
  testmcserver:
    build: .
    image: herald-test-mc-server
    container_name: herald-test-mc-server
    ports:
      - "25565:25565"
    volumes:
      - type: bind
        source: ./testmcserver
        target: /testmcserver
    environment:
      - MINECRAFT_VERSION=${MINECRAFT_VERSION}
      - OPERATOR_UUID=${OPERATOR_UUID}
      - OPERATOR_NAME=${OPERATOR_NAME}
      - OPERATOR_LEVEL=${OPERATOR_LEVEL}
      - OVERWRITE_EXISTING_SERVER=${OVERWRITE_EXISTING_SERVER}
    networks:
      - mail-network
```

Environment variables can be configured in a `.env` file (sample provided).

## Viewing Emails
All emails sent by the Herald plugin will be captured by MailHog. To view them:

1. Open your web browser
2. Go to http://localhost:8025
3. View and inspect all sent emails in the MailHog interface

## Troubleshooting
- **Emails not showing up in MailHog**: Make sure your Herald plugin is configured with the correct server address and port.
- **Connection refused errors**: Verify that the Docker containers are running with `docker ps` and that you're using the correct address.
- **Authentication failures**: This setup doesn't require authentication by default; make sure username and password fields are empty in your Herald config.

## Production Usage
This setup is primarily intended for development and testing. For production use:

1. Remove the `RELAYHOST` environment variable from the mailserver service
2. Configure proper DNS records for your mail server
3. Set up TLS certificates for secure email transmission
4. Consider adding spam protection measures

## Building from Source
The project uses Gradle for building:

    ```shell script
    ./gradlew build
    ```

This will create a fat JAR with all dependencies included.

## Authors
- Daniel McCoy Stephenson
package com.dansplugins.herald;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * What {@link Herald#notifyPlayerJoin} does with each notifier when a player joins: every
 * channel is tried even when an earlier one fails, and each outcome is logged under the
 * channel's display name. Stub notifiers stand in for Discord and email, so nothing is sent.
 */
class HeraldPlayerJoinTest {

    private final List<LogRecord> logRecords = Collections.synchronizedList(new ArrayList<>());
    private final List<String> calls = new ArrayList<>();
    private Logger logger;

    @BeforeEach
    void createCapturingLogger() {
        logger = Logger.getLogger("HeraldPlayerJoinTest-" + UUID.randomUUID());
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logRecords.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    /** A notifier that records each call and, when {@code failure} is set, throws it. */
    private Notifier stub(String displayName, Exception failure) {
        return new Notifier() {
            @Override
            public void notifyPlayerJoin(String playerName, String serverName) throws Exception {
                calls.add(displayName + ":" + playerName + "@" + serverName);
                if (failure != null) {
                    throw failure;
                }
            }

            @Override
            public String getDisplayName() {
                return displayName;
            }
        };
    }

    @Test
    void everyNotifierIsCalledInOrderWithTheNamesUnchanged() {
        Herald.notifyPlayerJoin(List.of(stub("Discord", null), stub("email", null)),
                "Steve", "My Server", logger);

        assertEquals(List.of("Discord:Steve@My Server", "email:Steve@My Server"), calls);
    }

    @Test
    void aFailingNotifierDoesNotStopTheNextOne() {
        Herald.notifyPlayerJoin(List.of(stub("Discord", new RuntimeException("boom")), stub("email", null)),
                "Steve", "My Server", logger);

        assertEquals(List.of("Discord:Steve@My Server", "email:Steve@My Server"), calls);
    }

    @Test
    void aCheckedExceptionDoesNotStopTheNextOneEither() {
        Herald.notifyPlayerJoin(List.of(stub("email", new java.io.IOException("refused")), stub("Discord", null)),
                "Alex", "Minecraft", logger);

        assertEquals(List.of("email:Alex@Minecraft", "Discord:Alex@Minecraft"), calls);
    }

    @Test
    void aSuccessIsLoggedAtInfoNamingTheChannel() {
        Herald.notifyPlayerJoin(List.of(stub("Discord", null)), "Steve", "My Server", logger);

        assertEquals(1, logRecords.size());
        LogRecord record = logRecords.get(0);
        assertEquals(Level.INFO, record.getLevel());
        assertEquals("Notification sent successfully for player: Steve via Discord", record.getMessage());
    }

    @Test
    void aFailureIsLoggedAtSevereNamingTheChannelWithTheExceptionAttached() {
        RuntimeException failure = new RuntimeException("Invalid Webhook Token");

        Herald.notifyPlayerJoin(List.of(stub("Discord", failure)), "Steve", "My Server", logger);

        assertEquals(1, logRecords.size());
        LogRecord record = logRecords.get(0);
        assertEquals(Level.SEVERE, record.getLevel());
        assertEquals("Failed to send notification via Discord: Invalid Webhook Token", record.getMessage());
        assertSame(failure, record.getThrown());
    }

    @Test
    void noNotifiersMeansNothingIsLogged() {
        Herald.notifyPlayerJoin(List.of(), "Steve", "My Server", logger);

        assertEquals(List.of(), calls);
        assertEquals(0, logRecords.size());
    }
}

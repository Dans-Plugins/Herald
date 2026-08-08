package com.dansplugins.herald;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the resolution of the configured {@code server-name} into the value
 * that fills the {@code {server}} placeholder.
 */
class ServerNameTest {

    @Nested
    @DisplayName("Fallback Tests")
    class FallbackTests {

        @Test
        @DisplayName("An absent server name should fall back to the default")
        void testNullFallsBackToDefault() {
            assertEquals(ServerName.DEFAULT, ServerName.resolve(null));
        }

        @Test
        @DisplayName("An empty server name should fall back to the default")
        void testEmptyFallsBackToDefault() {
            assertEquals(ServerName.DEFAULT, ServerName.resolve(""));
        }

        @Test
        @DisplayName("A whitespace-only server name should fall back to the default")
        void testWhitespaceOnlyFallsBackToDefault() {
            String[] blankValues = {" ", "   ", "\t", "\n", " \t\n "};

            for (String blank : blankValues) {
                assertEquals(ServerName.DEFAULT, ServerName.resolve(blank),
                        "A server name of only whitespace should be treated as blank");
            }
        }

        @Test
        @DisplayName("The default should be a name an operator would recognise, not an empty string")
        void testDefaultIsUsable() {
            assertEquals("Minecraft", ServerName.DEFAULT);
        }
    }

    @Nested
    @DisplayName("Configured Name Tests")
    class ConfiguredNameTests {

        @Test
        @DisplayName("A configured server name should be used as it is written")
        void testConfiguredNameIsKept() {
            assertEquals("My Awesome Server", ServerName.resolve("My Awesome Server"));
        }

        @Test
        @DisplayName("Surrounding whitespace should be stripped from a configured server name")
        void testSurroundingWhitespaceIsStripped() {
            assertEquals("MySurvivalServer", ServerName.resolve("  MySurvivalServer  "));
        }

        @Test
        @DisplayName("Whitespace inside a configured server name should be left alone")
        void testInnerWhitespaceIsKept() {
            assertEquals("The  Old  Kingdom", ServerName.resolve("  The  Old  Kingdom  "));
        }

        @Test
        @DisplayName("A configured name of a single character should be kept")
        void testSingleCharacterNameIsKept() {
            assertEquals("A", ServerName.resolve("A"));
        }

        @Test
        @DisplayName("A Unicode server name should be kept intact")
        void testUnicodeNameIsKept() {
            assertEquals("サーバー", ServerName.resolve("サーバー"));
        }
    }

    @Nested
    @DisplayName("Placeholder Filling Tests")
    class PlaceholderFillingTests {

        @Test
        @DisplayName("A whitespace-only server name should never reach a Discord message")
        void testWhitespaceNameDoesNotReachDiscordMessage() {
            String template = DiscordNotifier.DEFAULT_JOIN_MESSAGES.get(0);

            String filled = template
                    .replace("{player}", "Steve")
                    .replace("{server}", ServerName.resolve("   "));

            assertFalse(filled.contains("****"),
                    "Bold markdown wrapped around nothing means the fallback was bypassed: " + filled);
            assertTrue(filled.contains("**" + ServerName.DEFAULT + "**"),
                    "The fallback name should be bolded in the message: " + filled);
        }

        @Test
        @DisplayName("A whitespace-only server name should never reach an email subject")
        void testWhitespaceNameDoesNotReachEmailSubject() {
            String filled = EmailNotifier.DEFAULT_SUBJECT
                    .replace("{player}", "Steve")
                    .replace("{server}", ServerName.resolve(" "));

            assertEquals("Steve joined Minecraft server", filled);
        }
    }
}

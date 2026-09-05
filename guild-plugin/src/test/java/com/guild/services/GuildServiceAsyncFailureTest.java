package com.guild.services;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P6-c：Service 层 guardServiceFuture / logAsyncFailure 行为单测。
 */
class GuildServiceAsyncFailureTest {

    private final List<LogRecord> logRecords = new ArrayList<>();
    private Handler capturingHandler;
    private Logger logger;
    private TestSupport support;

    @BeforeEach
    void setUp() {
        GuildPlugin plugin = mock(GuildPlugin.class);
        when(plugin.getDatabaseManager()).thenReturn(mock(DatabaseManager.class));

        logger = Logger.getLogger("GuildServiceAsyncFailureTest");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        capturingHandler = new Handler() {
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
        };
        logger.addHandler(capturingHandler);
        when(plugin.getLogger()).thenReturn(logger);

        support = new TestSupport(new GuildServiceContext(plugin));
    }

    @AfterEach
    void tearDown() {
        if (capturingHandler != null) {
            logger.removeHandler(capturingHandler);
        }
    }

    @Test
    void guardServiceFutureBoolean_returnsFalseAndLogsOnFailure() {
        RuntimeException error = new RuntimeException("db down");

        assertFalse(support.runFailingBoolean("testOperation", error).join());
        assertEquals(1, logRecords.size());

        LogRecord record = logRecords.get(0);
        assertEquals(Level.SEVERE, record.getLevel());
        assertEquals("Guild service failed: testOperation", record.getMessage());
        assertEquals(error, record.getThrown());
    }

    @Test
    void guardServiceFuture_unwrapsCompletionExceptionCauseInLog() {
        IllegalStateException root = new IllegalStateException("root cause");
        CompletionException wrapped = new CompletionException(root);

        assertTrue(support.runIsGuildFullGuard(wrapped).join());
        assertEquals(1, logRecords.size());

        LogRecord record = logRecords.get(0);
        assertEquals(Level.SEVERE, record.getLevel());
        assertEquals("Guild service failed: isGuildFull", record.getMessage());
        assertEquals(root, record.getThrown());
    }

    @Test
    void guardServiceFutureNullable_returnsNullOnFailure() {
        assertNull(support.runFailingNullable("getGuildById",
                new RuntimeException("timeout")).join());
        assertEquals(1, logRecords.size());
        assertEquals("Guild service failed: getGuildById", logRecords.get(0).getMessage());
    }

    /** 暴露 protected guard 方法供同包单测使用。 */
    private static final class TestSupport extends GuildServiceSupport {
        TestSupport(GuildServiceContext ctx) {
            super(ctx);
        }

        CompletableFuture<Boolean> runFailingBoolean(String operation, Throwable error) {
            return guardServiceFutureBoolean(operation, CompletableFuture.failedFuture(error));
        }

        CompletableFuture<Boolean> runIsGuildFullGuard(Throwable error) {
            return guardServiceFuture("isGuildFull", CompletableFuture.failedFuture(error), true);
        }

        CompletableFuture<Object> runFailingNullable(String operation, Throwable error) {
            return guardServiceFutureNullable(operation, CompletableFuture.failedFuture(error));
        }
    }
}

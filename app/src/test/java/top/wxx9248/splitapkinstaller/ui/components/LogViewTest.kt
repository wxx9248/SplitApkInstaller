package top.wxx9248.splitapkinstaller.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for LogView component and related logging functionality
 */
class LogViewTest {

    @Test
    fun `MemoryLogger should add logs correctly`() {
        val logger = MemoryLogger()

        logger.info("Test info message")
        logger.warning("Test warning message")
        logger.error("Test error message")
        logger.success("Test success message")

        val logs = logger.getLogs()
        assertEquals(4, logs.size)

        assertEquals(LogLevel.INFO, logs[0].level)
        assertEquals("Test info message", logs[0].message)

        assertEquals(LogLevel.WARNING, logs[1].level)
        assertEquals("Test warning message", logs[1].message)

        assertEquals(LogLevel.ERROR, logs[2].level)
        assertEquals("Test error message", logs[2].message)

        assertEquals(LogLevel.SUCCESS, logs[3].level)
        assertEquals("Test success message", logs[3].message)
    }

    @Test
    fun `MemoryLogger should clear logs correctly`() {
        val logger = MemoryLogger()

        logger.info("Test message 1")
        logger.info("Test message 2")

        assertEquals(2, logger.getLogs().size)

        logger.clearLogs()

        assertEquals(0, logger.getLogs().size)
    }

    @Test
    fun `LogEntry should have correct timestamp`() {
        val beforeTime = System.currentTimeMillis()
        val logEntry = LogEntry(level = LogLevel.INFO, message = "Test message")
        val afterTime = System.currentTimeMillis()

        assertTrue(
            "Timestamp should be between before and after time",
            logEntry.timestamp >= beforeTime && logEntry.timestamp <= afterTime
        )
    }

    @Test
    fun `LogEntry should support custom timestamp`() {
        val customTimestamp = 1234567890L
        val logEntry = LogEntry(
            timestamp = customTimestamp,
            level = LogLevel.ERROR,
            message = "Custom timestamp message"
        )

        assertEquals(customTimestamp, logEntry.timestamp)
        assertEquals(LogLevel.ERROR, logEntry.level)
        assertEquals("Custom timestamp message", logEntry.message)
    }

    @Test
    fun `Logger interface should support all log levels`() {
        val logger = MemoryLogger()

        // Test all convenience methods
        logger.info("Info")
        logger.warning("Warning")
        logger.error("Error")
        logger.success("Success")

        val logs = logger.getLogs()
        assertEquals(4, logs.size)

        val levels = logs.map { it.level }
        assertTrue(levels.contains(LogLevel.INFO))
        assertTrue(levels.contains(LogLevel.WARNING))
        assertTrue(levels.contains(LogLevel.ERROR))
        assertTrue(levels.contains(LogLevel.SUCCESS))
    }

    @Test
    fun `Logger should support direct log entry addition`() {
        val logger = MemoryLogger()
        val customLog = LogEntry(
            timestamp = 9876543210L,
            level = LogLevel.WARNING,
            message = "Direct log entry"
        )

        logger.addLog(customLog)

        val logs = logger.getLogs()
        assertEquals(1, logs.size)
        assertEquals(customLog, logs[0])
    }
}
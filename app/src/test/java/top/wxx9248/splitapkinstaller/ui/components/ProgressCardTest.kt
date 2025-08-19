package top.wxx9248.splitapkinstaller.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ProgressCard component and related classes.
 * These tests demonstrate the testability and API usage of the ProgressCard component.
 */
class ProgressCardTest {

    @Test
    fun `ProgressState enum should have all expected values`() {
        val states = ProgressState.values()
        assertEquals(4, states.size)
        assertTrue(states.contains(ProgressState.IDLE))
        assertTrue(states.contains(ProgressState.IN_PROGRESS))
        assertTrue(states.contains(ProgressState.COMPLETED_SUCCESS))
        assertTrue(states.contains(ProgressState.COMPLETED_FAILURE))
    }

    @Test
    fun `ProgressTexts should be created with all required fields`() {
        val texts = ProgressTexts(
            title = "Test Progress",
            inProgressText = "Processing...",
            completedSuccessText = "Success",
            completedFailureText = "Failed",
            filesProcessedFormat = "%d / %d files processed"
        )

        assertEquals("Test Progress", texts.title)
        assertEquals("Processing...", texts.inProgressText)
        assertEquals("Success", texts.completedSuccessText)
        assertEquals("Failed", texts.completedFailureText)
        assertEquals("%d / %d files processed", texts.filesProcessedFormat)
    }

    @Test
    fun `ProgressTexts should support custom files processed format`() {
        val texts = ProgressTexts(
            title = "Download Progress",
            inProgressText = "Downloading...",
            completedSuccessText = "Downloaded",
            completedFailureText = "Download Failed",
            filesProcessedFormat = "%d of %d items downloaded"
        )

        assertEquals("%d of %d items downloaded", texts.filesProcessedFormat)
    }

    @Test
    fun `ProgressTexts should work with String format`() {
        val texts = ProgressTexts(
            title = "Test",
            inProgressText = "Processing...",
            completedSuccessText = "Success",
            completedFailureText = "Failed",
            filesProcessedFormat = "%d out of %d completed"
        )

        val formatted = String.format(texts.filesProcessedFormat, 5, 10)
        assertEquals("5 out of 10 completed", formatted)
    }

    @Test
    fun `ProgressState should be comparable for state transitions`() {
        // Test that we can compare states for logical transitions
        val idleState = ProgressState.IDLE
        val inProgressState = ProgressState.IN_PROGRESS
        val successState = ProgressState.COMPLETED_SUCCESS
        val failureState = ProgressState.COMPLETED_FAILURE

        // These are just basic enum comparisons to ensure the enum works as expected
        assertNotEquals(idleState, inProgressState)
        assertNotEquals(inProgressState, successState)
        assertNotEquals(successState, failureState)
        assertEquals(idleState, ProgressState.IDLE)
    }

    @Test
    fun `ProgressTexts should support installation scenario`() {
        val installationTexts = ProgressTexts(
            title = "Installation Progress",
            inProgressText = "Installing...",
            completedSuccessText = "Installation Complete",
            completedFailureText = "Installation Failed",
            filesProcessedFormat = "%d / %d files processed"
        )

        assertEquals("Installation Progress", installationTexts.title)
        assertEquals("Installing...", installationTexts.inProgressText)
        assertEquals("Installation Complete", installationTexts.completedSuccessText)
        assertEquals("Installation Failed", installationTexts.completedFailureText)
    }

    @Test
    fun `ProgressTexts should support download scenario`() {
        val downloadTexts = ProgressTexts(
            title = "Download Progress",
            inProgressText = "Downloading...",
            completedSuccessText = "Download Complete",
            completedFailureText = "Download Failed",
            filesProcessedFormat = "%d / %d files downloaded"
        )

        assertEquals("Download Progress", downloadTexts.title)
        assertEquals("Downloading...", downloadTexts.inProgressText)
        assertEquals("Download Complete", downloadTexts.completedSuccessText)
        assertEquals("Download Failed", downloadTexts.completedFailureText)
        assertEquals("%d / %d files downloaded", downloadTexts.filesProcessedFormat)
    }

    @Test
    fun `ProgressTexts should support backup scenario`() {
        val backupTexts = ProgressTexts(
            title = "Backup Progress",
            inProgressText = "Creating backup...",
            completedSuccessText = "Backup Created",
            completedFailureText = "Backup Failed",
            filesProcessedFormat = "%d / %d items backed up"
        )

        assertEquals("Backup Progress", backupTexts.title)
        assertEquals("Creating backup...", backupTexts.inProgressText)
        assertEquals("Backup Created", backupTexts.completedSuccessText)
        assertEquals("Backup Failed", backupTexts.completedFailureText)
        assertEquals("%d / %d items backed up", backupTexts.filesProcessedFormat)
    }

    @Test
    fun `ProgressCard API should be well-defined for different use cases`() {
        // Test that the API supports various scenarios through ProgressTexts
        val scenarios = listOf(
            ProgressTexts(
                title = "File Transfer",
                inProgressText = "Transferring...",
                completedSuccessText = "Transfer Complete",
                completedFailureText = "Transfer Failed",
                filesProcessedFormat = "%d / %d files processed"
            ),
            ProgressTexts(
                title = "Data Sync",
                inProgressText = "Syncing...",
                completedSuccessText = "Sync Complete",
                completedFailureText = "Sync Failed",
                filesProcessedFormat = "%d / %d records synced"
            ),
            ProgressTexts(
                title = "Image Processing",
                inProgressText = "Processing images...",
                completedSuccessText = "Processing Complete",
                completedFailureText = "Processing Failed",
                filesProcessedFormat = "%d / %d images processed"
            )
        )

        // Verify each scenario has the expected structure
        scenarios.forEach { texts ->
            assertNotNull(texts.title)
            assertNotNull(texts.inProgressText)
            assertNotNull(texts.completedSuccessText)
            assertNotNull(texts.completedFailureText)
            assertNotNull(texts.filesProcessedFormat)
            assertTrue(texts.title.isNotEmpty())
            assertTrue(texts.inProgressText.isNotEmpty())
            assertTrue(texts.completedSuccessText.isNotEmpty())
            assertTrue(texts.completedFailureText.isNotEmpty())
            assertTrue(texts.filesProcessedFormat.contains("%d"))
        }
    }
}

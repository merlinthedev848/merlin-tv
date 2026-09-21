package com.example.merlinmedia.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun testIsNewerVersion() {
        // Newer minor/patch versions
        assertTrue(UpdateManager.isNewerVersion("1.4.4", "1.4.3"))
        assertTrue(UpdateManager.isNewerVersion("1.5.0", "1.4.3"))
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "1.4.3"))
        assertTrue(UpdateManager.isNewerVersion("v1.4.4", "1.4.3"))

        // Same or older versions
        assertFalse(UpdateManager.isNewerVersion("1.4.3", "1.4.3"))
        assertFalse(UpdateManager.isNewerVersion("1.4.2", "1.4.3"))
        assertFalse(UpdateManager.isNewerVersion("1.3.9", "1.4.3"))
        assertFalse(UpdateManager.isNewerVersion("", "1.4.3"))
    }
}

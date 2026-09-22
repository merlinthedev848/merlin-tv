package com.example.merlinmedia.updater

import org.junit.Assert.*
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun `isNewerVersion returns true when remote is higher major`() {
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "1.5.0"))
    }

    @Test
    fun `isNewerVersion returns true when remote is higher minor`() {
        assertTrue(UpdateManager.isNewerVersion("1.6.0", "1.5.0"))
    }

    @Test
    fun `isNewerVersion returns true when remote is higher patch`() {
        assertTrue(UpdateManager.isNewerVersion("1.5.1", "1.5.0"))
    }

    @Test
    fun `isNewerVersion returns false when versions are equal`() {
        assertFalse(UpdateManager.isNewerVersion("1.5.0", "1.5.0"))
    }

    @Test
    fun `isNewerVersion returns false when remote is lower`() {
        assertFalse(UpdateManager.isNewerVersion("1.4.0", "1.5.0"))
    }

    @Test
    fun `isNewerVersion handles blank strings`() {
        assertFalse(UpdateManager.isNewerVersion("", "1.5.0"))
        assertFalse(UpdateManager.isNewerVersion("1.5.0", ""))
        assertFalse(UpdateManager.isNewerVersion("", ""))
    }

    @Test
    fun `isNewerVersion handles different number of segments`() {
        assertTrue(UpdateManager.isNewerVersion("1.5.1", "1.5"))
        assertTrue(UpdateManager.isNewerVersion("2.0", "1.5.0"))
    }

    @Test
    fun `isNewerVersion handles suffixes correctly`() {
        // Build suffixes after - _ + are stripped before comparison
        assertTrue(UpdateManager.isNewerVersion("2.0.0-beta", "1.5.0"))
        assertTrue(UpdateManager.isNewerVersion("2.0.0+build123", "1.5.0"))
        assertFalse(UpdateManager.isNewerVersion("1.5.0-rc1", "1.5.0"))
    }

    @Test
    fun `isNewerVersion handles large version numbers`() {
        assertTrue(UpdateManager.isNewerVersion("10.0.0", "9.99.99"))
        assertTrue(UpdateManager.isNewerVersion("1.100.0", "1.99.0"))
    }
}

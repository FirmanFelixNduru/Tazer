package com.tazer.taskorganizer.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConflictTest {
    @Test
    fun versionsTakePrecedenceAndWriterBreaksTies() {
        val lowerWriter = "11111111-1111-1111-1111-111111111111"
        val higherWriter = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        assertTrue(versionWins(3, lowerWriter, 2, higherWriter))
        assertFalse(versionWins(1, higherWriter, 2, lowerWriter))
        assertTrue(versionWins(2, higherWriter, 2, lowerWriter))
        assertFalse(versionWins(2, lowerWriter, 2, higherWriter))
        assertFalse(versionWins(2, higherWriter, 2, higherWriter))
    }
}

package com.tazer.taskorganizer.integration

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class AuthPkceTest {
    @Test
    fun stateIsIndependentAndChallengesMatchS256() {
        val now = Instant.EPOCH
        val pending = PendingAuth.create(AuthFlow.GOOGLE, now)
        val another = PendingAuth.create(AuthFlow.GOOGLE, now)
        assertNotEquals(pending.state, another.state)
        assertNotEquals(pending.state, pending.verifier)
        assertEquals(43, pending.state.length)
        val reference = PendingAuth(AuthFlow.GOOGLE, "state", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", 900)
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", reference.challenge)
    }

    @Test
    fun rejectsReplayedStateAndExpiredCallbacksAndRecognizesCancellation() {
        val pending = PendingAuth.create(AuthFlow.GOOGLE, Instant.EPOCH)
        fun validate(
            states: List<String> = listOf(pending.state), codes: List<String> = listOf("sample-code"),
            errors: List<String> = emptyList(), now: Instant = Instant.EPOCH, fragment: Boolean = false,
        ) = pending.validateCallback(now, states, codes, errors, emptyList(), fragment)
        assertEquals("sample-code", validate())
        fun rejects(block: () -> String) {
            assertEquals(IntegrationError.INVALID_CALLBACK, assertThrows(IntegrationException::class.java) { block() }.code)
        }
        rejects { validate(states = listOf(pending.state, pending.state)) }
        rejects { validate(now = Instant.ofEpochSecond(900)) }
        rejects { validate(fragment = true) }
        rejects { validate(states = listOf("wrong"), codes = emptyList(), errors = listOf("access_denied")) }
        val cancelled = assertThrows(IntegrationException::class.java) {
            validate(codes = emptyList(), errors = listOf("access_denied"), fragment = true)
        }
        assertEquals(IntegrationError.GOOGLE_CANCELLED, cancelled.code)
    }
}

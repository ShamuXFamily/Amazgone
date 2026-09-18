package com.cikup.amazgone.account

import com.cikup.amazgone.account.domain.model.CredentialsValidator
import com.cikup.amazgone.core.domain.ValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CredentialsValidatorTest {
    @Test
    fun usernamesAreNormalisedAndValidated() {
        assertNull(CredentialsValidator.validateUsername("  Shopper_01 "))
        assertEquals(ValidationReason.EMPTY, CredentialsValidator.validateUsername("   ")?.reason)
        assertEquals(ValidationReason.TOO_SHORT, CredentialsValidator.validateUsername("ab")?.reason)
        assertEquals(ValidationReason.TOO_LONG, CredentialsValidator.validateUsername("a".repeat(21))?.reason)
        assertEquals(ValidationReason.INVALID_CHARACTERS, CredentialsValidator.validateUsername("bad name!")?.reason)
    }

    @Test
    fun passwordsNeedLengthAndMatchingConfirmation() {
        assertNull(CredentialsValidator.validatePassword("longenough", "longenough"))
        assertEquals(ValidationReason.TOO_SHORT, CredentialsValidator.validatePassword("short")?.reason)
        val mismatch = CredentialsValidator.validatePassword("longenough", "different1")
        assertEquals(ValidationReason.MISMATCH, mismatch?.reason)
        assertEquals(CredentialsValidator.FIELD_CONFIRMATION, mismatch?.field)
    }

    @Test
    fun syntheticEmailIsCaseInsensitive() {
        assertEquals(CredentialsValidator.syntheticEmail("Bob"), CredentialsValidator.syntheticEmail("bob "))
    }
}

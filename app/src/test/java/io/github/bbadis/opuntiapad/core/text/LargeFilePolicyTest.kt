package io.github.bbadis.opuntiapad.core.text

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeFilePolicyTest {
    @Test
    fun `heavy grammars stop colouring earlier`() {
        assertTrue(LargeFilePolicy.highlightByDefault("yaml", 200_000))
        assertFalse(LargeFilePolicy.highlightByDefault("yaml", 300_000))
        assertTrue(LargeFilePolicy.highlightByDefault("json", 300_000))
        assertFalse(LargeFilePolicy.highlightByDefault("json", 3_000_000))
    }
}

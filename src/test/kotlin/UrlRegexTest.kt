package de.gianttree.discord.w2g

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UrlRegexTest {

    @Test
    fun prependedText() {
        assertNotNull(urlRegex.find("Mango hat es doch noch geschafft rechtzeitig https://www.youtube.com/watch?v=DryGQNvfM7c"))
    }

    @Test
    fun onlyUrl() {
        assertNotNull(urlRegex.find("https://www.youtube.com/watch?v=DryGQNvfM7c"))
    }

    @Test
    fun onlyText() {
        assertNull(urlRegex.find("Mango hat es doch noch geschafft rechtzeitig"))
    }
}

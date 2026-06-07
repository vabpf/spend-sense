package com.spendsense.data.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountParserTest {

    @Test
    fun testParseAmount_withVariousFormats() {
        // Instantiate NotificationProcessor via Unsafe to bypass constructor null checks
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafeField = unsafeClass.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null)
        val allocateInstanceMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
        val processor = allocateInstanceMethod.invoke(unsafe, NotificationProcessor::class.java) as NotificationProcessor

        // 1. Single comma as thousands separator (e.g. MB Bank Vietnamese format)
        assertEquals(48000.0, processor.parseAmount("-48,000VND"), 0.001)
        assertEquals(230000.0, processor.parseAmount("-230,000VND"), 0.001)
        assertEquals(58000.0, processor.parseAmount("58,000"), 0.001)

        // 2. Dots as thousands separator and comma as decimal separator (European format)
        assertEquals(1234.56, processor.parseAmount("1.234,56"), 0.001)

        // 3. Comma as thousands separator and dot as decimal separator (US/UK format)
        assertEquals(1234.56, processor.parseAmount("1,234.56"), 0.001)

        // 4. Dot as thousands separator (e.g., European format integers)
        assertEquals(58000.0, processor.parseAmount("58.000"), 0.001)
        assertEquals(48000.0, processor.parseAmount("48.000VND"), 0.001)

        // 5. Clean integer
        assertEquals(100.0, processor.parseAmount("100"), 0.001)
        assertEquals(100.0, processor.parseAmount("100VND"), 0.001)

        // 6. Decimals
        assertEquals(12.34, processor.parseAmount("12.34"), 0.001)
        assertEquals(12.34, processor.parseAmount("12,34"), 0.001)
    }
}

package com.raulshma.minkoa

import com.raulshma.minkoa.data.SlotContent
import org.junit.Assert.*
import org.junit.Test

class SlotContentTest {

    @Test
    fun appSerializesAndDeserializes() {
        val original = SlotContent.App("com.example/com.example.MainActivity")
        val serialized = original.serialize()
        val deserialized = SlotContent.deserialize(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun widgetSerializesAndDeserializes() {
        val original = SlotContent.Widget(42, "com.example.widget", "com.example.WidgetProvider", 2, 3)
        val serialized = original.serialize()
        val deserialized = SlotContent.deserialize(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun widgetWithDefaultSpansSerializesAndDeserializes() {
        val original = SlotContent.Widget(42, "com.example.widget", "com.example.WidgetProvider")
        val serialized = original.serialize()
        val deserialized = SlotContent.deserialize(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun appSerializationFormat() {
        val content = SlotContent.App("com.example/com.example.MainActivity")
        assertEquals("app:com.example/com.example.MainActivity", content.serialize())
    }

    @Test
    fun widgetSerializationFormat() {
        val content = SlotContent.Widget(99, "com.pkg", "com.pkg.Provider", 2, 1)
        assertEquals("widget:99:com.pkg:com.pkg.Provider:2:1", content.serialize())
    }

    @Test
    fun widgetSerializationFormatWithDefaultSpans() {
        val content = SlotContent.Widget(99, "com.pkg", "com.pkg.Provider")
        assertEquals("widget:99:com.pkg:com.pkg.Provider:1:1", content.serialize())
    }

    @Test
    fun deserializeInvalidPrefixReturnsNull() {
        assertNull(SlotContent.deserialize("unknown:data"))
    }

    @Test
    fun deserializeEmptyStringReturnsNull() {
        assertNull(SlotContent.deserialize(""))
    }

    @Test
    fun deserializeWidgetWithInvalidIdReturnsNull() {
        assertNull(SlotContent.deserialize("widget:notanumber:com.pkg:com.pkg.Provider"))
    }

    @Test
    fun deserializeWidgetMissingPartsReturnsNull() {
        assertNull(SlotContent.deserialize("widget:42:com.pkg"))
    }

    @Test
    fun deserializeWidgetBackwardsCompatibilityWithoutSpans() {
        val deserialized = SlotContent.deserialize("widget:42:com.pkg:com.pkg.Provider")
        assertNotNull(deserialized)
        val widget = deserialized as SlotContent.Widget
        assertEquals(42, widget.appWidgetId)
        assertEquals("com.pkg", widget.providerPkg)
        assertEquals("com.pkg.Provider", widget.providerCls)
        assertEquals(1, widget.spanX)
        assertEquals(1, widget.spanY)
    }

    @Test
    fun appProviderComponentIsNull() {
        val app = SlotContent.App("key")
        assertNull(app.providerComponent)
    }

    @Test
    fun appEquality() {
        val a1 = SlotContent.App("same.key")
        val a2 = SlotContent.App("same.key")
        assertEquals(a1, a2)
    }

    @Test
    fun appInequality() {
        val a1 = SlotContent.App("key1")
        val a2 = SlotContent.App("key2")
        assertNotEquals(a1, a2)
    }

    @Test
    fun widgetEquality() {
        val w1 = SlotContent.Widget(1, "pkg", "cls", 2, 2)
        val w2 = SlotContent.Widget(1, "pkg", "cls", 2, 2)
        assertEquals(w1, w2)
    }

    @Test
    fun widgetInequalityDifferentId() {
        val w1 = SlotContent.Widget(1, "pkg", "cls")
        val w2 = SlotContent.Widget(2, "pkg", "cls")
        assertNotEquals(w1, w2)
    }

    @Test
    fun widgetInequalityDifferentPkg() {
        val w1 = SlotContent.Widget(1, "pkg1", "cls")
        val w2 = SlotContent.Widget(1, "pkg2", "cls")
        assertNotEquals(w1, w2)
    }

    @Test
    fun widgetInequalityDifferentSpan() {
        val w1 = SlotContent.Widget(1, "pkg", "cls", 1, 1)
        val w2 = SlotContent.Widget(1, "pkg", "cls", 2, 1)
        assertNotEquals(w1, w2)
    }

    @Test
    fun appNotEqualToWidget() {
        val app = SlotContent.App("key")
        val widget = SlotContent.Widget(1, "key", "key")
        assertNotEquals(app, widget)
    }
}

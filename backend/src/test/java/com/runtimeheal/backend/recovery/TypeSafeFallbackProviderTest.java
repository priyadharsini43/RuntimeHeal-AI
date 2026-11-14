package com.runtimeheal.backend.recovery;

import com.runtimeheal.backend.recovery.provider.TypeSafeFallbackProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TypeSafeFallbackProviderTest {

    private TypeSafeFallbackProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TypeSafeFallbackProvider();
    }

    @Test
    @DisplayName("Should return 0 for primitive and boxed integers")
    void testIntegerDefaults() {
        assertEquals(0, provider.getFallbackValue("int"));
        assertEquals(0, provider.getFallbackValue("Integer"));
        assertEquals(0, provider.getFallbackValue("java.lang.Integer"));
    }

    @Test
    @DisplayName("Should return 0.0 for double and float types")
    void testFloatingDefaults() {
        assertEquals(0.0, provider.getFallbackValue("double"));
        assertEquals(0.0, provider.getFallbackValue("Double"));
        assertEquals(0.0, provider.getFallbackValue("java.lang.Double"));
        assertEquals(0.0f, provider.getFallbackValue("float"));
        assertEquals(0.0f, provider.getFallbackValue("Float"));
    }

    @Test
    @DisplayName("Should return false for boolean types")
    void testBooleanDefaults() {
        assertEquals(Boolean.FALSE, provider.getFallbackValue("boolean"));
        assertEquals(Boolean.FALSE, provider.getFallbackValue("Boolean"));
        assertEquals(Boolean.FALSE, provider.getFallbackValue("java.lang.Boolean"));
    }

    @Test
    @DisplayName("Should return 0L for long types")
    void testLongDefaults() {
        assertEquals(0L, provider.getFallbackValue("long"));
        assertEquals(0L, provider.getFallbackValue("Long"));
        assertEquals(0L, provider.getFallbackValue("java.lang.Long"));
    }

    @Test
    @DisplayName("Should return safe non-empty string for String return types")
    void testStringDefaults() {
        assertEquals("DEFAULT_FALLBACK", provider.getFallbackValue("String"));
        assertEquals("DEFAULT_FALLBACK", provider.getFallbackValue("java.lang.String"));
    }

    @Test
    @DisplayName("Should return empty collections for List, Map, Set, and Optional")
    void testCollectionAndOptionalDefaults() {
        Object list = provider.getFallbackValue("List");
        assertInstanceOf(List.class, list);
        assertTrue(((List<?>) list).isEmpty());

        Object map = provider.getFallbackValue("Map");
        assertInstanceOf(Map.class, map);
        assertTrue(((Map<?, ?>) map).isEmpty());

        Object set = provider.getFallbackValue("Set");
        assertInstanceOf(Set.class, set);
        assertTrue(((Set<?>) set).isEmpty());

        Object opt = provider.getFallbackValue("Optional");
        assertInstanceOf(Optional.class, opt);
        assertTrue(((Optional<?>) opt).isEmpty());
    }

    @Test
    @DisplayName("Should return null for void return types")
    void testVoidReturn() {
        assertNull(provider.getFallbackValue("void"));
        assertNull(provider.getFallbackValue("V"));
        assertNull(provider.getFallbackValue(null));
        assertNull(provider.getFallbackValue(""));
    }

    @Test
    @DisplayName("Should support custom supplier registrations")
    void testCustomSupplier() {
        provider.registerCustomFallback("com.example.CustomDto", () -> "CUSTOM_MOCK");
        assertEquals("CUSTOM_MOCK", provider.getFallbackValue("com.example.CustomDto"));
    }
}

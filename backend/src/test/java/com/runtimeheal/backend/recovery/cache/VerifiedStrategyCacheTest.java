package com.runtimeheal.backend.recovery.cache;

import com.runtimeheal.backend.llm.model.RecoveryStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VerifiedStrategyCacheTest {

    private VerifiedStrategyCache cache;

    @BeforeEach
    void setUp() {
        cache = new VerifiedStrategyCache();
    }

    @Test
    @DisplayName("Cache should return empty for un-cached method exception signatures")
    void testCacheMiss() {
        Optional<VerifiedStrategyCache.CachedStrategy> result = cache.get("com.example.OrderService", "process", "NullPointerException");
        assertTrue(result.isEmpty());
        assertEquals(0, cache.getTotalHits());
        assertEquals(1, cache.getTotalMisses());
        assertEquals(0.0, cache.getHitRate());
    }

    @Test
    @DisplayName("Cache should store and return verified strategy on cache hit")
    void testCacheHit() {
        cache.put("com.example.OrderService", "calculateDiscount", "ArithmeticException",
                RecoveryStrategy.RETURN_FALLBACK, 0.95, "Arithmetic zero divisor safe default");

        assertEquals(1, cache.size());

        Optional<VerifiedStrategyCache.CachedStrategy> hit = cache.get("com.example.OrderService", "calculateDiscount", "ArithmeticException");
        assertTrue(hit.isPresent());
        assertEquals(RecoveryStrategy.RETURN_FALLBACK, hit.get().getStrategy());
        assertEquals(0.95, hit.get().getConfidenceScore());
        assertEquals(1, cache.getTotalHits());
        assertEquals(0, cache.getTotalMisses());
        assertEquals(100.0, cache.getHitRate());
    }

    @Test
    @DisplayName("Cache should support eviction and clearing")
    void testEvictionAndClear() {
        cache.put("ClassA", "methodA", "ExceptionA", RecoveryStrategy.RETRY, 0.85, "Retry reason");
        cache.put("ClassB", "methodB", "ExceptionB", RecoveryStrategy.SKIP_OPERATION, 0.80, "Skip reason");
        assertEquals(2, cache.size());

        cache.evict("ClassA", "methodA", "ExceptionA");
        assertEquals(1, cache.size());
        assertTrue(cache.get("ClassA", "methodA", "ExceptionA").isEmpty());
        assertTrue(cache.get("ClassB", "methodB", "ExceptionB").isPresent());

        cache.clear();
        assertEquals(0, cache.size());
        assertEquals(0, cache.getTotalHits());
    }
}

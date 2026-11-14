package com.runtimeheal.backend.recovery.cache;

import com.runtimeheal.backend.llm.model.RecoveryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe Verified Strategy Cache providing sub-5ms low-latency recovery
 * by caching previously validated LLM diagnostic strategies for specific method-exception signatures.
 */
@Component
public class VerifiedStrategyCache {

    private static final Logger log = LoggerFactory.getLogger(VerifiedStrategyCache.class);

    public static class CachedStrategy {
        private final String key;
        private final RecoveryStrategy strategy;
        private final double confidenceScore;
        private final String policyReason;
        private final Instant cachedAt;
        private final AtomicLong hitCount = new AtomicLong(0);

        public CachedStrategy(String key, RecoveryStrategy strategy, double confidenceScore, String policyReason) {
            this.key = key;
            this.strategy = strategy;
            this.confidenceScore = confidenceScore;
            this.policyReason = policyReason;
            this.cachedAt = Instant.now();
        }

        public String getKey() { return key; }
        public RecoveryStrategy getStrategy() { return strategy; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getPolicyReason() { return policyReason; }
        public Instant getCachedAt() { return cachedAt; }
        public long getHitCount() { return hitCount.get(); }
        public void incrementHits() { hitCount.incrementAndGet(); }
    }

    private final Map<String, CachedStrategy> cache = new ConcurrentHashMap<>();
    private final AtomicLong totalHits = new AtomicLong(0);
    private final AtomicLong totalMisses = new AtomicLong(0);

    public String buildKey(String className, String methodName, String exceptionType) {
        return (className != null ? className : "") + "#"
                + (methodName != null ? methodName : "") + "#"
                + (exceptionType != null ? exceptionType : "");
    }

    public Optional<CachedStrategy> get(String className, String methodName, String exceptionType) {
        String key = buildKey(className, methodName, exceptionType);
        CachedStrategy entry = cache.get(key);
        if (entry != null) {
            entry.incrementHits();
            totalHits.incrementAndGet();
            log.debug("[Strategy Cache] HIT for key [{}] -> Strategy: {} (hits: {})",
                    key, entry.getStrategy(), entry.getHitCount());
            return Optional.of(entry);
        } else {
            totalMisses.incrementAndGet();
            log.debug("[Strategy Cache] MISS for key [{}]", key);
            return Optional.empty();
        }
    }

    public void put(String className, String methodName, String exceptionType,
                    RecoveryStrategy strategy, double confidenceScore, String reason) {
        String key = buildKey(className, methodName, exceptionType);
        CachedStrategy entry = new CachedStrategy(key, strategy, confidenceScore, reason);
        cache.put(key, entry);
        log.info("[Strategy Cache] Cached verified strategy for key [{}] -> {} (confidence: {})",
                key, strategy, confidenceScore);
    }

    public void evict(String className, String methodName, String exceptionType) {
        String key = buildKey(className, methodName, exceptionType);
        cache.remove(key);
        log.info("[Strategy Cache] Evicted key [{}]", key);
    }

    public void clear() {
        cache.clear();
        totalHits.set(0);
        totalMisses.set(0);
        log.info("[Strategy Cache] Cleared all cache entries");
    }

    public int size() {
        return cache.size();
    }

    public long getTotalHits() {
        return totalHits.get();
    }

    public long getTotalMisses() {
        return totalMisses.get();
    }

    public double getHitRate() {
        long hits = totalHits.get();
        long misses = totalMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total * 100.0 : 0.0;
    }

    public List<CachedStrategy> getAllEntries() {
        return new ArrayList<>(cache.values());
    }
}

package com.prompt.util;

import com.prompt.dto.PromptOptimizeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class PromptOptimizeCache {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public PromptOptimizeCache() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
    }

    public PromptOptimizeResponse get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        log.debug("[DEBUG] Cache hit for key: {}", key);
        return entry.getResponse();
    }

    public void put(String key, PromptOptimizeResponse response, long ttl, TimeUnit unit) {
        long expireTime = System.currentTimeMillis() + unit.toMillis(ttl);
        cache.put(key, new CacheEntry(response, expireTime));
        log.debug("[DEBUG] Cache put for key: {}, ttl: {} {}", key, ttl, unit);
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void clearByUserId(Long userId) {
        String prefix = userId + ":";
        cache.keySet().removeIf(key -> key.startsWith(prefix));
        log.debug("[DEBUG] Cache cleared for user: {}", userId);
    }

    public String generateCacheKey(Long userId, String promptContent) {
        String contentHash = hashPromptContent(promptContent);
        return userId + ":" + contentHash;
    }

    private String hashPromptContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("[DEBUG] Failed to hash prompt content", e);
            return String.valueOf(content.hashCode());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void cleanup() {
        int beforeSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int afterSize = cache.size();
        if (beforeSize != afterSize) {
            log.debug("[DEBUG] Cache cleanup: removed {} expired entries, remaining: {}",
                    beforeSize - afterSize, afterSize);
        }
    }

    private static class CacheEntry {
        private final PromptOptimizeResponse response;
        private final long expireTime;

        CacheEntry(PromptOptimizeResponse response, long expireTime) {
            this.response = response;
            this.expireTime = expireTime;
        }

        PromptOptimizeResponse getResponse() {
            return response;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
}

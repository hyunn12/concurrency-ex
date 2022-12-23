package com.example.stock.repository;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisLockRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public Boolean lock(Long key) {
        return redisTemplate
                .opsForValue()
                .setIfAbsent(generateKey(key), "lock", Duration.ofMillis(3_000));
        // setIfAbsent(key, value, 지속시간): setnx 명령어 설정과 같음
    }

    public Boolean unlock(Long key) {
        return redisTemplate.delete(generateKey(key));
        // delete(key): del 명령어 설정과 같음
    }

    private String generateKey(Long key) {
        return key.toString();
    }

}

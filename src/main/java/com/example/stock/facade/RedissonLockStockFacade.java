package com.example.stock.facade;

import com.example.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedissonLockStockFacade {

    private final RedissonClient redissonClient;
    private final StockService stockService;

    public void decrease(Long key, Long quantity) {
        // RLock 을 통해 lock 설정
        RLock lock = redissonClient.getLock(key.toString());

        try {
            // tryLock(획득시도시간, 점유시간, 시간단위)
            boolean available = lock.tryLock(5, 1, TimeUnit.SECONDS);

            // lock 획득여부
            if (!available) {
                System.out.println("fail to get lock");
                return;
            }

            stockService.decrease(key, quantity);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // lock 해제
            lock.unlock();
        }

    }

}

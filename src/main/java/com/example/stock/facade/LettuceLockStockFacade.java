package com.example.stock.facade;

import com.example.stock.repository.RedisLockRepository;
import com.example.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LettuceLockStockFacade {

    private final RedisLockRepository redisLockRepository;
    private final StockService stockService;

    public void decrease(Long key, Long quantity) throws InterruptedException {
        while (!redisLockRepository.lock(key)) {
            // redis 의 부하를 줄이기 위해 thread.sleep 설정
            Thread.sleep(100);
        }

        try {
            stockService.decrease(key, quantity);
        } finally {
            // lock 해제
            redisLockRepository.unlock(key);
        }
    }

}

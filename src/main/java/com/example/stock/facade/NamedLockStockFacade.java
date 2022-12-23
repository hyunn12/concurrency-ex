package com.example.stock.facade;

import com.example.stock.repository.MysqlLockRepository;
import com.example.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NamedLockStockFacade {

    private final MysqlLockRepository mysqlLockRepository;

    private final StockService stockService;

    public void decrease(Long id, Long quantity) {
        try {
            mysqlLockRepository.getLock(id.toString());
            stockService.namedLockDecrease(id, quantity);
        } finally {
            mysqlLockRepository.releaseLock(id.toString());
        }
    }

}

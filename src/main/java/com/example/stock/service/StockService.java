package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional
    public void decrease(Long id, Long quantity) {
        // get stock
        Stock stock = stockRepository.findById(id).orElseThrow();

        // 재고감소
        stock.decrease(quantity);

        // 저장
        stockRepository.saveAndFlush(stock);
    }

//    @Transactional
    public synchronized void syncDecrease(Long id, Long quantity) {
        // synchronized -> 한 번에 하나의 스레드만 접근할 수 있도록 함

        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }

    // 부모의 transaction과 별개로 실행되어야해서 propagation 설정
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void namedLockDecrease(Long id, Long quantity) {
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }

}

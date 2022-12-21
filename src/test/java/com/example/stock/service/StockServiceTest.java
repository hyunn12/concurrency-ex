package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private PessimisticLockStockService pessimisticLockStockService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void before() {
        Stock stock = new Stock(1L, 100L);
        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    void after() {
        stockRepository.deleteAll();
    }

    @Test
    @DisplayName("재고감소")
    void decrease() {
        stockService.decrease(1L, 1L);

        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertEquals(99, stock.getQuantity());
    }

    @Test
    @DisplayName("동시에 100개의 요청이 들어올 경우")
    void multiDecrease() throws InterruptedException {
        int threadCount = 100;

        // 비동기로 실행하는 작업을 단순화해 사용할 수 있게 도와주는 API
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 다른 스레드에서 실행 중인 작업이 마무리될 때까지 기다릴 수 있도록 해줌
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decrease(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertEquals(0L, stock.getQuantity());

        // 예상 결과: 0 | 실제 결과: 37
        // 왜 예상한 결과와 다른 값이 나오게 되었는가?
        //     Race Condition 이 일어났기 때문
        //     둘 이상의 스레드가 공유 데이터에 접근할 수 있는 상황에서 동시에 접근해 데이터를 변경하려할 때 발생하는 문제
        //     둘 이상의 스레드가 동시에 변경을 일으키려해 원하는 대로 값이 바뀌지 않게 됨
        //     해결방법? 한 번에 하나의 스레드만 데이터에 접근할 수 있도록 함 => 여기서는 synchronized 이용
    }

    @Test
    @DisplayName("동시에 100개의 요청이 들어올 경우 - synchronized")
    void syncDecrease() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.syncDecrease(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        Stock stock = stockRepository.findById(1L).orElseThrow();
        assertEquals(0L, stock.getQuantity());
        // 예상 결과: 0 | 실제 결과: 37

        // 왜 예상한 결과와 다른 값이 나오게 되었는가?
        //      Spring의 @Transactional 동작 방식  때문
        //      @Transactional 을 사용하는 경우 해당 메소드의 시작과 끝에 startTransaction() ~ endTransaction() 메소드가 생성됨
        //      endTransaction() 이 끝나기 전에 다른 스레드가 기존 로직에 접근할 수 있게되어버림
        //      따라서 해당 문제 해결을 위해서는 @Transactional 을 없애야함
        //      @Transactional 주석 처리 시 테스트 정상적으로 통과 => 하지만 이게 실질적인 해결일까??

        // synchronized 를 이용할 경우 발생할 수 있는 문제
        //      인스턴스 단위로 thread-safe 보장이 되는데 서버가 여러 대인 경우엔 인스턴스가 여러 개인 것과 같아짐
        //      따라서 여러 대의 서버에서 동시에 접근한다면?? 서로 다른 프로세스를 가지기때문에 보장이 되지 않음 => Race Condition 발생
        //      실 운영 서버는 대부분 두 대 이상의 서버를 사용하기 때문에 synchronized 로 동시성 이슈를 해결할 수 없음!
    }

    @Test
    @DisplayName("동시에 100개의 요청이 들어올 경우 - Pessimistic Lock")
    void pessimisticLockDecrease() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pessimisticLockStockService.decrease(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        Stock stock = stockRepository.findById(1L).orElseThrow();
        assertEquals(0L, stock.getQuantity());

        // [ Pessimistic Lock ]
        //   select stock0_.id as id1_0_, stock0_.product_id as product_2_0_, stock0_.quantity as quantity3_0_ from stock stock0_ where stock0_.id=? for update
        //   마지막의 for update 부분이 락을 걸고 데이터를 가지고오는 부분
        // 장점
        //      충돌이 빈번하게 일어난다면 optimistic lock 보다 성능이 좋을 수 있음
        //      lock을 통해 update를 제어하기에 데이터 정합성이 어느정보 보장됨
        // 단점
        //      별도의 lock을 걸기때문에 성능저하가 발생할 수 있음
    }

}
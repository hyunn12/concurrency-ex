package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.facade.NamedLockStockFacade;
import com.example.stock.facade.OptimisticLockStockFacade;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class LockStockServiceTest {

    @Autowired
    private PessimisticLockStockService pessimisticLockStockService;

    @Autowired
    private OptimisticLockStockFacade optimisticLockStockFacade;

    @Autowired
    private NamedLockStockFacade namedLockStockFacade;

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

    @Test
    @DisplayName("동시에 100개의 요청이 들어올 경우 - Optimistic Lock")
    void optimisticLockDecrease() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    optimisticLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        Stock stock = stockRepository.findById(1L).orElseThrow();
        assertEquals(0L, stock.getQuantity());

        // [ Optimistic Lock ]
        // 장점
        //      별도의 lock을 걸지 않으므로 pessimistic lock 보다 성능상의 이점이 있음
        // 단점
        //      update 실패 시 재시도 로직을 직접 추가해야함 (여기서는 facade pattern 을 이용함)
        //      충돌이 빈번하게 일어난다면 성능저하가 일어날 수 있음
    }

    @Test
    @DisplayName("동시에 100개의 요청이 들어올 경우 - Named Lock")
    void namedLockDecrease() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    namedLockStockFacade.decrease(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        Stock stock = stockRepository.findById(1L).orElseThrow();
        assertEquals(0L, stock.getQuantity());

        // [ Named Lock ]
        // 위의 두 lock은 Stock에 lock을 걸었음
        // named lock은 다른 위치에 lock을 따로 설정함
        // 여기서는 임의로 테스트하는 것이기때문에 같은 데이터 소스를 사용했지만 실무에서는 다른 데이터 소스를 이용하는 것을 권장함
        // 같은 데이터 소스 사용 시 커넥션 풀이 부족해지는 현상이 일어나 다른 서비스에도 영향을 줄 수 있기때문에

        // named lock은 주로 분산락을 구현할 때 이용함
        // pessimistic lock 은 time out 구현이 어렵지만 named lock은 쉬움
        // data 삽입 시 정합성 맞춰야하는 경우에도 이용 가능
        // 단점
        //      transaction 종료 시 lock 해제와 세션관리에 주의해아함
        //      실제 사용 시 구현이 복잡할 수 있음
    }

}
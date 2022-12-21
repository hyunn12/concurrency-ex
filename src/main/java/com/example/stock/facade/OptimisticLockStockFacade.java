package com.example.stock.facade;

import com.example.stock.service.OptimisticLockStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OptimisticLockStockFacade {
    // Optimistic lock 의 경우에는 version이 다르면 update가 실패하도록 되어있음
    // 따라서 update가 실패할 경우 재시도를 할 수있도록 별도의 로직으로 따로 감싸주어야함
    // 감싸주기 위해서 파사드 패턴을 이용해 외부에 OptimisticLockStockService 의 decrease가 직접적으로 노축되지 않도록 함
    // 파사드패턴이 단순히 하나의 메서드만 감싸고있으므로 알아보기 용이하게 감싸주는 메서드와 동일한 이름을 사용함

    private final OptimisticLockStockService optimisticLockStockService;

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (true) {
            try {
                optimisticLockStockService.decrease(id, quantity);
                break;
            } catch (Exception e) {
                Thread.sleep(50);
            }
        }
    }

}

# [재고 시스템으로 알아보는 동시성 이슈 해결 방법](https://www.inflearn.com/course/%EB%8F%99%EC%8B%9C%EC%84%B1%EC%9D%B4%EC%8A%88-%EC%9E%AC%EA%B3%A0%EC%8B%9C%EC%8A%A4%ED%85%9C#)

## 목표
- 동시성 이슈란 무엇인가?
- 동시성 이슈를 어떻게 처리해야 하는가?


---

### MySQL을 이용한 방법

#### Pessimistic Lock (exclusive lock)

- 실제로 데이터에 lock을 걸어 정합성을 맞춤
- 다른 트랜잭션에선 lock이 해제되기 전까지 연결할 수 없음
- 실제 사용 시 서버에 `dead lock`이 걸릴 가능성이 높아 주의해서 사용해야함

#### Optimistic Lock

- 실제로 lock을 거는 것이 아닌 데이터에 `VERSION` 컬럼을 추가해 정합성을 맞춤
- 읽을 때 가져온 버전과 수정 시의 버전이 같을 경우에만 update가 가능함

#### Named Lock

- 이름을 가진 metadata locking
- 이름을 가진 lock을 획득한 후 해제할 때까지 다른 세선은 lock을 획득할 수 없음
- 트랜잭션이 종료될 때 자동으로 lock이 해제되지 않아 따로 명시하거나 선점시간이 끝나야 해지됨
- pessimistic lock과 비슷하지만 다름
  
  - pessimistic lock은 로우나 테이블 단위로 락을 걺
  - named lock은 metadata 에 락을 걺


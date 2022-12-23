# [재고 시스템으로 알아보는 동시성 이슈 해결 방법](https://www.inflearn.com/course/%EB%8F%99%EC%8B%9C%EC%84%B1%EC%9D%B4%EC%8A%88-%EC%9E%AC%EA%B3%A0%EC%8B%9C%EC%8A%A4%ED%85%9C#)

## 목표
- 동시성 이슈란 무엇인가?
- 동시성 이슈를 어떻게 처리해야 하는가?


---

## MySQL을 이용한 방법

### Pessimistic Lock (exclusive lock)

- 실제로 데이터에 lock을 걸어 정합성을 맞춤
- 다른 트랜잭션에선 lock이 해제되기 전까지 연결할 수 없음
- 실제 사용 시 서버에 `dead lock`이 걸릴 가능성이 높아 주의해서 사용해야함

### Optimistic Lock

- 실제로 lock을 거는 것이 아닌 데이터에 `VERSION` 컬럼을 추가해 정합성을 맞춤
- 읽을 때 가져온 버전과 수정 시의 버전이 같을 경우에만 update가 가능함

### Named Lock

- 이름을 가진 metadata locking
- 이름을 가진 lock을 획득한 후 해제할 때까지 다른 세선은 lock을 획득할 수 없음
- 트랜잭션이 종료될 때 자동으로 lock이 해제되지 않아 따로 명시하거나 선점시간이 끝나야 해지됨
- pessimistic lock과 비슷하지만 다름
  
  - pessimistic lock은 로우나 테이블 단위로 락을 걺
  - named lock은 metadata 에 락을 걺


---
## Library를 이용한 방법

### Lettuce
- setnx 명령어를 활용해 분산력 구현
  - set if not exist (setnx) -> key-value 를 set할 때 기존값이 없을 경우만 set함
- spin lock 방식이므로 retry 로직을 작성해야함
  - lock을 획득하려는 스레드가 lock을 사용할 수 있는지 반복적으로 확인하며 시도하는 방식

```
# setnx key value
# setnx 1 lock
> 1
// 처음에는 1이라는 key에 값이 존재하지 않으므로 lock 이라는 value 가 설정되어 결과값이 1이 출력됨

# setnx 1 lock
> 0
// 이미 1이라는 key에 값이 존재하므로 set되지않고 결과값이 0 이 출력됨

# del key
# del 1
> 1
// del 명령어를 이용해 1이라는 key값을 제거. 제거 후에는 다시 setnx 로 key 1에 값 설정 가능
```

- 작동 방식은 MySQL의 named lock과 비슷
  - 하지만 redis를 활용한다는 것과 세션관리에 신경쓰지 않아도 된다는 차이점이 존재

- spring-data-redis 이용 시 lettuce가 기본으로 따라오기때문에 별도의 라이브러리를 설치할 필요가 없고 구현이 쉬움

### Redisson
- pub-sub 기반으로 lock 구현
  - lock을 점유중인 스레드가 기다리는 스레드에게 채널을 통해 점유해제를 전달해 lock을 가져갈 수 있게 함
  - retry 로직을 만들 필요가 없음
- 자신이 점유하고 있는 lock을 해제할 때 채널에 메세지를 보내줌으로써 lock을 획득해야하는 스레드들에게 해제를 알려줌

```
pub-sub 기반
# subscribe [channel]
# publish [channel] [message]

// 1번 스레드
# subscribe ch1

// 2번 스레드
# publish ch1 hello, world!

// 1번 스레드
> hello, world!
```

- lock 획득 재시도를 기본으로 제공
- lettuce 는 lock을 획득할 때까지 계속해서 시도를 하지만 redisson은 점유가 끝났다고 할 때만 몇번정도 시도하기때문에 상대적으로 부하가 덜함
- 단, 별도의 라이브러리를 이용해야하고 구현이 좀 더 복잡해짐
  - lock 을 라이브러리 차원에서 제공하기때문에 별도의 학습이 요구됨

### 실무에서 Lettuce vs Redisson
- 재시도가 필요하지 않은 lock: Lettuce
- 재시도가 필요한 lock: Redisson

---

## MySQL vs Redis

### MySQL
- 이미 MySQL 사용 시 별도의 비용 지불 없이 사용 가능
- 어느정도 트래픽은 감당 가능
- Redis보다 성능이 좋지 않음

### Redis
- 활용 중이 아니라면 별도의 비용 발생
- MySQL 보다 성능이 좋음

### 실무에서 MySQL vs Redis
- 비용적 여유가 없거나 MySQL로 처리가 가능한 정도라면 MySQL을 이용하는 등 상황에 맞게 선택




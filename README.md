# 선착순 예약 시스템 — Warm Pool vs 대기열 비교 실험

## 프로젝트 개요

AWS Auto Scaling scale-out 병목 문제를 해결하는 두 가지 방식을 비교합니다.

| 방식 | 설명 |
|---|---|
| **Warm Pool** | AWS ASG Warm Pool로 EC2를 미리 워밍 → scale-out 즉시 투입 |
| **대기열** | Queue①(EC2 진입 제어) + Queue②(DB 접근 제어)로 트래픽 버퍼링 |

---

## 아키텍처

```
[대기열 방식]
사용자 → Queue① (scale-out 중 EC2 진입 제어)
       → Queue② (Redis ZSET, DB 접근 제어)
       → WaitingScheduler (5명/초 처리)
       → SQS → SqsConsumer
       → MySQL (좌석 자동 배정)

[Warm Pool 방식]
사용자 → POST /api/reservations
       → MySQL (비관적 락, 직접 예약)
```

---

## 환경 변수

### transaction 패키지 (DB)

| 환경 변수 | 설명 | 필수 |
|---|---|---|
| `SPRING_DATASOURCE_URL` | MySQL RDS 접속 URL | ✅ |
| `SPRING_DATASOURCE_USERNAME` | DB 계정 | ✅ |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | ✅ |

### waiting_redis 패키지 (Redis / SQS)

| 환경 변수 | 설명 | 기본값 | 필수 |
|---|---|---|---|
| `SPRING_DATA_REDIS_HOST` | ElastiCache 엔드포인트 | `localhost` | ✅ |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | `6379` | - |
| `AWS_REGION` | AWS 리전 | `ap-southeast-2` | - |
| `SQS_QUEUE_URL` | 예약 처리 큐 URL | 없음 | ✅ |
| `SQS_LIFECYCLE_HOOK_QUEUE_URL` | Lifecycle Hook 큐 URL | 없음 | ✅ |
| `QUEUE1_TTL_MINUTES` | Queue① 유지 시간(분) | `3` | - |
| `QUEUE1_DRAIN_SIZE` | Queue①→② 초당 처리 수 | `10` | - |

---

## AWS 배포

### 사전 준비

- EC2 IAM Role에 SQS 권한 부여 (`sqs:SendMessage`, `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes`)
- AWS SQS 큐 생성: `waiting-process-queue`, `waiting-process-dlq`, `lifecycle-hook-queue`
- ASG Lifecycle Hook → SNS → SQS(`lifecycle-hook-queue`) 연결

### Docker 실행

```bash
docker build -t reservation-app .

docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://{rds-endpoint}:3306/ticketing?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8 \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD={password} \
  -e SPRING_DATA_REDIS_HOST={elasticache-endpoint} \
  -e SQS_QUEUE_URL=https://sqs.ap-southeast-2.amazonaws.com/{accountId}/waiting-process-queue \
  -e SQS_LIFECYCLE_HOOK_QUEUE_URL=https://sqs.ap-southeast-2.amazonaws.com/{accountId}/lifecycle-hook-queue \
  reservation-app
```

---

## 로컬 테스트

### 환경 구성

`docker-compose.yml`이 자동으로 아래 환경을 구성합니다.

| 항목 | 로컬 값 | 설정 위치 |
|---|---|---|
| Spring Profile | `local` | docker-compose `SPRING_PROFILES_ACTIVE=local` |
| MySQL | 로컬 컨테이너 (`mysql:3306`) | docker-compose 환경변수 |
| Redis | 로컬 컨테이너 (`redis:6379`) | docker-compose 환경변수 |
| SQS | LocalStack (`http://localstack:4566`) | application-local.yml |
| AWS 인증 | fake (`test` / `test`) | application-local.yml |
| SQS Queue URL | `http://localstack:4566/000000000000/...` | application-local.yml |

```bash
docker-compose up --build
```

### API 테스트 순서

```bash
# 1. 대기열 진입
POST http://localhost:8080/api/waitings?userId=user1

# 2. 상태 폴링 (입장권 발급 대기)
GET  http://localhost:8080/api/waitings/status?userId=user1

# 3. Form 제출 (입장권 확인 후)
POST http://localhost:8080/api/waitings/confirm?userId=user1

# Queue① 활성화 시뮬레이션 (scale-out 흉내)
aws --endpoint-url=http://localhost:4566 sqs send-message \
  --queue-url http://localhost:4566/000000000000/lifecycle-hook-queue \
  --message-body '{"LifecycleTransition":"autoscaling:EC2_INSTANCE_LAUNCHING"}' \
  --region us-east-1
```

---

## AWS 배포

### 환경 구성

`SPRING_PROFILES_ACTIVE` 미설정 시 기본 `application.yml`만 로드됩니다.

| 항목 | AWS 값 | 설정 방법 |
|---|---|---|
| Spring Profile | 없음 (기본) | 설정 불필요 |
| MySQL | RDS 엔드포인트 | `SPRING_DATASOURCE_URL` 환경변수 |
| Redis | ElastiCache 엔드포인트 | `SPRING_DATA_REDIS_HOST` 환경변수 |
| SQS | 실제 AWS SQS | `SQS_QUEUE_URL` 환경변수 |
| AWS 인증 | EC2 IAM Role 자동 적용 | 환경변수 불필요 (SqsConfig 비활성) |

```bash
docker build -t reservation-app .

docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://{rds-endpoint}:3306/ticketing?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8 \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD={password} \
  -e SPRING_DATA_REDIS_HOST={elasticache-endpoint} \
  -e SQS_QUEUE_URL=https://sqs.ap-southeast-2.amazonaws.com/{accountId}/waiting-process-queue \
  -e SQS_LIFECYCLE_HOOK_QUEUE_URL=https://sqs.ap-southeast-2.amazonaws.com/{accountId}/lifecycle-hook-queue \
  reservation-app
```

### 사전 준비

- EC2 IAM Role에 SQS 권한 부여 (`sqs:SendMessage`, `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes`)
- AWS SQS 큐 생성: `waiting-process-queue`, `waiting-process-dlq`, `lifecycle-hook-queue`
- ASG Lifecycle Hook → SNS → SQS(`lifecycle-hook-queue`) 연결

---

## Redis Keys

| 키 | 타입 | 설명 |
|---|---|---|
| `queue1` | ZSET | Queue① 대기자 (서버 진입 제어) |
| `queue1:active` | String | Queue① 활성 플래그 (TTL 3분) |
| `waiting-queue` | ZSET | Queue② 대기자 (DB 접근 제어) |
| `active_user:{userId}` | String | 입장권 (TTL 5분) |

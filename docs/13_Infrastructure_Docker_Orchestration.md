# SSO Platform - Infrastructure & Docker Orchestration

Tài liệu này đặc tả cấu hình Docker Compose để chạy toàn bộ hệ thống **SSO Platform** ở local và staging.

---

## 1. Cấu Trúc Infrastructure Files

```
infrastructure/
├── docker-compose.infra.yml     # Chỉ infrastructure: DB, Redis, Kafka, Monitoring
├── docker-compose.apps.yml      # Tất cả application services
├── docker-compose.full.yml      # Full stack (include infra)
├── postgres/
│   └── init.sql                 # Tạo databases khi khởi động lần đầu
├── prometheus/
│   └── prometheus.yml           # Scrape targets config
└── grafana/
    └── dashboards/              # Dashboard JSON definitions
```

---

## 2. docker-compose.infra.yml

```yaml
version: '3.9'

services:
  # ==================== Databases ====================
  postgres:
    image: postgres:16-alpine
    container_name: sso-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: sso_db        # Default DB; init.sql tạo thêm các DB khác
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./postgres/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ==================== Cache ====================
  redis:
    image: redis:7-alpine
    container_name: sso-redis
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  # ==================== Kafka ====================
  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: sso-kafka
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_NUM_PARTITIONS: 3
      CLUSTER_ID: "sso-platform-kafka-cluster"
    ports:
      - "9092:9092"
    healthcheck:
      test: ["CMD", "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"]
      interval: 30s
      timeout: 10s
      retries: 5

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: sso-kafka-ui
    depends_on: [kafka]
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
    ports:
      - "8081:8080"

  # ==================== Object Storage ====================
  minio:
    image: minio/minio:RELEASE.2024-02-09T22-07-22Z
    container_name: sso-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ==================== Monitoring & Logging ====================
  prometheus:
    image: prom/prometheus:v2.50.0
    container_name: sso-prometheus
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=7d'
    ports:
      - "9090:9090"

  loki:
    image: grafana/loki:2.9.4
    container_name: sso-loki
    ports:
      - "3100:3100"
    command: -config.file=/etc/loki/local-config.yaml

  promtail:
    image: grafana/promtail:2.9.4
    container_name: sso-promtail
    volumes:
      - /var/log:/var/log
      - ./promtail/promtail-config.yaml:/etc/promtail/config.yml
    command: -config.file=/etc/promtail/config.yml

  grafana:
    image: grafana/grafana:10.3.0
    container_name: sso-grafana
    depends_on: [prometheus, loki]
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: admin
      GF_AUTH_ANONYMOUS_ENABLED: "false"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards:ro
    ports:
      - "3001:3000"

volumes:
  postgres_data:
  redis_data:
  minio_data:
  grafana_data:

networks:
  default:
    name: sso-network
```

---

## 3. postgres/init.sql — Tạo Databases

```sql
-- Chạy tự động lần đầu khởi động PostgreSQL container
-- Tạo tất cả databases cần thiết cho dự án

CREATE DATABASE sso_db;
CREATE DATABASE monolith_db;
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE notification_db;
CREATE DATABASE file_db;

-- Grant quyền (tất cả dùng chung postgres user ở local dev)
GRANT ALL PRIVILEGES ON DATABASE sso_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE monolith_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE user_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE product_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE order_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE file_db TO postgres;
```

---

## 4. Dockerfile Chuẩn (Multi-stage Build)

```dockerfile
# Dockerfile cho mỗi Java service
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Nếu multi-module, copy parent pom và common-contracts trước
# COPY ../common-contracts ./common-contracts

RUN ./mvnw clean package -DskipTests --no-transfer-progress

# Stage 2: Runtime (nhẹ hơn JDK 3x)
FROM eclipse-temurin:21-jre-alpine AS runtime

# Không chạy với root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# JVM tuning cho containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseZGC \
               -Dspring.profiles.active=prod"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 5. prometheus/prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'sso-server'
    static_configs:
      - targets: ['sso-server:9000']
    metrics_path: '/actuator/prometheus'

  - job_name: 'monolith-app'
    static_configs:
      - targets: ['monolith-app:8080']
    metrics_path: '/actuator/prometheus'

  - job_name: 'api-gateway'
    static_configs:
      - targets: ['api-gateway:8090']
    metrics_path: '/actuator/prometheus'

  - job_name: 'user-service'
    static_configs:
      - targets: ['user-service:8091']
    metrics_path: '/actuator/prometheus'

  - job_name: 'product-service'
    static_configs:
      - targets: ['product-service:8092']
    metrics_path: '/actuator/prometheus'

  - job_name: 'order-service'
    static_configs:
      - targets: ['order-service:8093']
    metrics_path: '/actuator/prometheus'

  - job_name: 'payment-service'
    static_configs:
      - targets: ['payment-service:8094']
    metrics_path: '/actuator/prometheus'

  - job_name: 'file-service'
    static_configs:
      - targets: ['file-service:8096']
    metrics_path: '/actuator/prometheus'

  - job_name: 'config-server'
    static_configs:
      - targets: ['config-server:8888']
    metrics_path: '/actuator/prometheus'
```

---

## 6. Application Health Check Configuration

```yaml
# Trong application.yml của mỗi service
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized   # Chi tiết health chỉ cho authenticated
  metrics:
    tags:
      service: ${spring.application.name}  # Tag service name cho Prometheus
```

---

## 7. Docker Networks (Trust Boundary)

```yaml
# docker-compose.full.yml — Tách mạng để enforce security boundary
networks:
  external-net:
    # Internet-facing network — chỉ Gateway được expose ra đây
  internal-net:
    # Internal service-to-service communication — không expose ra ngoài

services:
  api-gateway:
    networks:
      - external-net  # Nhận request từ internet
      - internal-net  # Forward đến internal services

  order-service:
    networks:
      - internal-net  # Chỉ communicate qua internal network
    # Không expose port ra host machine (không cần ports: trong production)
```

---

## 8. Lệnh Docker Thường Dùng

```bash
# Khởi động chỉ infrastructure
docker compose -f infrastructure/docker-compose.infra.yml up -d

# Xem log realtime của một service
docker compose logs -f order-service

# Rebuild một service cụ thể
docker compose up --build order-service

# Scale một service (nếu stateless)
docker compose up --scale product-service=3

# Kiểm tra tất cả services healthy
docker compose ps

# Xóa tất cả containers + volumes (reset hoàn toàn)
docker compose down -v

# Exec vào container để debug
docker exec -it sso-postgres psql -U postgres -d sso_db
docker exec -it sso-redis redis-cli
```

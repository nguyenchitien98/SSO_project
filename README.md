# SSO Platform - README

<div align="center">

## 🔐 SSO Platform
### Production-Grade Single Sign-On System

*Học kiến trúc bảo mật qua 2 cách tiếp cận: Monolith và Microservice*

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green?logo=spring)](https://spring.io/projects/spring-boot)
[![Spring Authorization Server](https://img.shields.io/badge/Spring%20Auth%20Server-1.3-green)](https://spring.io/projects/spring-authorization-server)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.x-black?logo=apache-kafka)](https://kafka.apache.org/)
[![MinIO](https://img.shields.io/badge/MinIO-Object--Storage-red?logo=amazon-s3)](https://min.io/)
[![GitLab CI](https://img.shields.io/badge/GitLab%20CI-CD-blue?logo=gitlab)](https://about.gitlab.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-K8s-blue?logo=kubernetes)](https://kubernetes.io/)

</div>

---

## 🎯 Mục Tiêu Dự Án

Dự án này xây dựng một **nền tảng SSO hoàn chỉnh** để minh họa sự khác biệt rõ ràng giữa:

| Tiêu chí | Monolith App | Microservice App |
|---|---|---|
| **Authorization** | `@PreAuthorize("hasAuthority('PRODUCT_CREATE')")` | `authorizationService.requirePermission(user, "PRODUCT_CREATE")` |
| **Identity Source** | Spring Security SecurityContext (từ JWT trực tiếp) | Gateway-injected trusted headers (X-User-*) |
| **JWT Validation** | Mỗi request validate tại filter chain | Chỉ tại API Gateway (JWKS cache) |
| **Session** | Spring Session + Redis | Stateless |
| **DB** | Một database duy nhất | Mỗi service một DB riêng |
| **Service Comm.** | Method calls trực tiếp | REST + Kafka |

---

## 🏗️ Kiến Trúc Hệ Thống

```
                    ┌─────────────────────────┐
                    │        Browser          │
                    └────────────┬────────────┘
                                 │
               ┌─────────────────┴─────────────────┐
               │                                   │
               ▼                                   ▼
      ┌─────────────────┐                ┌──────────────────┐
      │  Monolith App   │                │ Microservice App  │
      │  :8080          │                │ (REST Clients)   │
      │                 │                └────────┬─────────┘
      │ @PreAuthorize   │                         │
      │ Spring Security │                         ▼
      │ SecurityContext │                ┌─────────────────┐
      └────────┬────────┘                │   API Gateway   │
               │                        │   :8090          │
               │                        │ JWT Validation   │
               │                        │ Strip Headers    │
               │                        │ Inject X-User-*  │
               │                        └────────┬────────┘
               │                                 │
               │                    ┌────────────┼────────────┐
               │                    │            │            │
               │                ┌───▼───┐  ┌────▼───┐  ┌────▼────┐
               │                │ User  │  │Product │  │ Order   │
               │                │:8091  │  │:8092   │  │:8093    │
               │                └───────┘  └────────┘  └────┬────┘
               │                                             │
               │                                        ┌────▼────┐
               │                                        │Payment  │
               │                                        │:8094    │
               │                                        └────┬────┘
               │                                             │ Kafka
               │                                        ┌────▼────┐
               │                                        │Notif.   │
               │                                        │:8095    │
               └──────────────────┐                     └─────────┘
                                  │ OAuth2/OIDC
                                  ▼
                    ┌─────────────────────────┐
                    │      SSO Server         │
                    │      :9000              │
                    │                         │
                    │  Spring Auth Server     │
                    │  OAuth2 + OIDC          │
                    │  RSA Key Management     │
                    │  RBAC + Permissions     │
                    │  Refresh Token Rotation │
                    │  Brute-Force Protection │
                    │  Audit Logging          │
                    └────────────┬────────────┘
                                 │
                    ┌────────────┼────────────┐
                    ▼            ▼            ▼
                 Postgres      Redis        Kafka
```

---

## 🔒 Security Features

- ✅ **OAuth2 Authorization Code + PKCE** (chuẩn RFC 6749)
- ✅ **OpenID Connect** (OIDC Core 1.0)
- ✅ **JWT với RSA asymmetric key** (RS256, JWKS endpoint)
- ✅ **Key Rotation** (không downtime)
- ✅ **Refresh Token Rotation** + **Replay Attack Detection**
- ✅ **RBAC** (6 roles) + **ABAC** (Resource Ownership)
- ✅ **SSO Cross-App Session** (login once, access both apps)
- ✅ **Back-Channel Logout**
- ✅ **Brute-Force Protection** (Redis counter + account lock)
- ✅ **Header Spoofing Prevention** (Gateway strips X-User-* from clients)
- ✅ **Service-to-Service Authentication** (OAuth2 Client Credentials)
- ✅ **Security Audit Log** đầy đủ

---

## 📦 Tech Stack

### Backend
| Layer | Technology |
|---|---|
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.3 |
| SSO | Spring Authorization Server 1.3 (Keycloak as architecture reference) |
| Security | Spring Security 6, 2FA/TOTP |
| Config | Spring Cloud Config Server |
| Gateway | Spring Cloud Gateway |
| Discovery | Netflix Eureka |
| File Upload | file-service |
| Resilience | Resilience4j (Circuit Breaker, Retry) |
| ORM | Spring Data JPA + Hibernate |
| Migration | Flyway |
| Messaging | Apache Kafka |
| Cache | Redis (Lettuce client) |
| Build | Maven |
| Code Quality | Spotless (Google Java Style) |

### Infrastructure
| Service | Purpose |
|---|---|
| PostgreSQL 16 | Primary databases |
| Redis 7 | Session, cache, rate limiting, OTP cache |
| Apache Kafka 3 | Async event bus |
| MinIO | Object Storage (avatars & images) |
| Prometheus | Metrics collection |
| Grafana | Dashboards |
| Jaeger | Distributed tracing |
| Loki / Promtail | Log aggregation |
| GitLab CI / K8s | Pipeline and orchestrator |

---

## 🚀 Quick Start

```bash
# 1. Khởi động infrastructure
docker compose -f infrastructure/docker-compose.infra.yml up -d

# 2. Chạy SSO Server
cd sso-server && ./mvnw spring-boot:run

# 3. Chạy Monolith
cd monolith-app && ./mvnw spring-boot:run

# 4. Chạy Microservices
cd microservice-app/api-gateway && ./mvnw spring-boot:run
# ... (xem docs/06_Run_Guide.md cho hướng dẫn đầy đủ)
```

Xem [docs/06_Run_Guide.md](docs/06_Run_Guide.md) để có hướng dẫn chi tiết.

---

## 📚 Tài Liệu

| File | Nội dung |
|---|---|
| [docs/00_Project_Vision.md](docs/00_Project_Vision.md) | Tầm nhìn và mục tiêu dự án |
| [docs/01_Architecture_Bible.md](docs/01_Architecture_Bible.md) | Kiến trúc hệ thống và security model |
| [docs/02_Coding_Guideline.md](docs/02_Coding_Guideline.md) | Tiêu chuẩn code và Javadoc |
| [docs/03_AI_Coding_Guide.md](docs/03_AI_Coding_Guide.md) | Prompt templates cho AI agent |
| [docs/04_Database_Schema.md](docs/04_Database_Schema.md) | Database schema đầy đủ |
| [docs/05_Sprint_Plan.md](docs/05_Sprint_Plan.md) | Lộ trình 25 Sprint |
| [docs/06_Run_Guide.md](docs/06_Run_Guide.md) | Hướng dẫn chạy project |
| [docs/07_Security_Model.md](docs/07_Security_Model.md) | Deep dive security decisions |
| [docs/15_Sequence_Diagrams.md](docs/15_Sequence_Diagrams.md) | Sơ đồ tuần tự các luồng nghiệp vụ |
| [docs/16_ADR.md](docs/16_ADR.md) | Nhật ký quyết định thiết kế kiến trúc |
| [docs/17_2FA_TOTP_Guide.md](docs/17_2FA_TOTP_Guide.md) | Hướng dẫn triển khai xác thực 2FA/TOTP |
| [docs/18_MinIO_File_Storage.md](docs/18_MinIO_File_Storage.md) | Hướng dẫn tích hợp MinIO Object Storage |
| [docs/19_CI_CD_Pipeline.md](docs/19_CI_CD_Pipeline.md) | Đặc tả cấu hình GitLab CI/CD |
| [prompt.md](prompt.md) | Prompt khởi đầu cho AI agent |
| [clauderules.md](clauderules.md) | Quy tắc cho AI coding (Claude/Gemini) |

---

## 🎓 Giá Trị Học Thuật

Khi hoàn thành dự án, bạn có thể trả lời được các câu hỏi phỏng vấn:

- "Giải thích OAuth2 Authorization Code Flow + PKCE"
- "Tại sao Gateway validate JWT thay vì gọi Auth Service?"
- "Sự khác nhau giữa @PreAuthorize trong Monolith và AuthorizationService trong Microservice?"
- "Refresh Token Rotation hoạt động thế nào? Làm sao detect replay attack?"
- "Tại sao dùng RSA key thay vì shared secret cho JWT?"
- "X-User-* headers đến từ đâu? Tại sao phải strip từ client?"
- "Service-to-service authentication khác gì user authentication?"
- "Outbox Pattern giải quyết vấn đề gì?"
- "Circuit Breaker hoạt động thế nào?"

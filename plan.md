Bạn giúp mình lên plan cho dự án này sẽ làm về hệ thống SSO, có app dùng kiến trúc monothilich, app dùng microservice, chỉ cần tạo 2 app riêng biệt thôi, cũng sẽ có mọi thứ như phân quyền trong monothilich, phân quyền cho microservice, mỗi loại sẽ dùng khác nhau, ví dụ nếu mono thì dùng @preauthorize, còn microservice thì chỉ giải mã JWT ở auth service rồi expore x-user-id, x-user-role ..., nói chung là càng chi tiết càng tốt, sao cho khi hoàn thành thì mọi thứ đề hoạt động trơn chu,
Worked for 8s

Được. Với mục tiêu của bạn, mình khuyên không làm một app rồi “bật/tắt microservice”, mà xây thành một hệ thống SSO hoàn chỉnh gồm 2 application độc lập:

Monolithic App — toàn bộ business nằm trong một Spring Boot application.
Microservice App — cùng domain/business tương đương nhưng tách thành nhiều service.
Cả hai dùng chung một SSO/Identity Provider.
Cố tình triển khai hai cơ chế authorization khác nhau để bạn có thể hiểu rõ sự khác biệt giữa Monolith và Distributed System.

Đây sẽ là một project rất tốt để showcase các kiến thức Spring Security, OAuth2/OIDC, JWT, RBAC, ABAC, SSO, API Gateway, service-to-service authentication, distributed tracing, Redis, Kafka, audit log...

Điểm quan trọng: ý tưởng của bạn về x-user-id, x-user-role là đúng hướng về mặt kiến trúc, nhưng không nên để Auth Service đơn thuần decode JWT rồi các service tin các header do client gửi lên. Mình sẽ thiết kế lại phần này cho an toàn.

1. Mục tiêu cuối cùng

Ta xây một hệ thống như sau:

                         ┌─────────────────────┐
                         │       Browser       │
                         │                     │
                         │ User                 │
                         └──────────┬──────────┘
                                    │
                         Login      │
                                    ▼
                     ┌─────────────────────────┐
                     │       SSO Server        │
                     │                         │
                     │ OAuth2 / OIDC           │
                     │ User Authentication     │
                     │ Token Management        │
                     │ RBAC                    │
                     └────────────┬────────────┘
                                  │
                 ┌────────────────┴────────────────┐
                 │                                 │
                 ▼                                 ▼
      ┌────────────────────┐            ┌────────────────────┐
      │  Monolith App      │            │ Microservice App   │
      │                    │            │                    │
      │ Spring Boot        │            │ API Gateway        │
      │ Spring Security    │            │       │            │
      │                    │            │       ├── User      │
      │ @PreAuthorize      │            │       ├── Order     │
      │                    │            │       ├── Product   │
      └─────────┬──────────┘            │       └── Payment   │
                │                       └────────────────────┘
                ▼
             Database

Và cả hai app đều sử dụng:

SSO
│
├── Login
├── Logout
├── Access Token
├── Refresh Token
├── User
├── Role
└── Permission

Nhưng cách authorization bên trong hai app sẽ khác nhau.

2. Kiến trúc tổng thể mình đề xuất

Mình đề xuất chia repository như này:

sso-platform/
│
├── sso-server/
│
├── monolith-app/
│
├── microservice-app/
│ │
│ ├── api-gateway/
│ ├── user-service/
│ ├── product-service/
│ ├── order-service/
│ ├── payment-service/
│ └── notification-service/
│
├── shared-contracts/
│
├── infrastructure/
│ ├── docker-compose.yml
│ ├── postgres/
│ ├── redis/
│ ├── kafka/
│ └── observability/
│
└── docs/
├── architecture/
├── security/
├── api/
└── deployment/

Tuy nhiên shared-contracts không được biến thành một "common library khổng lồ".

Đây cũng liên quan trực tiếp đến câu hỏi trước của bạn về microservice và common library.

Nên:

shared-contracts
│
├── event contracts
└── DTO contracts

Không nên:

shared-library
│
├── entity
├── repository
├── service
├── security
├── business logic
└── utility

Nếu không, microservice rất dễ biến thành distributed monolith.

3. SSO Server

Đây là trung tâm của toàn bộ hệ thống.

Có thể gọi:

Identity Service

hoặc:

Authorization Server

Nhiệm vụ:

Authentication
Authorization
Token issuance
Token refresh
SSO
User management
Role management
Permission management
Client management
Session management
Audit 4. Chuẩn giao thức

Đừng tự chế authentication protocol.

Nên dùng:

OAuth 2.0

- OpenID Connect

Trong đó:

OAuth2
↓
Authorization

OIDC
↓
Authentication / Identity

SSO server sẽ đóng vai trò:

Authorization Server

- OpenID Provider

5. Login flow

Ví dụ user truy cập:

https://monolith.example.com

Chưa login.

Monolith redirect:

https://sso.example.com/oauth2/authorize

Ví dụ:

client_id=monolith-web
response_type=code
redirect_uri=https://monolith.example.com/login/oauth2/code/sso
scope=openid profile email
state=...
code_challenge=...

SSO login.

Sau đó:

SSO
↓
Authorization Code
↓
Monolith
↓
Token Endpoint
↓
Access Token
Refresh Token
ID Token 6. SSO giữa hai app

Đây là phần rất đáng làm.

User login:

Monolith
↓
SSO
↓
login

Sau đó user mở:

Microservice App

Microservice redirect tới SSO:

Microservice
↓
SSO

SSO phát hiện:

User already authenticated

=> không yêu cầu login lại.

Microservice
↓
SSO
↓
Existing SSO Session
↓
Authorization Code
↓
Microservice

Đây chính là SSO thực sự.

7. Hai client khác nhau

SSO nên có:

## Client 1

client_id = monolith-web

## Client 2

client_id = microservice-web

Không dùng chung client ID.

Ví dụ:

SSO
│
├── monolith-web
│ ├── redirect URI
│ └── scopes
│
└── microservice-web
├── redirect URI
└── scopes 8. User model

Database của SSO:

users

Ví dụ:

id
username
email
password_hash
first_name
last_name
enabled
locked
created_at
updated_at

Role:

roles

Ví dụ:

ADMIN
MANAGER
USER
AUDITOR
SUPPORT

Permission:

permissions

Ví dụ:

USER_READ
USER_CREATE
USER_UPDATE
USER_DELETE

PRODUCT_READ
PRODUCT_CREATE
PRODUCT_UPDATE
PRODUCT_DELETE

ORDER_READ
ORDER_CREATE
ORDER_CANCEL
ORDER_REFUND

Relationship:

users
│
└── user_roles
│
▼
roles
│
└── role_permissions
│
▼
permissions 9. RBAC

Ta sẽ hỗ trợ:

User
↓
Role
↓
Permission

Ví dụ:

admin
├── USER_READ
├── USER_CREATE
├── USER_UPDATE
├── USER_DELETE
├── ORDER_READ
└── ORDER_REFUND

Manager:

manager
├── USER_READ
├── ORDER_READ
├── ORDER_CREATE
└── ORDER_CANCEL

User:

user
├── PRODUCT_READ
└── ORDER_CREATE 10. JWT

Access token có thể chứa:

{
"iss": "https://sso.example.com",
"sub": "8c1...",
"aud": [
"monolith-api"
],
"exp": 1786250000,
"iat": 1786246400,
"scope": [
"openid",
"profile",
"email"
],
"roles": [
"ADMIN"
],
"permissions": [
"USER_READ",
"USER_DELETE"
]
}

Nhưng cần quyết định cẩn thận những claim nào đưa vào JWT.

Không nhét toàn bộ dữ liệu user vào JWT.

11. Monolith authorization

Đây chính là phần bạn muốn:

@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) {
}

Hoặc:

@PreAuthorize("hasAuthority('USER_DELETE')")
public void deleteUser(Long id) {
}

Ví dụ:

@PreAuthorize("hasAuthority('ORDER_CANCEL')")
public void cancelOrder(Long orderId) {
} 12. Method Security

Monolith:

@EnableMethodSecurity

Sau đó:

@Service
public class UserService {

    @PreAuthorize("hasAuthority('USER_READ')")
    public UserDto getUser(Long id) {
        ...
    }

    @PreAuthorize("hasAuthority('USER_DELETE')")
    public void deleteUser(Long id) {
        ...
    }

}

Đây là một điểm rất hay để showcase.

13. Không chỉ authorization ở Controller

Không nên:

@RestController
class UserController {

    @PreAuthorize(...)
    public ...

}

rồi service bên dưới không bảo vệ gì.

Tốt hơn:

Controller
↓
Service
↓
Repository

Authorization quan trọng nằm ở:

Service

Ví dụ:

@PreAuthorize("hasAuthority('USER_DELETE')")
public void deleteUser(...) {
} 14. Ownership authorization

Không chỉ RBAC.

Ví dụ:

USER

chỉ được xem order của chính mình.

Không thể:

GET /orders/123

nếu order 123 thuộc user khác.

Có thể:

@PreAuthorize("@orderSecurity.isOwner(authentication, #orderId)")

Đây là:

RBAC + ABAC / resource ownership

Rất đáng đưa vào project.

15. Microservice authorization

Đây là phần cần thiết kế kỹ.

Mình không khuyến nghị flow:

Request
↓
Auth Service
↓
decode JWT
↓
x-user-id
x-user-role
↓
Microservice

nếu "Auth Service" phải được gọi trên mọi request.

Vì như vậy:

1000 requests
↓
1000 requests đến Auth Service

Auth Service trở thành bottleneck.

16. Kiến trúc đúng hơn

Dùng:

Client
↓
API Gateway
↓
Microservice

Gateway validate JWT.

JWT
↓
Signature verification
↓
Expiration
↓
Issuer
↓
Audience
↓
Claims

Sau khi verified:

user-id
user-role
permissions
tenant-id

mới được truyền xuống.

17. Header propagation

Ví dụ:

X-User-Id: 123
X-User-Roles: ADMIN
X-User-Permissions: USER_READ,USER_DELETE

Nhưng cực kỳ quan trọng:

Client không được phép tự gửi các header này.

Ví dụ attacker gửi:

X-User-Id: 999
X-User-Roles: ADMIN

Gateway phải:

1. Remove X-User-Id
2. Remove X-User-Roles
3. Validate JWT
4. Generate trusted headers
5. Forward request

Tức:

Internet
↓
API Gateway
↓
TRUST BOUNDARY
↓
Internal Network
↓
Services 18. Microservice Service Security

Ví dụ:

Gateway
↓
Order Service

Order Service nhận:

X-User-Id
X-User-Roles
X-User-Permissions

Có thể tạo:

CurrentUser

Ví dụ:

public record CurrentUser(
Long userId,
Set<String> roles,
Set<String> permissions
) {}

Sau đó:

CurrentUser currentUser = currentUserProvider.get(); 19. Authorization ở microservice

Không dùng:

@PreAuthorize(...)

làm cơ chế chính như monolith.

Ta tạo:

AuthorizationService

Ví dụ:

authorizationService.requirePermission(
currentUser,
"ORDER_CANCEL"
);

Hoặc:

authorizationService.requireRole(
currentUser,
"ADMIN"
);

Ví dụ:

public void cancelOrder(Long orderId) {

    CurrentUser user = currentUserProvider.get();

    authorizationService.requirePermission(
        user,
        "ORDER_CANCEL"
    );

    ...

} 20. Nhưng microservice vẫn nên validate JWT

Có một điểm mình muốn chỉnh lại ý tưởng ban đầu của bạn.

Không nên hiểu:

Auth Service decode JWT

là:

Order Service hoàn toàn tin Gateway

Tốt hơn có hai tầng:

Tầng 1

Gateway:

JWT validation
Tầng 2

Service:

JWT validation hoặc trusted internal identity

Có thể chọn tùy mức độ project.

Với project portfolio, mình khuyên triển khai cả hai mode:

Mode A
Gateway validates JWT
Service trusts gateway headers

Mode B
Gateway validates JWT
Service independently validates JWT

Sau đó document trade-off.

21. Vì sao đây là một điểm rất hay để showcase?

Bạn có thể giải thích:

Monolith
Authentication
↓
Spring Security
↓
SecurityContext
↓
@PreAuthorize
↓
Service
Microservice
Authentication
↓
SSO
↓
JWT
↓
API Gateway
↓
JWT validation
↓
Trusted identity propagation
↓
Service-level authorization

Đây là hai mô hình rất khác nhau.

22. Microservice architecture

Mình đề xuất khoảng:

                    API Gateway
                         │
          ┌──────────────┼───────────────┐
          │              │               │
          ▼              ▼               ▼
     User Service   Product Service   Order Service
                                          │
                                          ▼
                                    Payment Service

Thêm:

Notification Service

nhưng không cần quá nhiều service.

Khoảng 5–6 service là đẹp.

23. User Service

Chịu trách nhiệm:

User profile
User information
User preferences

Không chịu trách nhiệm:

Authentication

Authentication thuộc:

SSO Server 24. Product Service
GET /products
GET /products/{id}

POST /products
PUT /products/{id}
DELETE /products/{id}

Permission:

PRODUCT_READ
PRODUCT_CREATE
PRODUCT_UPDATE
PRODUCT_DELETE 25. Order Service
POST /orders
GET /orders/{id}
GET /orders
POST /orders/{id}/cancel

Permission:

ORDER_READ
ORDER_CREATE
ORDER_CANCEL

Ownership:

USER
↓
only own order

Admin:

ADMIN
↓
all orders 26. Payment Service
POST /payments
GET /payments/{id}
POST /payments/{id}/refund

Permission:

PAYMENT_READ
PAYMENT_CREATE
PAYMENT_REFUND 27. Notification Service

Dùng Kafka:

Order Service
↓
OrderCreatedEvent
↓
Kafka
↓
Notification Service

Ví dụ:

OrderCreated
OrderCancelled
PaymentCompleted
UserRegistered 28. SSO logout

Phải làm logout thật sự.

Có:

Local logout

và:

SSO logout

Ví dụ:

User
↓
Logout Monolith
↓
SSO logout
↓
SSO session destroyed

Sau đó:

Microservice
↓
SSO
↓
login required 29. Refresh token

Flow:

Access Token
↓
expired
↓
Refresh Token
↓
SSO
↓
new Access Token

Nên implement:

Refresh Token Rotation

Ví dụ:

RT1
↓
RT2

RT1 bị invalid sau khi sử dụng.

Nếu RT1 bị replay:

security incident

có thể revoke token family.

30. Token strategy

Access token:

5–15 minutes

Refresh token:

days/weeks

Không nên làm:

Access Token = 30 days 31. Key management

Không hard-code:

JWT secret

trong source code.

Nên dùng asymmetric key:

private key
↓
SSO
↓
sign JWT

public key
↓
Gateway
↓
verify JWT

Expose:

/.well-known/openid-configuration

và:

/jwks.json

Gateway có thể lấy public keys.

32. Key rotation

Đây là feature rất đáng làm.

Ban đầu:

key-v1

Sau đó:

key-v2

SSO:

sign bằng v2

nhưng vẫn publish:

v1
v2

để token cũ vẫn verify được.

Sau khi token v1 hết hạn:

remove v1 33. Security audit

Lưu:

LOGIN_SUCCESS
LOGIN_FAILED
LOGOUT
TOKEN_REFRESH
PASSWORD_CHANGED
ROLE_CHANGED
PERMISSION_CHANGED
USER_CREATED
USER_DISABLED

Ví dụ:

audit_logs

id
user_id
action
ip_address
user_agent
resource
resource_id
timestamp
metadata 34. Brute-force protection

Login:

5 failed attempts

=> lock hoặc rate-limit.

Dùng:

Redis

Ví dụ:

login:attempt:{username}

TTL:

5 minutes 35. Rate limiting

Gateway:

/IP

Ví dụ:

100 requests/minute

Authentication endpoint:

10 requests/minute

nhạy cảm hơn.

36. CORS

Phải cấu hình:

Frontend
↓
Gateway

Không:

allowOrigins("\*")

trong production.

37. CSRF

Nếu frontend dùng:

Authorization: Bearer JWT

thì khác với:

Cookie Session

Bạn nên document rõ:

Cookie-based authentication

và:

Bearer token authentication

khác nhau như thế nào.

Đây là một phần interview rất hay.

38. Database architecture

Monolith:

monolith_db

Microservice:

user_db
product_db
order_db
payment_db

Không dùng chung database schema giữa các microservice.

Đừng làm:

user-service ──┐
order-service ─┼──> giant_db
payment ───────┘

nếu mục tiêu là học microservice architecture.

39. Service-to-service authentication

Ví dụ:

Order Service
↓
Payment Service

Không nên:

X-User-Role: ADMIN

để service tự tin tưởng nhau.

Nên có:

Service Identity

Ví dụ:

order-service

có credential riêng.

Có thể dùng:

OAuth2 Client Credentials

Flow:

Order Service
↓
SSO
↓
Client Credentials
↓
Service Access Token
↓
Payment Service 40. Phân biệt hai loại token

Đây là phần cực kỳ quan trọng.

User token
user → API

Ví dụ:

sub = user-123
Service token
service → service

Ví dụ:

client_id = order-service

Không nên đánh đồng:

user authentication

với:

service authentication 41. Gateway

Gateway chịu:

Routing
JWT validation
Rate limiting
CORS
Request ID
Correlation ID
Trusted identity propagation

Ví dụ:

/api/users/** → user-service
/api/products/** → product-service
/api/orders/** → order-service
/api/payments/** → payment-service 42. Resilience

Microservice phải có:

Timeout
Retry
Circuit Breaker
Bulkhead
Rate Limit

Ví dụ:

Order Service
↓
Payment Service

Payment chết.

Không được:

request treo 30 seconds

Mà:

timeout
↓
circuit breaker
↓
fallback/error

Có thể dùng Resilience4j.

43. Distributed tracing

Mỗi request có:

X-Correlation-Id

Ví dụ:

request-8a92

Flow:

Gateway
request-8a92
↓
Order Service
request-8a92
↓
Payment Service
request-8a92

Sau đó xem log:

request-8a92

là tìm được toàn bộ request.

44. Observability

Nên có:

Prometheus
Grafana
Loki
OpenTelemetry

Tối thiểu:

Metrics
Logs
Tracing

Dashboard:

HTTP request count
HTTP latency
Error rate
JVM memory
CPU
Kafka lag
DB connection pool 45. Kafka

Kafka dùng cho event:

OrderCreated
PaymentCompleted
UserRegistered

Không dùng Kafka cho mọi thứ.

Ví dụ:

GET /orders/123

=> HTTP/gRPC.

Còn:

OrderCreated

=> Kafka.

46. Outbox Pattern

Bạn đã từng hỏi về Outbox, project này rất phù hợp để áp dụng.

Ví dụ:

Order DB transaction
│
├── orders
│
└── outbox_events

Commit thành công.

Sau đó:

Outbox Publisher
↓
Kafka
↓
Notification

Tránh:

DB commit thành công
Kafka publish thất bại

=> mất event.

47. Idempotency

Order:

POST /orders
Idempotency-Key: abc123

User click 2 lần:

Request 1 → create order
Request 2 → return existing order

Đây là feature cực tốt để showcase distributed system.

48. Monolith cũng phải có những feature này?

Không nhất thiết.

Đây chính là điểm của project:

Feature Monolith Microservice
SSO ✅ ✅
OAuth2/OIDC ✅ ✅
JWT ✅ ✅
RBAC ✅ ✅
@PreAuthorize ✅ ❌/không chính
Gateway ❌ ✅
Header propagation ❌ ✅
Service identity ❌ ✅
Kafka optional ✅
Outbox optional ✅
Circuit breaker ❌ ✅
Distributed tracing optional ✅
Multiple DB ❌ ✅
Service-to-service auth ❌ ✅ 49. Frontend

Bạn nói chỉ cần 2 app riêng biệt, vậy mình sẽ làm:

monolith-web

và:

microservice-web

Không cần tạo frontend riêng cho từng microservice.

Frontend gọi:

Monolith API

hoặc:

API Gateway 50. Flow Monolith
Browser
│
│ login
▼
SSO
│
│ authorization code
▼
Monolith
│
│ token exchange
▼
SSO
│
│ tokens
▼
Monolith
│
▼
Spring Security
│
▼
SecurityContext
│
▼
@PreAuthorize
│
▼
Service
│
▼
Database 51. Flow Microservice
Browser
│
▼
Microservice Web
│
▼
SSO
│
▼
Authorization Code
│
▼
Gateway
│
▼
JWT validation
│
├── user_id
├── roles
└── permissions
│
▼
Trusted identity propagation
│
▼
Order Service
│
▼
AuthorizationService
│
▼
Database 52. Project phases

Mình sẽ chia dự án thành 14 phase.

PHASE 0 — Architecture

Tạo:

C4 Context Diagram
C4 Container Diagram
Sequence Diagram
Deployment Diagram
Security Architecture

Document:

docs/
├── architecture.md
├── authentication.md
├── authorization.md
├── token.md
├── security.md
└── deployment.md 53. PHASE 1 — Infrastructure

Docker Compose:

PostgreSQL
Redis
Kafka
Kafka UI
Prometheus
Grafana

Sau đó:

docker compose up

tất cả chạy được.

54. PHASE 2 — SSO Server

Implement:

User
Role
Permission
Client
Session

Endpoints quản trị:

POST /admin/users
GET /admin/users
POST /admin/roles
POST /admin/permissions

OAuth2/OIDC:

/authorize
/token
/logout
/jwks
/.well-known/openid-configuration 55. PHASE 3 — Authentication

Implement:

Login
Logout
Refresh
Password change
Account lock
Email verification

Security:

BCrypt/Argon2
Rate limiting
Audit log
Session management 56. PHASE 4 — Monolith

Build:

monolith-app

Modules:

user
product
order
payment
security
audit

Security:

Spring Security
OAuth2 Login
JWT
Method Security 57. PHASE 5 — Monolith Authorization

Implement:

@PreAuthorize

Các case:

ADMIN
MANAGER
USER

và:

resource ownership

Ví dụ:

USER → own orders
MANAGER → department orders
ADMIN → everything 58. PHASE 6 — Microservices

Tách:

gateway
user-service
product-service
order-service
payment-service
notification-service

Mỗi service:

own database
own deployment
own configuration 59. PHASE 7 — Gateway Security

Implement:

JWT validation
Issuer validation
Audience validation
Expiration validation
Signature validation
JWKS

Sau đó:

strip client identity headers

rồi:

add trusted headers 60. PHASE 8 — Microservice Authorization

Implement:

CurrentUser
AuthorizationService
PermissionEvaluator
ResourceOwnership

Ví dụ:

authorizationService.require("ORDER_CANCEL");

và:

authorizationService.requireOwner(orderId); 61. PHASE 9 — Service-to-Service Security

Implement:

Client Credentials

Ví dụ:

Order Service
↓
SSO
↓
Service Token
↓
Payment Service

Payment Service chỉ cho phép:

order-service

gọi endpoint:

POST /internal/payments 62. PHASE 10 — Distributed Communication

HTTP:

Gateway → service

REST hoặc Feign.

Async:

Order
↓
Kafka
↓
Notification

Implement:

retry
dead-letter topic
idempotent consumer 63. PHASE 11 — Distributed Transaction

Implement:

Transactional Outbox

Sau đó làm một flow:

Create Order
↓
Reserve Payment
↓
Payment
↓
Order confirmation

Có thể mô phỏng Saga:

Order Created
↓
Payment Requested
↓
Payment Success
↓
Order Confirmed

Nếu payment fail:

Payment Failed
↓
Order Cancelled 64. PHASE 12 — Resilience

Test:

Payment Service DOWN

Order Service phải:

timeout
retry
circuit breaker

Không làm sập toàn hệ thống.

65. PHASE 13 — Observability

Implement:

OpenTelemetry
Prometheus
Grafana
Loki

Theo dõi:

request
↓
gateway
↓
order
↓
payment

với cùng:

traceId 66. PHASE 14 — Production Deployment

Cuối cùng:

Docker

Build:

sso-server
monolith-app
api-gateway
user-service
product-service
order-service
payment-service
notification-service

Mỗi service là container riêng.

Có thể deploy:

VPS 1
└── SSO

VPS 2
└── Monolith

VPS 3
└── Gateway
└── Microservices

Hoặc:

Kubernetes

về sau.

67. CI/CD

GitHub Actions:

git push
↓
test
↓
build
↓
Docker image
↓
push registry
↓
deploy

Pipeline:

unit test
integration test
security scan
docker build
docker push
deploy 68. Testing

Đừng chỉ test:

200 OK

Security phải test rất nhiều.

Authentication
wrong password
locked account
expired token
invalid token
revoked refresh token
refresh token reuse
Authorization
USER → admin endpoint
ADMIN → admin endpoint
USER → own order
USER → other user's order
Gateway
fake X-User-Id
fake X-User-Role
expired JWT
wrong issuer
wrong audience
invalid signature
Microservice
service token
invalid service token
wrong service 69. Integration test

Ví dụ test:

User login
↓
SSO
↓
JWT
↓
Gateway
↓
Order Service
↓
Database

Test end-to-end.

70. Security attack scenarios

Mình cực kỳ khuyên bạn cố tình attack chính hệ thống của mình.

Test:

JWT tampering
alg:none
expired JWT
wrong audience
wrong issuer
header spoofing
privilege escalation
IDOR
refresh token replay
brute force

Đây sẽ biến project từ:

"Một project CRUD có login"

thành:

"Một distributed security platform".

71. Database cuối cùng
    SSO
    users
    roles
    permissions
    user_roles
    role_permissions
    oauth_clients
    sessions
    refresh_tokens
    audit_logs
    Monolith
    users/profile
    products
    orders
    order_items
    payments
    audit_logs
    Microservice
    user_db
    users

product_db
products

order_db
orders
order_items
outbox_events

payment_db
payments
outbox_events 72. Những thứ tuyệt đối không nên làm
Sai 1
Microservices
↓
shared database
Sai 2
Client
↓
X-User-Role: ADMIN

rồi service tin.

Sai 3
Every request
↓
Auth Service
↓
decode JWT
Sai 4
common-library

chứa toàn bộ domain.

Sai 5
JWT secret

hard-code.

Sai 6
Access Token = 30 days
Sai 7

Microservice nào cũng gọi:

User Service

để kiểm tra user mỗi request.

73. Một điểm rất quan trọng về @PreAuthorize

Bạn không nên kết luận rằng:

Monolith dùng @PreAuthorize, Microservice không dùng @PreAuthorize.

Thực tế:

@PreAuthorize

vẫn có thể dùng trong microservice.

Sự khác biệt lớn hơn nằm ở:

Where is the security boundary?
Where is identity established?
Where is authorization enforced?
How is identity propagated?
How do services authenticate each other?

Do đó project của bạn nên cố tình triển khai hai cách:

Monolith
Spring Security

- SecurityContext
- @PreAuthorize
  Microservice
  Gateway
- JWT validation
- trusted identity propagation
- service-level AuthorizationService

Như vậy khi phỏng vấn bạn có thể giải thích tại sao chọn cách đó, chứ không đơn giản là "microservice thì không dùng @PreAuthorize".

74. Kiến trúc cuối cùng

Mình hình dung project hoàn chỉnh như này:

                         ┌─────────────────┐
                         │    Frontend     │
                         └────────┬────────┘
                                  │
                   ┌──────────────┴──────────────┐
                   │                             │
                   ▼                             ▼
          ┌─────────────────┐           ┌─────────────────┐
          │  Monolith App   │           │ Microservice UI │
          │                 │           └────────┬────────┘
          │ Spring Security │                    │
          │ @PreAuthorize   │                    ▼
          └────────┬────────┘             ┌───────────────┐
                   │                      │ API Gateway   │
                   │                      └───────┬───────┘
                   │                              │
                   │                 ┌────────────┼─────────────┐
                   │                 │            │             │
                   │                 ▼            ▼             ▼
                   │              User Svc    Order Svc    Product Svc
                   │                              │
                   │                              ▼
                   │                         Payment Svc
                   │                              │
                   │                              ▼
                   │                         Notification
                   │
                   │
                   └──────────────┐
                                  │
                                  ▼
                         ┌─────────────────┐
                         │    SSO Server   │
                         │                 │
                         │ OAuth2          │
                         │ OIDC            │
                         │ JWT             │
                         │ RBAC            │
                         │ Sessions        │
                         │ Refresh Token   │
                         └────────┬────────┘
                                  │
                   ┌──────────────┼──────────────┐
                   ▼              ▼              ▼
                Postgres        Redis          Kafka

75. Thứ tự code thực tế

Nếu bạn để AI code dự án này, đừng đưa toàn bộ plan 75 mục cho AI rồi bảo "build project". Rất dễ sinh ra một đống code nhưng integration không chạy.

Hãy bắt AI thực hiện theo thứ tự:

1.  Architecture
2.  Infrastructure
3.  SSO Server
4.  OAuth2/OIDC
5.  User/Role/Permission
6.  Monolith authentication
7.  Monolith authorization
8.  Monolith business
9.  Microservice skeleton
10. API Gateway
11. JWT validation
12. Identity propagation
13. Microservice authorization
14. Service-to-service authentication
15. Kafka
16. Outbox
17. Saga
18. Resilience
19. Observability
20. Security testing
21. Integration testing
22. Docker
23. CI/CD
24. Production deployment

Mỗi phase phải có Definition of Done và integration test trước khi chuyển phase tiếp theo.

Đây là điểm mình đặc biệt khuyên bạn làm nếu dự án này được code bằng AI: AI không nên được phép tự quyết định kiến trúc giữa các phase. Architecture → contract → implementation → test → verify → mới sang phase tiếp theo.

Và mình sẽ nâng project này lên một mức nữa

Nếu mục tiêu của bạn là dùng project này làm project showcase/interview Java Backend, mình không chỉ dừng ở "SSO + 2 architecture". Ta có thể biến nó thành một Security/Distributed System Lab với một bộ test scenario rất đẹp:

                    SSO PLATFORM
                         │
          ┌──────────────┴───────────────┐
          │                              │
      MONOLITH                     MICROSERVICES
          │                              │
    @PreAuthorize                 API Gateway
          │                              │
    SecurityContext             JWT validation
                                         │
                                  Identity Propagation
                                         │
                                  Service Authorization
                                         │
                                Service-to-Service OAuth2
                                         │
                                Kafka + Outbox + Saga
                                         │
                              Circuit Breaker + Retry
                                         │
                             Distributed Tracing

Sau khi hoàn thành, bạn sẽ có thể dùng cùng một business requirement để giải thích cho interviewer:

"Nếu đặt toàn bộ hệ thống vào một process thì authentication/authorization hoạt động thế nào; khi tách thành distributed system thì security boundary thay đổi thế nào; tại sao không gọi Auth Service trên mỗi request; JWT được verify ở đâu; tại sao phải strip X-User-\*; user token khác service token thế nào; tại sao microservice không dùng chung database; Kafka giải quyết vấn đề gì; Outbox giải quyết vấn đề gì; và khi Payment Service chết thì Order Service xử lý thế nào."

Đó mới là giá trị lớn nhất của project này.

Nếu triển khai đúng plan trên, đây không còn là một project CRUD có SSO, mà gần như là một mini production-grade identity + distributed system platform để bạn học và showcase kiến trúc Java/Spring thực tế.

# SSO Platform - Sơ Đồ Tuần Tự (Sequence Diagrams)

Tài liệu này chứa sơ đồ tuần tự chi tiết mô tả các luồng nghiệp vụ và bảo mật cốt lõi trong hệ thống **SSO Platform**.

---

## 1. Luồng Đăng Nhập SSO (OAuth2 Authorization Code + PKCE)

Mô tả cách một Client App (Monolith hoặc Gateway) authenticate người dùng qua SSO Server sử dụng mã kiểm chứng PKCE (`code_challenge` / `code_verifier`).

```mermaid
sequenceDiagram
    autonumber
    actor User as User / Browser
    participant App as Client App
    participant SSO as SSO Server (Spring Auth Server)
    participant Redis as Redis Session Cache
    participant DB as SSO Database

    User->>App: Truy cập /dashboard
    App->>App: Detect: Chưa có Session / Token
    App->>User: Redirect đến /oauth2/authorize<br/>(Client_id, Code_Challenge, Challenge_Method, State)
    User->>SSO: GET /oauth2/authorize?client_id=...&code_challenge=...
    SSO->>SSO: Lưu challenge & state vào OAuth2 Auth Session
    SSO->>User: Trả về trang đăng nhập (Login Form)
    
    User->>SSO: POST /login (Username, Password)
    SSO->>Redis: Kiểm tra Brute-Force Counter (IP/Username)
    Alt Bị Lock
        SSO->>User: Trả về lỗi 429 Account Locked
    else Hợp lệ
        SSO->>DB: Truy vấn thông tin User & Password Hash
        SSO->>SSO: Verify BCrypt password hash
        SSO->>DB: Ghi Audit Log (LOGIN_SUCCESS)
        SSO->>SSO: Generate Authorization Code (Auth_Code)
        SSO->>User: Redirect về Client App với Auth_Code & State
    end

    User->>App: GET /login/oauth2/code/sso?code=Auth_Code&state=State
    App->>App: Validate State token
    App->>SSO: POST /oauth2/token (Auth_Code, Code_Verifier, Client_ID, Client_Secret)
    SSO->>SSO: Verify Code_Verifier bằng S256(Code_Verifier) == Code_Challenge
    SSO->>SSO: Verify Client credentials
    SSO->>DB: Load User Roles & Permissions
    SSO->>SSO: Sinh JWT (Access Token, ID Token, Refresh Token)
    SSO->>App: Trả về Tokens (Access, Refresh, ID Token)
    App->>Redis: Lưu Tokens vào Secure Session
    App->>User: Trả về /dashboard (HTTP 200)
```

---

## 2. Luồng SSO Cross-App (Single Sign-On)

Khi đã đăng nhập App 1, truy cập App 2 không cần nhập lại mật khẩu vì Session tại SSO Server vẫn tồn tại.

```mermaid
sequenceDiagram
    autonumber
    actor User as User / Browser
    participant App2 as Client App 2
    participant SSO as SSO Server
    participant Redis as Redis Session Cache

    User->>App2: Truy cập App 2
    App2->>App2: Detect: Chưa có token
    App2->>User: Redirect đến SSO /oauth2/authorize
    User->>SSO: GET /oauth2/authorize?client_id=app2-id&...
    SSO->>Redis: Check SSO Session Cookie (JSESSIONID)
    Note over SSO,Redis: Tìm thấy session đang active của User
    SSO->>SSO: Skip login screen, tự động sinh Auth_Code
    SSO->>User: Redirect về App 2 với Auth_Code
    User->>App2: GET /callback?code=Auth_Code
    App2->>SSO: POST /oauth2/token (Auth_Code, verifier...)
    SSO->>App2: Trả về Tokens (Access Token cho App 2)
    App2->>User: Hiển thị App 2 Dashboard (Đăng nhập thành công)
```

---

## 3. Luồng Xác Thực Hai Lớp (2FA / TOTP) khi Đăng Nhập

Quy trình đăng nhập nâng cao khi tài khoản đã kích hoạt 2FA.

```mermaid
sequenceDiagram
    autonumber
    actor User as User / Browser
    participant SSO as SSO Server
    participant Redis as Redis Cache
    participant DB as SSO Database

    User->>SSO: POST /login (Username, Password)
    SSO->>DB: Verify credentials (OK)
    SSO->>DB: Check: `totp_enabled` == true
    SSO->>SSO: Tạo Pre-Auth Token (thời hạn 3 phút)
    SSO->>User: Trả về HTTP 200 {"status": "REQUIRES_2FA", "preAuthToken": "..."}
    
    User->>User: Mở App Authenticator lấy mã 6 số (OTP)
    User->>SSO: POST /auth/2fa/verify (preAuthToken, otpCode)
    SSO->>SSO: Validate preAuthToken
    SSO->>DB: Load encrypted TOTP Secret, giải mã AES
    SSO->>SSO: Validate otpCode theo thuật toán TOTP (RFC 6238)
    Alt OTP Sai
        SSO->>User: Trả về HTTP 400 "Mã OTP không hợp lệ"
    else OTP Đúng
        SSO->>SSO: Sinh Access Token & Refresh Token chính thức
        SSO->>User: Trả về Tokens (Success)
    end
```

---

## 4. Centralized File Upload (MinIO + file-service)

Quy trình tải lên hình ảnh sản phẩm / avatar bảo mật thông qua Gateway và file-service lên MinIO Object Storage.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Browser / Client
    participant GW as API Gateway
    participant FS as file-service
    participant MinIO as MinIO S3 API

    Client->>GW: POST /api/files/upload (Form-Data: file, JWT)
    GW->>GW: Validate JWT, Strip headers
    GW->>GW: Inject X-User-Id
    GW->>FS: Forward upload request
    FS->>FS: Validate file: type == image/*, size < 5MB
    FS->>FS: Sinh tên file ngẫu nhiên (UUID) để tránh ghi đè
    FS->>MinIO: PutObject (file stream, bucket: "sso-platform")
    MinIO->>FS: Ack upload success
    FS->>FS: Sinh public URL hoặc presigned URL
    FS->>GW: Trả về JSON {"fileUrl": "http://minio/sso-platform/avatar-123.jpg"}
    GW->>Client: Trả về JSON cho Client
```

---

## 5. Luồng Tạo Đơn Hàng (Microservice + Outbox + Kafka)

Mô tả quá trình tạo Order đảm bảo tính nhất quán dữ liệu (Eventual Consistency) bằng Transactional Outbox Pattern.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Browser
    participant GW as API Gateway
    participant OrdSvc as Order Service
    participant DB as Order DB (PostgreSQL)
    participant Redis as Redis (Idempotency)
    participant Pub as Outbox Publisher (Scheduler)
    participant Kafka as Kafka Event Bus
    participant Notif as Notification Service

    Client->>GW: POST /api/orders (JWT, Idempotency-Key)
    GW->>GW: Validate JWT
    GW->>OrdSvc: Forward request + X-User-Id
    OrdSvc->>Redis: SETNX order:idempotency:{key} -> TTL 24h
    Alt Key đã tồn tại (Duplicate request)
        OrdSvc->>Client: Trả về Cached Response cũ (HTTP 409 hoặc 200)
    else Key mới
        OrdSvc->>DB: Bắt đầu Transaction
        OrdSvc->>DB: 1. Tạo Order Record (status: PENDING)
        OrdSvc->>DB: 2. Ghi Outbox Event (ORDER_CREATED)
        OrdSvc->>DB: Commit Transaction
        OrdSvc->>Client: Trả về 201 Created (Order Response)
    end

    Loop Mỗi 5 giây
        Pub->>DB: Quét bảng outbox_events với status = PENDING
        DB->>Pub: Trả về danh sách events
        Pub->>Kafka: Publish "ORDER_CREATED" event to topic 'order-created'
        Kafka->>Pub: Acknowledge
        Pub->>DB: Cập nhật status = SENT, sent_at = NOW
    end

    Kafka->>Notif: Consume "ORDER_CREATED" event
    Notif->>Notif: Check duplicate event_id
    Notif->>Notif: Thực hiện gửi email xác nhận (Mock)
```

---

## 6. Refresh Token Rotation & Replay Attack Detection

Bảo vệ hệ thống khỏi việc Refresh Token bị đánh cắp và dùng lại.

```mermaid
sequenceDiagram
    autonumber
    actor Attacker as Attacker
    actor User as Legitimate User
    participant SSO as SSO Server
    participant DB as SSO Database

    Note over User,SSO: Cả User và Attacker đều có Refresh Token 1 (RT1)
    
    User->>SSO: POST /oauth2/token (grant_type=refresh_token, token=RT1)
    SSO->>DB: Tìm RT1 trong database (revoked == false)
    SSO->>DB: Đánh dấu RT1 đã sử dụng (revoked = true, lý do: USED)
    SSO->>DB: Tạo cặp token mới: Access Token 2 (AT2) + Refresh Token 2 (RT2)
    SSO->>User: Trả về AT2 + RT2
    
    Note over Attacker,SSO: Sau đó, Attacker cố gắng dùng lại RT1 đã bị thu hồi
    Attacker->>SSO: POST /oauth2/token (grant_type=refresh_token, token=RT1)
    SSO->>DB: Tìm RT1 trong database -> phát hiện: revoked == true (USED)!
    SSO->>SSO: Detect: REPLAY ATTACK!
    SSO->>DB: Ghi Audit Log: SECURITY_INCIDENT_REPLAY_ATTACK
    SSO->>DB: Thu hồi toàn bộ Refresh Tokens cùng Family ID với RT1 (Force Logout)
    SSO->>Attacker: Trả về HTTP 401 Unauthorized
    
    Note over User,SSO: Lần tiếp theo Legitimate User dùng RT2
    User->>SSO: POST /oauth2/token (grant_type=refresh_token, token=RT2)
    SSO->>DB: Tìm RT2 -> Trạng thái: revoked == true (do bị thu hồi hàng loạt)!
    SSO->>User: Trả về HTTP 401 Unauthorized (Bắt buộc phải login lại)
```

---

## 7. Service-to-Service Authentication (Client Credentials)

Order Service gọi Payment Service an toàn bằng Token riêng của Service.

```mermaid
sequenceDiagram
    autonumber
    participant Ord as Order Service
    participant SSO as SSO Server
    participant Redis as Redis (Token Cache)
    participant Pay as Payment Service

    Ord->>Redis: Lấy cached Service Token cho 'payment-service'
    Alt Không có token / Token hết hạn
        Ord->>SSO: POST /oauth2/token (grant_type=client_credentials, scope=payment:write)
        SSO->>SSO: Xác thực client credentials của Order Service
        SSO->>Ord: Trả về Service Access Token (sub = 'order-service')
        Ord->>Redis: Cache Token (TTL < Token Expiry)
    end

    Ord->>Pay: POST /internal/payments (Bearer Service_Token, Body)
    Pay->>Pay: Validate Service_Token (JWKS local check)
    Pay->>Pay: Verify claim `client_id` == 'order-service' VÀ `scope` chứa 'payment:write'
    Pay->>Pay: Xử lý thanh toán
    Pay->>Ord: Trả về Kết quả thanh toán
```

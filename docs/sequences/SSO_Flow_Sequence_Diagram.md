# Biểu Đồ Trình Tự SSO Flow (Sequence Diagram)

Tài liệu này mô tả chi tiết quy trình đăng nhập tập trung (Single Sign-On), kiểm tra phiên hoạt động chéo ứng dụng (Cross-App Session) và đăng xuất đồng bộ (Back-Channel Logout) giữa **SSO Server**, **Monolith App**, và **Microservice App**.

---

## 1. Đăng Nhập Tập Trung (SSO Login Flow)

Khi người dùng truy cập Monolith App lần đầu và chưa đăng nhập:

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng (Browser)
    participant Mono as Monolith App
    participant SSO as SSO Server (OAuth2/OIDC)
    participant Redis as Redis Session Cache

    User->>Mono: 1. Truy cập trang yêu cầu đăng nhập (/secure)
    Mono-->>User: 2. Trả về mã Redirect 302 đến SSO Server (/oauth2/authorize)
    User->>SSO: 3. Redirect gửi kèm ClientID, RedirectURI & PKCE Challenge
    SSO->>Redis: 4. Kiểm tra session cookie hiện tại của trình duyệt
    Redis-->>SSO: 5. Không tìm thấy phiên hoạt động (Chưa đăng nhập)
    SSO-->>User: 6. Trả về giao diện Đăng Nhập (login.html)
    User->>SSO: 7. Gửi Username & Password (POST /login)
    Note over SSO, Redis: Nếu tài khoản bật 2FA, hiển thị màn hình 2FA và xác thực OTP
    SSO->>Redis: 8. Tạo phiên đăng nhập (Session) & Lưu cookie trình duyệt
    SSO-->>User: 9. Trả về Authorization Code thông qua RedirectURI của Monolith
    User->>Mono: 10. Gửi Auth Code đến Monolith App (/login/oauth2/code/monolith)
    Mono->>SSO: 11. Đổi Auth Code lấy Tokens (POST /oauth2/token + PKCE Verifier)
    SSO-->>Mono: 12. Trả về ID Token & Access Token (JWT)
    Mono-->>User: 13. Đăng nhập thành công, lưu session chéo cục bộ trên Monolith
```

---

## 2. Kiểm Tra Phiên Hoạt Động Chéo Ứng Dụng (SSO Cross-App Session Flow)

Khi người dùng đã đăng nhập ở Monolith App, nay mở tiếp Microservice App:

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng (Browser)
    participant Micro as Microservice App (API Gateway)
    participant SSO as SSO Server (OAuth2/OIDC)
    participant Redis as Redis Session Cache

    User->>Micro: 1. Truy cập Microservice App lần đầu
    Micro-->>User: 2. Trả về mã Redirect 302 đến SSO Server (/oauth2/authorize)
    User->>SSO: 3. Redirect gửi kèm ClientID & RedirectURI mới
    SSO->>Redis: 4. Kiểm tra session cookie trình duyệt tự động gửi kèm
    Redis-->>SSO: 5. Phát hiện phiên hoạt động hợp lệ đã tồn tại (Đăng nhập ở bước trước)
    Note over SSO: Phát hiện session đã đăng nhập thành công. Bỏ qua nhập Password.
    SSO-->>User: 6. Tạo Authorization Code mới ngay lập tức và Redirect về
    User->>Micro: 7. Gửi Auth Code đến API Gateway (/login/oauth2/code/gateway)
    Micro->>SSO: 8. Đổi Auth Code lấy JWT Tokens
    SSO-->>Micro: 9. Trả về ID Token & Access Token (JWT)
    Micro-->>User: 10. Truy cập thành công mà không cần nhập lại mật khẩu
```

---

## 3. Đăng Xuất Đồng Bộ (Back-Channel Logout Flow)

Khi người dùng thực hiện đăng xuất khỏi Monolith App:

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng (Browser)
    participant Mono as Monolith App
    participant SSO as SSO Server
    participant Gateway as API Gateway (Microservice App)
    participant Redis as Redis Session Cache

    User->>Mono: 1. Gửi yêu cầu Logout (POST /logout)
    Mono->>SSO: 2. Redirect về SSO Server Logout Endpoint (/connect/logout)
    SSO->>Redis: 3. Hủy phiên hoạt động (Session) của User trên Redis
    
    par Back-Channel Logout tới Monolith
        SSO->>Mono: 4. POST /logout/back-channel (gửi kèm Logout Token JWT)
        Note over Mono: Thu hồi Session cục bộ của User
        Mono-->>SSO: Phản hồi 200 OK
    and Back-Channel Logout tới Gateway
        SSO->>Gateway: 5. POST /logout/back-channel (gửi kèm Logout Token JWT)
        Note over Gateway: Thu hồi Session cục bộ của User
        Gateway-->>SSO: Phản hồi 200 OK
    end
    
    SSO-->>User: 6. Chuyển hướng về màn hình Đăng Nhập (/login?logout)
```

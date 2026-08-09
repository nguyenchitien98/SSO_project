# SSO Platform - Hướng Dẫn Tích Hợp Xác Thực Hai Lớp (2FA / TOTP)

Tài liệu này hướng dẫn chi tiết cách triển khai tính năng Xác thực 2 lớp (Two-Factor Authentication - 2FA) bằng phương thức **TOTP (Time-Based One-Time Password)** trong dự án **SSO Platform**.

---

## 1. Nguyên Lý Hoạt Động (TOTP - RFC 6238)

TOTP là thuật toán tạo mật khẩu một lần (OTP) dựa trên thời gian thực tế. Công thức cốt lõi:
$$\text{OTP} = \text{HMAC-SHA1}(\text{SecretKey}, \text{TimeStep})$$
Trong đó:
- **SecretKey:** Chuỗi ngẫu nhiên dạng Base32 được sinh ra riêng cho mỗi User.
- **TimeStep:** Số chu kỳ thời gian đã trôi qua kể từ mốc thời gian Epoch Unix. Mỗi chu kỳ dài **30 giây** ($\text{TimeStep} = \lfloor \text{CurrentUnixTime} / 30 \rfloor$).

---

## 2. Quy Trình Nghiệp Vụ & Luồng API

### Bước 1: Kích Hoạt 2FA (Setup)
1. User nhấn nút "Bật 2FA" trên giao diện cấu hình Profile.
2. Frontend gọi API của SSO Server: `POST /api/2fa/setup`.
3. SSO Server sinh khóa Secret ngẫu nhiên (32 ký tự Base32) và trả về thông tin dưới dạng QR Code URL:
   ```
   otpauth://totp/SSO-Platform:admin@example.com?secret=NBSWY3DPEB3W64TBNQ&issuer=SSO-Platform
   ```
4. Frontend render URL này thành ảnh QR Code và hiển thị cho người dùng quét bằng ứng dụng Google Authenticator / Microsoft Authenticator.

### Bước 2: Xác Nhận Bật (Verification Code)
1. Sau khi quét mã QR, ứng dụng Authenticator của user bắt đầu sinh mã OTP 6 số thay đổi mỗi 30 giây.
2. User nhập mã OTP 6 số hiện tại vào Frontend và nhấn "Xác Nhận".
3. Frontend gọi API: `POST /api/2fa/enable` kèm mã OTP.
4. SSO Server verify mã OTP:
   - Nếu khớp: Mã hóa đối xứng AES Secret Key, lưu vào DB cột `totp_secret`, đặt `totp_enabled = true` và sinh 8 mã Backup Codes trả về cho user lưu trữ.
   - Nếu không khớp: Trả về lỗi 400.

### Bước 3: Đăng Nhập Với 2FA (Login Verification)
Khi người dùng đăng nhập bằng Username/Password thông thường:
1. SSO Server kiểm tra trạng thái 2FA của User.
2. Nếu `totp_enabled == true`, SSO Server **chưa phát hành access token ngay**, mà trả về HTTP 200 kèm status đặc biệt:
   ```json
   {
     "status": "REQUIRES_2FA",
     "preAuthToken": "temp-session-token-valid-3-minutes"
   }
   ```
3. Frontend hiển thị màn hình nhập mã 2FA (tương ứng với mockup 1.6 trong UI).
4. User nhập mã OTP 6 số, Frontend gửi request: `POST /oauth2/verify-2fa` (kèm `preAuthToken` và `otpCode`).
5. SSO Server xác thực thành công → Trả về Access Token, ID Token, Refresh Token chính thức.

---

## 3. Cấu Trúc Code Java (Spring Boot)

### 3.1 Thư viện khuyên dùng
Sử dụng thư viện `com.warrenstrange:googleauth` trong `pom.xml`:
```xml
<dependency>
    <groupId>com.warrenstrange</groupId>
    <artifactId>googleauth</artifactId>
    <version>1.5.0</version>
</dependency>
```

### 3.2 Service xử lý TOTP
```java
/**
 * Service xử lý các nghiệp vụ liên quan đến xác thực 2FA/TOTP.
 *
 * <p>Tại sao cần mã hóa Secret Key?
 * TOTP Secret Key là chìa khóa duy nhất để sinh ra mã OTP hợp lệ. Nếu bị rò rỉ cơ sở dữ liệu,
 * hacker có thể chiếm đoạt và tự sinh OTP của bất kỳ user nào. Do đó ta sử dụng thuật toán
 * mã hóa đối xứng AES-GCM-256 để bảo vệ key này trong cơ sở dữ liệu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TotpService {

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    private final EncryptionUtils encryptionUtils; // Helper mã hóa AES

    /**
     * Sinh khóa bí mật ngẫu nhiên Base32 cho User.
     */
    public String generateSecretKey() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /**
     * Sinh QR Code URL chuẩn otpauth.
     */
    public String getQrCodeUrl(String secretKey, String email) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL("SSO-Platform", email, 
            new GoogleAuthenticatorKey.Builder(secretKey).build());
    }

    /**
     * Xác thực mã OTP người dùng gửi lên.
     * Hỗ trợ clock drift (lệch múi giờ) trong khoảng cho phép (30 giây trước/sau).
     *
     * @param encryptedSecret Khóa secret đã được mã hóa lấy từ DB
     * @param verificationCode Mã OTP 6 số người dùng nhập
     */
    public boolean verifyCode(String encryptedSecret, int verificationCode) {
        try {
            // Giải mã secret key bằng AES trước khi verify
            String plainSecret = encryptionUtils.decrypt(encryptedSecret);
            return gAuth.authorize(plainSecret, verificationCode);
        } catch (Exception e) {
            log.error("Lỗi giải mã hoặc xác thực TOTP", e);
            return false;
        }
    }
}
```

---

## 4. Các Quy Tắc Bảo Mật Bắt Buộc (Security Rules)

1. **Mã hóa secrets:** Cột `totp_secret` trong DB tuyệt đối không lưu dạng plain-text. Phải dùng thuật toán mã hóa đối xứng mạnh (AES-256-GCM hoặc tương đương). KEK (Key Encryption Key) để mã hóa phải được lưu ở môi trường an toàn (Environment Variable, HashiCorp Vault), không được commit vào mã nguồn.
2. **Chống Replay Attack OTP:** Để ngăn kẻ tấn công chặn gói tin và dùng lại mã OTP trong vòng 30 giây của chu kỳ, SSO Server bắt buộc phải cache mã OTP đã verify thành công vào Redis với TTL = 30 giây. Nếu mã đó được gửi lại lần nữa → Từ chối ngay.
3. **Backup Codes:**
   - Khi kích hoạt 2FA, sinh ra 8 mã backup ngẫu nhiên dạng `XXXX-XXXX`.
   - Các mã backup này phải được Hash bằng BCrypt/Argon2 trước khi lưu vào DB (giống như mật khẩu).
   - Mỗi mã backup chỉ được sử dụng một lần duy nhất để đăng nhập khẩn cấp.

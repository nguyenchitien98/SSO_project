package com.sso.server.security;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;

/**
 * Lớp tiện ích (Utility) hỗ trợ cấu hình và xác thực mã OTP 2 lớp (TOTP 2FA).
 *
 * <p>Sử dụng thư viện AeroGear OTP để sinh secret key ngẫu nhiên theo chuẩn Base32 và xác thực mã
 * OTP 6 chữ số sinh ra từ Google Authenticator hoặc Microsoft Authenticator.
 *
 * @author SSO Platform Team
 * @since Sprint 04
 */
@Slf4j
public class TotpUtils {

  private static final String ISSUER = "SSO-Platform";

  /**
   * Sinh mã bí mật ngẫu nhiên (Secret Key) chuẩn Base32.
   *
   * @return Chuỗi mã bí mật dạng Base32
   */
  public static String generateSecretKey() {
    return Base32.random();
  }

  /**
   * Tạo URL mã QR (QR Code URL) để người dùng quét vào các app Authenticator.
   *
   * <p>Cấu trúc URL tuân thủ đặc tả Key Uri Format của Google Authenticator: {@code
   * otpauth://totp/[Issuer]:[Username]?secret=[Secret]&issuer=[Issuer]}
   *
   * @param secretKey Mã bí mật Base32 của người dùng
   * @param username Tên đăng nhập của người dùng
   * @return Chuỗi URL dùng để tạo mã QR Code
   */
  public static String getQrCodeUrl(String secretKey, String username) {
    try {
      return String.format(
          "otpauth://totp/%s:%s?secret=%s&issuer=%s",
          URLEncoder.encode(ISSUER, StandardCharsets.UTF_8.name()),
          URLEncoder.encode(username, StandardCharsets.UTF_8.name()),
          secretKey,
          URLEncoder.encode(ISSUER, StandardCharsets.UTF_8.name()));
    } catch (UnsupportedEncodingException e) {
      log.error("Lỗi encode ký tự khi sinh URL QR Code", e);
      throw new IllegalStateException("Không thể encode dữ liệu QR Code");
    }
  }

  /**
   * Xác thực mã OTP 6 chữ số dựa trên mã bí mật (Secret Key) và thời điểm hiện tại.
   *
   * @param secretKey Mã bí mật Base32 cần đối chiếu
   * @param code Mã OTP 6 số do người dùng nhập
   * @return {@code true} nếu mã chính xác, ngược lại {@code false}
   */
  public static boolean verifyOtp(String secretKey, String code) {
    if (secretKey == null || secretKey.isBlank() || code == null || code.isBlank()) {
      return false;
    }
    try {
      Totp totp = new Totp(secretKey);
      return totp.verify(code.trim());
    } catch (Exception e) {
      log.warn("Lỗi kiểm tra tính hợp lệ của mã OTP", e);
      return false;
    }
  }
}

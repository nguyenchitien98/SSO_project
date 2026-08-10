package com.sso.server.security;

import static org.junit.jupiter.api.Assertions.*;

import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.Test;

/**
 * Lớp kiểm thử đơn vị (Unit Test) cho {@link TotpUtils}.
 *
 * @author SSO Platform Team
 * @since Sprint 04
 */
class TotpUtilsTest {

  /** Test sinh khóa bí mật ngẫu nhiên (Base32 format). */
  @Test
  void generateSecretKey() {
    String secret = TotpUtils.generateSecretKey();
    assertNotNull(secret);
    assertFalse(secret.isBlank());
    assertEquals(16, secret.length(), "Mã secret key chuẩn Base32 của AeroGear OTP có độ dài 16");
  }

  /** Test tạo QR Code URL đúng định dạng. */
  @Test
  void getQrCodeUrl() {
    String secret = "MZXW6YTBOI======";
    String url = TotpUtils.getQrCodeUrl(secret, "testuser");

    assertNotNull(url);
    assertTrue(url.startsWith("otpauth://totp/SSO-Platform:testuser?secret="));
    assertTrue(url.contains("secret=MZXW6YTBOI======"));
    assertTrue(url.contains("issuer=SSO-Platform"));
  }

  /** Test kiểm tra tính hợp lệ của OTP (Happy path & Failure path). */
  @Test
  void verifyOtp() {
    String secret = TotpUtils.generateSecretKey();

    // Test mã linh tinh thì không khớp
    assertFalse(TotpUtils.verifyOtp(secret, "123456"));
    assertFalse(TotpUtils.verifyOtp(secret, null));
    assertFalse(TotpUtils.verifyOtp(secret, ""));

    // Test mã chuẩn sinh ra từ thư viện Totp
    Totp totp = new Totp(secret);
    String currentCode = totp.now();
    assertTrue(TotpUtils.verifyOtp(secret, currentCode));
  }
}

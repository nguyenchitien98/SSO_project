package com.sso.server.security;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lớp kiểm thử bảo mật JWT (Security Test) mô phỏng các kịch bản tấn công Token.
 *
 * <p>Kiểm tra độ chính xác của các thuật toán mã hóa/xác thực chữ ký số RSA256 đối với JWT: - Happy
 * Path: Token hợp lệ được giải mã thành công. - JWT Tampering: Token bị giả mạo thông tin
 * payload/chữ ký bị từ chối. - Expired Token: Token hết hạn sử dụng bị từ chối. - Wrong Issuer:
 * Token được phát hành bởi bên thứ ba (Wrong Issuer) bị từ chối.
 *
 * @author SSO Platform Team
 * @since Sprint 05
 */
class JwtSecurityTest {

  private RSAPublicKey publicKey;
  private RSAPrivateKey privateKey;
  private JWSSigner signer;
  private JWSVerifier verifier;
  private String validIssuer = "http://localhost:9000";

  @BeforeEach
  void setUp() throws Exception {
    // Sinh cặp khóa RSA phục vụ ký nhận và xác minh chữ ký số
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    KeyPair kp = kpg.generateKeyPair();
    publicKey = (RSAPublicKey) kp.getPublic();
    privateKey = (RSAPrivateKey) kp.getPrivate();

    signer = new RSASSASigner(privateKey);
    verifier = new RSASSAVerifier(publicKey);
  }

  /** Kiểm thử giải mã JWT hợp lệ (Happy Path). */
  @Test
  void validateJwt_Success() throws Exception {
    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject("user-uuid-123")
            .issuer(validIssuer)
            .expirationTime(new Date(System.currentTimeMillis() + 900_000)) // 15 mins
            .issueTime(new Date())
            .claim("roles", "USER")
            .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
    signedJWT.sign(signer);

    String serializedToken = signedJWT.serialize();

    // Thực hiện xác minh
    SignedJWT parsedJWT = SignedJWT.parse(serializedToken);
    assertTrue(parsedJWT.verify(verifier), "Chữ ký hợp lệ phải được xác minh thành công");
    assertEquals(validIssuer, parsedJWT.getJWTClaimsSet().getIssuer());
    assertTrue(
        parsedJWT.getJWTClaimsSet().getExpirationTime().after(new Date()), "Token chưa hết hạn");
  }

  /**
   * Kiểm thử tấn công giả mạo nội dung JWT (JWT Tampering).
   *
   * <p>Mô phỏng hacker thay đổi nội dung claims (ví dụ nâng quyền lên ADMIN) nhưng giữ nguyên chữ
   * ký, hoặc tự sinh chữ ký bằng khóa riêng không trùng khớp với khóa công khai của SSO Server.
   */
  @Test
  void validateJwt_TamperedPayload_Fails() throws Exception {
    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject("user-uuid-123")
            .issuer(validIssuer)
            .expirationTime(new Date(System.currentTimeMillis() + 900_000))
            .claim("roles", "USER")
            .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
    signedJWT.sign(signer);

    String validSerializedToken = signedJWT.serialize();

    // 1. Hacker thay đổi payload của token thành ADMIN
    String[] parts = validSerializedToken.split("\\.");
    String originalHeader = parts[0];
    String tamperedPayloadBase64 =
        "eyJzdWIiOiJ1c2VyLXV1aWQtMTIzIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo5MDAwIiwiZXhwIjoyMDMwMDAwMDAwLCJyb2xlcyI6IkFETUlOIn0="; // roles: ADMIN
    String originalSignature = parts[2];

    String tamperedToken = originalHeader + "." + tamperedPayloadBase64 + "." + originalSignature;

    // Xác minh chữ ký của token giả mạo bằng public key chính thức
    SignedJWT parsedTamperedJWT = SignedJWT.parse(tamperedToken);
    assertFalse(parsedTamperedJWT.verify(verifier), "Chữ ký giả mạo phải bị phát hiện và từ chối");
  }

  /** Kiểm thử tấn công dùng Token hết hạn (Expired Token Replay). */
  @Test
  void validateJwt_ExpiredToken_Fails() throws Exception {
    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject("user-uuid-123")
            .issuer(validIssuer)
            .expirationTime(
                new Date(System.currentTimeMillis() - 1000)) // Đã hết hạn cách đây 1 giây
            .issueTime(new Date(System.currentTimeMillis() - 10000))
            .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
    signedJWT.sign(signer);

    String expiredToken = signedJWT.serialize();

    // Xác minh thời gian hết hạn
    SignedJWT parsedJWT = SignedJWT.parse(expiredToken);
    assertTrue(parsedJWT.verify(verifier));
    assertTrue(
        parsedJWT.getJWTClaimsSet().getExpirationTime().before(new Date()),
        "Token hết hạn phải bị xác nhận hết hạn");
  }

  /** Kiểm thử sử dụng Token phát hành từ nhà cung cấp giả mạo (Wrong Issuer Attack). */
  @Test
  void validateJwt_WrongIssuer_Fails() throws Exception {
    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject("user-uuid-123")
            .issuer("http://fake-sso-server.com") // Issuer giả mạo
            .expirationTime(new Date(System.currentTimeMillis() + 900_000))
            .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
    signedJWT.sign(signer);

    String fakeToken = signedJWT.serialize();

    SignedJWT parsedJWT = SignedJWT.parse(fakeToken);
    assertTrue(parsedJWT.verify(verifier));
    assertNotEquals(
        validIssuer,
        parsedJWT.getJWTClaimsSet().getIssuer(),
        "Hệ thống phải kiểm chứng Issuer và từ chối nếu không trùng khớp");
  }
}

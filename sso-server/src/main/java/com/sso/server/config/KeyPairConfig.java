package com.sso.server.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình sinh khóa mã hóa bất đối xứng RSA và công bố nguồn khóa JWK (JSON Web Key).
 *
 * <p>Sinh cặp khóa RSA 2048-bit động khi ứng dụng khởi động. Khóa Private được dùng để ký chữ ký số
 * cho các token JWT (Access Token, ID Token) do Authorization Server phát hành. Khóa Public được
 * công bố công khai qua JWKS endpoint (/oauth2/jwks) để các Client và Gateway tải về thực hiện kiểm
 * chứng chữ ký của token một cách ngoại tuyến (offline validation).
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Configuration
@Slf4j
public class KeyPairConfig {

  /**
   * Khởi tạo nguồn cung cấp JWK (JWKSource Bean).
   *
   * <p>Được Spring Authorization Server sử dụng làm nguồn khóa để ký tất cả các token.
   *
   * @return Đối tượng JWKSource bọc khóa RSA
   */
  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    log.info("Khởi tạo và cấu hình nguồn khóa bất đối xứng RSA-2048 cho SSO Server");
    RSAKey rsaKey = generateRsaKey();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
  }

  private RSAKey generateRsaKey() {
    KeyPair keyPair = generateKeyPair();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    return new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(UUID.randomUUID().toString())
        .build();
  }

  private KeyPair generateKeyPair() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      log.error("Lỗi nghiêm trọng khi khởi sinh KeyPair RSA", ex);
      throw new IllegalStateException("Không thể sinh khóa mã hóa RSA-2048", ex);
    }
    return keyPair;
  }
}

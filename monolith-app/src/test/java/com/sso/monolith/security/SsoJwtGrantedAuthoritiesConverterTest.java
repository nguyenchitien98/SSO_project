package com.sso.monolith.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Lớp kiểm thử đơn vị (Unit Test) cho {@link SsoJwtGrantedAuthoritiesConverter}.
 *
 * @author SSO Platform Team
 * @since Sprint 06
 */
class SsoJwtGrantedAuthoritiesConverterTest {

  private SsoJwtGrantedAuthoritiesConverter converter;

  @BeforeEach
  void setUp() {
    converter = new SsoJwtGrantedAuthoritiesConverter();
  }

  /** Kiểm thử ánh xạ đầy đủ vai trò (Roles) và quyền cụ thể (Permissions). */
  @Test
  void convert_Success() {
    // Arrange
    Jwt jwt =
        Jwt.withTokenValue("mock-token-value")
            .header("alg", "none")
            .claim("sub", "user-uuid-123")
            .claim("roles", List.of("ADMIN", "support"))
            .claim("permissions", List.of("USER_READ", "PRODUCT_CREATE"))
            .build();

    // Act
    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    // Assert
    assertNotNull(authorities);
    assertEquals(4, authorities.size(), "Phải ánh xạ thành công 2 roles và 2 permissions");

    Set<String> authorityNames =
        authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

    // Kiểm chứng các vai trò
    assertTrue(authorityNames.contains("ROLE_ADMIN"));
    assertTrue(
        authorityNames.contains("ROLE_SUPPORT"), "Vai trò phải được chuẩn hóa thành chữ hoa");

    // Kiểm chứng các quyền cụ thể
    assertTrue(authorityNames.contains("USER_READ"));
    assertTrue(authorityNames.contains("PRODUCT_CREATE"));
  }

  /** Kiểm thử trường hợp JWT không chứa bất kỳ claims roles/permissions nào. */
  @Test
  void convert_NoClaims_ReturnsEmpty() {
    // Arrange
    Jwt jwt =
        Jwt.withTokenValue("mock-token-value")
            .header("alg", "none")
            .claim("sub", "user-uuid-123")
            .build();

    // Act
    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    // Assert
    assertNotNull(authorities);
    assertTrue(
        authorities.isEmpty(),
        "Không có claims thì trả về danh sách rỗng, không ném NullPointerException");
  }
}

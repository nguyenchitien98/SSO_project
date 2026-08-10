package com.sso.monolith.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Lớp chuyển đổi (Converter) ánh xạ từ các Claims trong JWT Token sang Authorities của Spring
 * Security.
 *
 * <p>Tại sao cần lớp này? - SSO Server đóng gói vai trò (`roles`) và quyền cụ thể (`permissions`)
 * vào Access Token. - Converter này phân tách 2 claims này ra và ánh xạ thành đối tượng {@link
 * GrantedAuthority}: - Các vai trò (roles) chuyển đổi thành: `ROLE_{ROLE_NAME}` (Ví dụ:
 * `ROLE_ADMIN`, `ROLE_USER`). - Các quyền hạn (permissions) được giữ nguyên tên (Ví dụ:
 * `USER_READ`, `PRODUCT_CREATE`). - Nhờ vậy, cơ chế kiểm soát phân quyền chéo bằng `@PreAuthorize`
 * ở Monolith App hoạt động chính xác.
 *
 * @author SSO Platform Team
 * @since Sprint 06
 */
@Slf4j
public class SsoJwtGrantedAuthoritiesConverter
    implements Converter<Jwt, Collection<GrantedAuthority>> {

  private static final String ROLES_CLAIM = "roles";
  private static final String PERMISSIONS_CLAIM = "permissions";

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Collection<GrantedAuthority> authorities = new ArrayList<>();

    // 1. Ánh xạ các vai trò (Roles) -> ROLE_{NAME}
    Object rolesClaim = jwt.getClaim(ROLES_CLAIM);
    if (rolesClaim instanceof List<?> rolesList) {
      for (Object role : rolesList) {
        if (role instanceof String roleStr) {
          String authorityName = "ROLE_" + roleStr.trim().toUpperCase();
          authorities.add(new SimpleGrantedAuthority(authorityName));
          log.debug("Mapped Role Claim: {} -> Authority: {}", roleStr, authorityName);
        }
      }
    }

    // 2. Ánh xạ các quyền cụ thể (Permissions)
    Object permissionsClaim = jwt.getClaim(PERMISSIONS_CLAIM);
    if (permissionsClaim instanceof List<?> permissionsList) {
      for (Object permission : permissionsList) {
        if (permission instanceof String permissionStr) {
          String authorityName = permissionStr.trim().toUpperCase();
          authorities.add(new SimpleGrantedAuthority(authorityName));
          log.debug("Mapped Permission Claim: {} -> Authority: {}", permissionStr, authorityName);
        }
      }
    }

    log.info("Tổng số lượng authorities ánh xạ thành công từ JWT: {}", authorities.size());
    return authorities;
  }
}

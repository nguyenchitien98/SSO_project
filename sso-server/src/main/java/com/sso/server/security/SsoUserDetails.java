package com.sso.server.security;

import com.sso.server.entity.User;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Lớp bọc thông tin chi tiết người dùng (UserDetails Wrapper) phục vụ Spring Security.
 *
 * <p>Chuyển đổi thông tin thực thể {@link User} cùng danh sách vai trò (Roles) và quyền hạn
 * (Permissions) thành tập hợp {@link GrantedAuthority} tương thích với Spring Security.
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@Getter
public class SsoUserDetails implements UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Khởi tạo SsoUserDetails từ thực thể User của hệ thống.
     *
     * <p>Logic map quyền hạn:
     * - Mỗi Role map thành authority dạng "ROLE_{NAME}" (ví dụ: ROLE_ADMIN)
     * - Mỗi Permission của Role map thành authority dạng "{NAME}" (ví dụ: PRODUCT_CREATE)
     *
     * @param user Thực thể User cần map
     */
    public SsoUserDetails(User user) {
        this.user = user;
        this.authorities = mapRolesAndPermissionsToAuthorities(user);
    }

    private Collection<? extends GrantedAuthority> mapRolesAndPermissionsToAuthorities(User user) {
        Set<GrantedAuthority> authoritiesSet = new HashSet<>();

        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                // Map Role dạng ROLE_ADMIN, ROLE_USER...
                authoritiesSet.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

                // Map Permissions của Role dạng PRODUCT_READ, ORDER_CREATE...
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(permission -> {
                        authoritiesSet.add(new SimpleGrantedAuthority(permission.getName()));
                    });
                }
            });
        }

        return authoritiesSet;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}

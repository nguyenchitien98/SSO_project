package com.sso.server.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.sso.server.entity.Permission;
import com.sso.server.entity.Role;
import com.sso.server.entity.User;
import com.sso.server.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Lớp kiểm thử đơn vị (Unit Test) cho dịch vụ {@link CustomUserDetailsService}.
 *
 * <p>Kiểm tra tính đúng đắn của việc nạp thông tin user từ repository và chuyển đổi
 * các quyền hạn (Roles/Permissions) thành GrantedAuthority trong Spring Security.
 *
 * @author SSO Platform Team
 * @since Sprint 02
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_Success() {
        // Arrange: Cài đặt dữ liệu giả lập cho User, Role và Permission
        Permission readPermission = Permission.builder()
                .name("PRODUCT_READ")
                .resource("PRODUCT")
                .action("READ")
                .build();

        Role userRole = Role.builder()
                .name("USER")
                .permissions(Set.of(readPermission))
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .passwordHash("hashed_password")
                .enabled(true)
                .locked(false)
                .roles(Set.of(userRole))
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Act: Thực thi phương thức cần test
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert: Xác thực kết quả đầu ra
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonLocked());

        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Kiểm tra xem authorities có chứa cả Role và Permission tương ứng không
        assertTrue(authorities.contains("ROLE_USER"));
        assertTrue(authorities.contains("PRODUCT_READ"));
    }

    @Test
    void loadUserByUsername_NotFound() {
        // Arrange: Giả lập không tìm thấy user
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert: Xác thực ném ngoại lệ UsernameNotFoundException
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("unknown");
        });
    }
}

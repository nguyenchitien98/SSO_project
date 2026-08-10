package com.sso.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.server.dto.request.CreateUserRequest;
import com.sso.server.dto.response.UserResponse;
import com.sso.server.entity.Role;
import com.sso.server.entity.User;
import com.sso.server.repository.RoleRepository;
import com.sso.server.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Lớp kiểm thử đơn vị (Unit Test) cho {@link UserService}.
 *
 * @author SSO Platform Team
 * @since Sprint 03
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private RoleRepository roleRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserService userService;

  private CreateUserRequest createUserRequest;
  private User user;
  private Role userRole;

  @BeforeEach
  void setUp() {
    userRole = Role.builder().id(1L).name("USER").description("Người dùng thông thường").build();

    createUserRequest =
        CreateUserRequest.builder()
            .username("testuser")
            .email("testuser@example.com")
            .password("password123")
            .firstName("Test")
            .lastName("User")
            .roles(Set.of("USER"))
            .build();

    user =
        User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("testuser@example.com")
            .passwordHash("encodedPassword")
            .firstName("Test")
            .lastName("User")
            .roles(Set.of(userRole))
            .enabled(true)
            .locked(false)
            .build();
  }

  /** Kiểm thử trường hợp tạo người dùng mới thành công (Happy Path). */
  @Test
  void createUser_Success() {
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(roleRepository.findByName(anyString())).thenReturn(Optional.of(userRole));
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(user);

    UserResponse response = userService.createUser(createUserRequest);

    assertNotNull(response);
    assertEquals(user.getId(), response.getId());
    assertEquals(user.getUsername(), response.getUsername());
    assertTrue(response.getRoles().contains("USER"));
    verify(userRepository, times(1)).save(any(User.class));
  }

  /** Kiểm thử trường hợp tạo người dùng thất bại do trùng username. */
  @Test
  void createUser_ConflictUsername() {
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> {
              userService.createUser(createUserRequest);
            });

    assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("Tên đăng nhập"));
    verify(userRepository, never()).save(any(User.class));
  }

  /** Kiểm thử trường hợp tạo người dùng thất bại do trùng email. */
  @Test
  void createUser_ConflictEmail() {
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> {
              userService.createUser(createUserRequest);
            });

    assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("Email"));
    verify(userRepository, never()).save(any(User.class));
  }

  /** Kiểm thử trường hợp tạo người dùng thất bại do vai trò không tồn tại. */
  @Test
  void createUser_RoleNotFound() {
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> {
              userService.createUser(createUserRequest);
            });

    assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    assertTrue(exception.getMessage().contains("Không tìm thấy vai trò"));
    verify(userRepository, never()).save(any(User.class));
  }
}

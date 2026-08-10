package com.sso.server.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.sso.server.entity.User;
import com.sso.server.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Lớp kiểm thử đơn vị (Unit Test) cho {@link BruteForceProtectionService}.
 *
 * @author SSO Platform Team
 * @since Sprint 04
 */
@ExtendWith(MockitoExtension.class)
class BruteForceProtectionServiceTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private UserRepository userRepository;

  @Mock private ValueOperations<String, String> valueOperations;

  @InjectMocks private BruteForceProtectionService bruteForceProtectionService;

  private User user;

  @BeforeEach
  void setUp() {
    user =
        User.builder()
            .username("testuser")
            .email("testuser@example.com")
            .passwordHash("hashed")
            .locked(false)
            .build();
  }

  /** Kiểm thử trường hợp đăng nhập thất bại lần đầu: tăng counter Redis. */
  @Test
  void registerLoginFailure_FirstTime() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(anyString())).thenReturn(1L);

    bruteForceProtectionService.registerLoginFailure("testuser");

    verify(valueOperations, times(1)).increment(contains("testuser"));
    verify(redisTemplate, times(1)).expire(anyString(), any());
    verify(userRepository, never()).save(any());
  }

  /** Kiểm thử trường hợp đăng nhập thất bại lần 5: khóa tạm thời trên Redis. */
  @Test
  void registerLoginFailure_TempLock() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(anyString())).thenReturn(5L);

    bruteForceProtectionService.registerLoginFailure("testuser");

    verify(valueOperations, times(1)).set(contains("login:lock:testuser"), eq("LOCKED"), any());
    verify(userRepository, never()).save(any());
  }

  /** Kiểm thử trường hợp đăng nhập thất bại lần 10: khóa vĩnh viễn trong CSDL. */
  @Test
  void registerLoginFailure_PermanentLock() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment(anyString())).thenReturn(10L);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

    bruteForceProtectionService.registerLoginFailure("testuser");

    verify(userRepository, times(1)).save(user);
    assertTrue(user.isLocked());
    assertEquals("BRUTE_FORCE_PERMANENT", user.getLockedReason());
  }

  /** Kiểm thử khi login thành công: xóa toàn bộ keys Redis. */
  @Test
  void registerLoginSuccess() {
    bruteForceProtectionService.registerLoginSuccess("testuser");

    verify(redisTemplate, times(1)).delete("login:attempts:testuser");
    verify(redisTemplate, times(1)).delete("login:lock:testuser");
  }

  /** Kiểm thử check trạng thái block (blocked tạm thời). */
  @Test
  void isBlocked_TempBlocked() {
    when(redisTemplate.hasKey("login:lock:testuser")).thenReturn(true);

    boolean blocked = bruteForceProtectionService.isBlocked("testuser");

    assertTrue(blocked);
    verify(userRepository, never()).findByUsername(anyString());
  }
}

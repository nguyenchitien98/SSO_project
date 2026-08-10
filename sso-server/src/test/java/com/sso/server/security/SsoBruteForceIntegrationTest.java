package com.sso.server.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sso.server.entity.User;
import com.sso.server.repository.UserRepository;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Kiểm thử tích hợp hệ thống (System Integration Test) cho tính năng chống brute force của SSO
 * Server.
 *
 * <p>Kiểm chứng: - Khi nhập sai mật khẩu liên tiếp, bộ đếm trên Redis tăng lên. - Đạt 5 lần nhập
 * sai ➔ Tài khoản bị khóa tạm thời (Temporary Lock) trên Redis, chặn đăng nhập tiếp theo. - Đạt 10
 * lần nhập sai ➔ Tài khoản bị khóa vĩnh viễn (Permanent Lock) trong PostgreSQL DB.
 *
 * @author SSO Platform Team
 * @since Sprint 10
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SsoBruteForceIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockBean private StringRedisTemplate redisTemplate;

  @MockBean private ValueOperations<String, String> valueOperations;

  private final AtomicLong mockRedisAttempts = new AtomicLong(0);
  private boolean isMockRedisLocked = false;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    // Cấu hình mock cho StringRedisTemplate và ValueOperations
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // Giả lập increment trên Redis
    when(valueOperations.increment(anyString()))
        .thenAnswer(
            invocation -> {
              return mockRedisAttempts.incrementAndGet();
            });

    // Giả lập lưu lock key trên Redis
    doAnswer(
            invocation -> {
              isMockRedisLocked = true;
              return null;
            })
        .when(valueOperations)
        .set(contains("login:lock:"), eq("LOCKED"), any());

    // Giả lập kiểm tra lock key trên Redis
    when(redisTemplate.hasKey(contains("login:lock:")))
        .thenAnswer(
            invocation -> {
              return isMockRedisLocked;
            });

    // Đăng ký tài khoản mẫu
    User testUser =
        User.builder()
            .username("brute_force_user")
            .email("brute@sso.com")
            .passwordHash(passwordEncoder.encode("correct-password"))
            .locked(false)
            .build();
    userRepository.save(testUser);

    // Reset state
    mockRedisAttempts.set(0);
    isMockRedisLocked = false;
  }

  /** Test: Đăng nhập sai 5 lần liên tiếp ➔ Redis tạm khóa tài khoản. */
  @Test
  void failedLogins_TriggersTemporaryLock_BlockedOnNextAttempts() throws Exception {
    // 1. Thực hiện 4 lần đăng nhập sai (chưa bị khóa)
    for (int i = 0; i < 4; i++) {
      mockMvc
          .perform(formLogin().user("brute_force_user").password("wrong-password"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/login?error"));
    }
    assertFalse(isMockRedisLocked, "Chưa đủ 5 lần sai, không được khóa");

    // 2. Lần đăng nhập sai thứ 5 ➔ Gây khóa tạm thời trên Redis
    mockMvc
        .perform(formLogin().user("brute_force_user").password("wrong-password"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));

    assertTrue(isMockRedisLocked, "Đạt 5 lần sai, tài khoản phải bị khóa tạm thời");

    // 3. Lần thứ 6: Đăng nhập bằng mật khẩu ĐÚNG vẫn phải bị chặn vì tài khoản đang bị khóa
    mockMvc
        .perform(formLogin().user("brute_force_user").password("correct-password"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
  }

  /** Test: Đăng nhập sai 10 lần liên tiếp ➔ Khóa vĩnh viễn trong DB. */
  @Test
  void failedLogins_ExceedsTenTimes_LocksPermanentlyInDatabase() throws Exception {
    // Giả lập khóa tạm thời trên Redis đã hết hạn (hoặc chưa kích hoạt) để cho phép đếm tiếp tới 10
    // lần
    when(redisTemplate.hasKey(anyString())).thenReturn(false);

    // Thực hiện 10 lần đăng nhập sai
    for (int i = 0; i < 10; i++) {
      mockMvc
          .perform(formLogin().user("brute_force_user").password("wrong-password"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/login?error"));
    }

    // Kiểm tra trạng thái tài khoản trong DB
    User user = userRepository.findByUsername("brute_force_user").orElseThrow();
    assertTrue(user.isLocked(), "Vượt quá 10 lần đăng nhập sai phải khóa vĩnh viễn trong DB");
    assertEquals(
        "BRUTE_FORCE_PERMANENT",
        user.getLockedReason(),
        "Lý do khóa phải là BRUTE_FORCE_PERMANENT");
  }
}

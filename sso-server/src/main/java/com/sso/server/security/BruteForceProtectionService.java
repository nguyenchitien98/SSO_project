package com.sso.server.security;

import com.sso.server.entity.User;
import com.sso.server.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dịch vụ chống tấn công dò mật khẩu (Brute-Force Protection Service).
 *
 * <p>Tại sao sử dụng Redis kết hợp PostgreSQL? - Redis lưu số lần thử sai tạm thời và trạng thái
 * khóa tạm thời (TTL tự giải phóng). - PostgreSQL lưu trạng thái khóa vĩnh viễn (locked = true) khi
 * số lần sai vượt quá giới hạn cực đoan. - Đảm bảo tốc độ kiểm tra O(1) qua Redis cho mỗi lượt
 * login.
 *
 * @author SSO Platform Team
 * @since Sprint 04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BruteForceProtectionService {

  private final StringRedisTemplate redisTemplate;
  private final UserRepository userRepository;

  private static final String ATTEMPTS_PREFIX = "login:attempts:";
  private static final String LOCK_PREFIX = "login:lock:";
  private static final int MAX_TEMPORARY_ATTEMPTS = 5;
  private static final int MAX_PERMANENT_ATTEMPTS = 10;
  private static final Duration TEMP_LOCK_DURATION = Duration.ofMinutes(30);
  private static final Duration ATTEMPTS_TTL = Duration.ofMinutes(5);

  /**
   * Ghi nhận lượt đăng nhập thất bại của người dùng.
   *
   * <p>Logic xử lý: 1. Tăng counter trên Redis với TTL = 5 phút. 2. Nếu đạt 5 lần thất bại ➔ Khóa
   * tạm thời 30 phút trên Redis. 3. Nếu đạt 10 lần thất bại ➔ Khóa vĩnh viễn trong CSDL.
   *
   * @param username Tên đăng nhập của tài khoản
   */
  @Transactional
  public void registerLoginFailure(String username) {
    String attemptsKey = ATTEMPTS_PREFIX + username;
    String lockKey = LOCK_PREFIX + username;

    // Tăng số lần thử trên Redis
    Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
    if (attempts != null && attempts == 1) {
      redisTemplate.expire(attemptsKey, ATTEMPTS_TTL);
    }

    int attemptCount = attempts != null ? attempts.intValue() : 1;
    log.warn("Đăng nhập thất bại lần {} cho tài khoản: {}", attemptCount, username);

    // Khóa tạm thời
    if (attemptCount >= MAX_TEMPORARY_ATTEMPTS && attemptCount < MAX_PERMANENT_ATTEMPTS) {
      log.warn(
          "Tài khoản {} bị khóa tạm thời {} phút do nghi ngờ brute force",
          username,
          TEMP_LOCK_DURATION.toMinutes());
      redisTemplate.opsForValue().set(lockKey, "LOCKED", TEMP_LOCK_DURATION);
    }

    // Khóa vĩnh viễn
    if (attemptCount >= MAX_PERMANENT_ATTEMPTS) {
      log.error(
          "Tài khoản {} vượt quá {} lần đăng nhập sai. Khóa tài khoản vĩnh viễn trong DB",
          username,
          MAX_PERMANENT_ATTEMPTS);
      Optional<User> userOpt = userRepository.findByUsername(username);
      if (userOpt.isPresent()) {
        User user = userOpt.get();
        user.setLocked(true);
        user.setLockedReason("BRUTE_FORCE_PERMANENT");
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
      }
    }
  }

  /**
   * Ghi nhận đăng nhập thành công ➔ Dọn dẹp bộ đếm lỗi trên Redis.
   *
   * @param username Tên đăng nhập
   */
  public void registerLoginSuccess(String username) {
    String attemptsKey = ATTEMPTS_PREFIX + username;
    String lockKey = LOCK_PREFIX + username;

    redisTemplate.delete(attemptsKey);
    redisTemplate.delete(lockKey);
    log.info("Đăng nhập thành công. Đã reset counter brute-force cho tài khoản: {}", username);
  }

  /**
   * Kiểm tra xem tài khoản có đang bị khóa (tạm thời hoặc vĩnh viễn) không.
   *
   * @param username Tên đăng nhập
   * @return {@code true} nếu đang bị khóa, ngược lại {@code false}
   */
  public boolean isBlocked(String username) {
    // 1. Kiểm tra khóa tạm thời trên Redis
    String lockKey = LOCK_PREFIX + username;
    if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
      return true;
    }

    // 2. Kiểm tra khóa vĩnh viễn trên Database
    return userRepository.findByUsername(username).map(User::isLocked).orElse(false);
  }
}

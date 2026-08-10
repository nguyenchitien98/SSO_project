package com.sso.server.controller;

import com.sso.common.dto.ApiResponse;
import com.sso.common.exception.BusinessException;
import com.sso.common.exception.ErrorCode;
import com.sso.server.dto.request.ChangePasswordRequest;
import com.sso.server.dto.response.TotpSetupResponse;
import com.sso.server.entity.User;
import com.sso.server.repository.UserRepository;
import com.sso.server.security.SsoAuthenticationSuccessHandler;
import com.sso.server.security.SsoUserDetails;
import com.sso.server.security.TotpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý luồng giao diện xác thực (Đăng nhập, 2FA OTP) và các API bảo mật tài khoản.
 *
 * <p>Sử dụng cơ chế Thymeleaf cho các màn hình trình duyệt (login, 2fa-login) và REST API cho các
 * tác vụ thay đổi cấu hình bảo mật tài khoản.
 *
 * @author SSO Platform Team
 * @since Sprint 04
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RequestCache requestCache = new HttpSessionRequestCache();

  /** Hiển thị trang đăng nhập tùy chỉnh. */
  @GetMapping("/login")
  public String loginPage() {
    return "login";
  }

  /** Hiển thị trang xác thực OTP 2 lớp. */
  @GetMapping("/login/2fa")
  public String totpLoginPage(HttpSession session, Model model) {
    Authentication tempAuth =
        (Authentication)
            session.getAttribute(SsoAuthenticationSuccessHandler.TEMP_AUTH_SESSION_KEY);
    if (tempAuth == null) {
      log.warn(
          "Truy cập trang /login/2fa bất hợp pháp - Không tìm thấy thông tin TEMP_AUTH trong session");
      return "redirect:/login";
    }
    return "2fa-login";
  }

  /**
   * Xử lý xác thực mã OTP 2 lớp từ người dùng gửi lên.
   *
   * <p>Nếu mã OTP chính xác ➔ khôi phục Authentication vào SecurityContext và chuyển hướng về URL
   * ban đầu được lưu trong requestCache.
   */
  @PostMapping("/login/2fa")
  public String processTotpLogin(
      @RequestParam("code") String code,
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {

    Authentication tempAuth =
        (Authentication)
            session.getAttribute(SsoAuthenticationSuccessHandler.TEMP_AUTH_SESSION_KEY);
    if (tempAuth == null) {
      return "redirect:/login";
    }

    SsoUserDetails userDetails = (SsoUserDetails) tempAuth.getPrincipal();
    User user = userDetails.getUser();

    log.info("Xử lý xác thực OTP cho tài khoản: {}", user.getUsername());

    // Kiểm tra mã OTP
    if (TotpUtils.verifyOtp(user.getTotpSecret(), code)) {
      log.info("Xác thực OTP thành công cho user: {}", user.getUsername());

      // 1. Khôi phục authentication hoàn chỉnh vào SecurityContext
      SecurityContextHolder.getContext().setAuthentication(tempAuth);

      // 2. Xóa thông tin tạm trong session
      session.removeAttribute(SsoAuthenticationSuccessHandler.TEMP_AUTH_SESSION_KEY);

      // 3. Chuyển hướng về SavedRequest ban đầu
      SavedRequest savedRequest = requestCache.getRequest(request, response);
      String targetUrl = savedRequest != null ? savedRequest.getRedirectUrl() : "/";
      log.info("Định tuyến người dùng về địa chỉ ban đầu: {}", targetUrl);
      return "redirect:" + targetUrl;
    } else {
      log.warn("Xác thực OTP thất bại cho user: {}", user.getUsername());
      model.addAttribute("error", true);
      return "2fa-login";
    }
  }

  /** API REST thay đổi mật khẩu tài khoản người dùng hiện tại. */
  @PostMapping("/auth/change-password")
  @ResponseBody
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @AuthenticationPrincipal SsoUserDetails userDetails,
      @Valid @RequestBody ChangePasswordRequest request) {

    User user = userDetails.getUser();
    log.info(
        "API POST /auth/change-password - Yêu cầu đổi mật khẩu cho user: {}", user.getUsername());

    if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
      log.warn(
          "Đổi mật khẩu thất bại: Mật khẩu cũ không chính xác cho user: {}", user.getUsername());
      throw new BusinessException(ErrorCode.INVALID_INPUT, "Mật khẩu cũ không chính xác");
    }

    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    user.setUpdatedAt(Instant.now());
    userRepository.save(user);

    log.info("Đổi mật khẩu thành công cho user: {}", user.getUsername());
    return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
  }

  /** API REST sinh mã bí mật 2FA và URL QR Code để người dùng thiết lập. */
  @PostMapping("/auth/2fa/setup")
  @ResponseBody
  public ResponseEntity<ApiResponse<TotpSetupResponse>> setupTotp(
      @AuthenticationPrincipal SsoUserDetails userDetails, HttpSession session) {

    User user = userDetails.getUser();
    log.info("API POST /auth/2fa/setup - Khởi tạo thiết lập 2FA cho user: {}", user.getUsername());

    // Sinh secret key mới tạm thời và lưu vào session để xác nhận trước khi lưu DB
    String tempSecret = TotpUtils.generateSecretKey();
    session.setAttribute("TEMP_TOTP_SECRET", tempSecret);

    String qrCodeUrl = TotpUtils.getQrCodeUrl(tempSecret, user.getUsername());

    TotpSetupResponse response =
        TotpSetupResponse.builder().secretKey(tempSecret).qrCodeUrl(qrCodeUrl).build();

    return ResponseEntity.ok(ApiResponse.success("Khởi tạo cấu hình 2FA thành công", response));
  }

  /** API REST xác nhận OTP để kích hoạt chính thức bảo mật 2 lớp. */
  @PostMapping("/auth/2fa/verify")
  @ResponseBody
  public ResponseEntity<ApiResponse<Void>> verifyAndEnableTotp(
      @AuthenticationPrincipal SsoUserDetails userDetails,
      @RequestBody Map<String, String> payload,
      HttpSession session) {

    User user = userDetails.getUser();
    String code = payload.get("code");
    log.info(
        "API POST /auth/2fa/verify - Yêu cầu xác nhận mã OTP kích hoạt 2FA cho user: {}",
        user.getUsername());

    String tempSecret = (String) session.getAttribute("TEMP_TOTP_SECRET");
    if (tempSecret == null) {
      log.warn("Kích hoạt 2FA thất bại: Không tìm thấy TEMP_TOTP_SECRET trong session");
      throw new BusinessException(
          ErrorCode.INVALID_INPUT, "Yêu cầu thiết lập 2FA trước khi xác nhận");
    }

    if (TotpUtils.verifyOtp(tempSecret, code)) {
      // Lưu chính thức vào Database
      User dbUser =
          userRepository
              .findById(user.getId())
              .orElseThrow(
                  () -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng"));

      dbUser.setTotpSecret(tempSecret);
      dbUser.setTotpEnabled(true);
      dbUser.setUpdatedAt(Instant.now());
      userRepository.save(dbUser);

      // Cập nhật session user details hiện tại
      user.setTotpSecret(tempSecret);
      user.setTotpEnabled(true);

      session.removeAttribute("TEMP_TOTP_SECRET");
      log.info("Kích hoạt bảo mật 2 lớp thành công cho user: {}", user.getUsername());
      return ResponseEntity.ok(ApiResponse.success("Kích hoạt bảo mật 2 lớp thành công", null));
    } else {
      log.warn("Xác thực OTP kích hoạt 2FA thất bại cho user: {}", user.getUsername());
      throw new BusinessException(ErrorCode.INVALID_INPUT, "Mã xác thực OTP không chính xác");
    }
  }
}

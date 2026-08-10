package com.sso.monolith.repository;

import com.sso.monolith.entity.UserProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository xử lý các truy vấn cơ sở dữ liệu cho thực thể {@link UserProfile}.
 *
 * @author SSO Platform Team
 * @since Sprint 06
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {}

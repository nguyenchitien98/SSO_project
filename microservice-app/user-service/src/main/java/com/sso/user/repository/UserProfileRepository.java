package com.sso.user.repository;

import com.sso.user.entity.UserProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository truy xuất dữ liệu UserProfile.
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {}

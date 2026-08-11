package com.sso.file.repository;

import com.sso.file.entity.FileMetadata;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository quản lý thực thể FileMetadata.
 *
 * @author SSO Platform Team
 * @since Sprint 13
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
  List<FileMetadata> findAllByUserId(UUID userId);
}

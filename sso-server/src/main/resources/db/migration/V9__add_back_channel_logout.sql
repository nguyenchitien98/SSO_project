-- V9: Thêm trường cấu hình Back-Channel Logout URI cho OAuth2 Clients
-- Hỗ trợ cơ chế Single Logout (SLO): Khi user logout từ SSO Server, SSO Server tự động gửi tín hiệu
-- invalidate session chéo sang các ứng dụng client được cấu hình Back-Channel Logout.

ALTER TABLE oauth_clients ADD COLUMN back_channel_logout_uri VARCHAR(255);

-- V10: Cập nhật redirect_uris cho client monolith-web để cho phép monolith-frontend (ReactJS port 3000) đăng nhập
UPDATE oauth_clients
SET redirect_uris = 'http://localhost:8080/login/oauth2/code/sso,http://localhost:3000/callback'
WHERE id = 'monolith-web';

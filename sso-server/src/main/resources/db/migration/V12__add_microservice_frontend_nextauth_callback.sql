-- V12: Cập nhật redirect_uris cho client microservice-gateway để cho phép microservice-frontend (Next.js port 3001) đăng nhập qua NextAuth callback
UPDATE oauth_clients
SET redirect_uris = 'http://localhost:3001/callback,http://localhost:3001/api/auth/callback/sso-server'
WHERE id = 'microservice-gateway';

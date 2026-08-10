import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/useAuth';
import styles from './LoginPage.module.css';

export const LoginPage: React.FC = () => {
  const { login, isAuthenticated, isLoading } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const handleLoginClick = async () => {
    try {
      await login();
    } catch (e) {
      console.error('Redirection to SSO Server failed', e);
      alert('Không thể kết nối đến máy chủ SSO. Vui lòng thử lại sau.');
    }
  };

  if (isLoading) {
    return (
      <div className={styles.container}>
        <div style={{ color: 'var(--text-secondary)' }}>Đang tải cấu hình...</div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.logo}>SSO Platform</h1>
        <p className={styles.subtitle}>Hệ thống Đăng nhập Tập trung</p>

        <div className={styles.divider}></div>

        <p className={styles.description}>
          Hệ thống Monolith App này sử dụng luồng xác thực an toàn OAuth2 với PKCE. 
          Bấm nút bên dưới để chuyển hướng sang SSO Server đăng nhập.
        </p>

        <button className={styles.loginBtn} onClick={handleLoginClick}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
            <polyline points="10 17 15 12 10 7" />
            <line x1="15" y1="12" x2="3" y2="12" />
          </svg>
          Đăng nhập với SSO
        </button>
      </div>
    </div>
  );
};

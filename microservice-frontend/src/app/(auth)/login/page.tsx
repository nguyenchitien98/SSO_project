'use client';

import React, { useState, Suspense } from 'react';
import { signIn } from 'next-auth/react';
import { useSearchParams } from 'next/navigation';
import styles from './Login.module.css';

function LoginContent() {
  const searchParams = useSearchParams();
  const callbackUrl = searchParams.get('callbackUrl') || '/';
  const error = searchParams.get('error');

  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async () => {
    setIsLoading(true);
    try {
      await signIn('sso-server', { callbackUrl });
    } catch (e) {
      console.error(e);
      setIsLoading(false);
    }
  };

  return (
    <div className={styles.card}>
      <div className={styles.logoBadge}>MS</div>
      <h1 className={styles.title}>SSO Platform</h1>
      <p className={styles.subtitle}>Microservice App Portal</p>
      
      <p className={styles.description}>
        Hệ thống Quản lý phân tán bao gồm 5 Microservices được xác thực và bảo mật qua API Gateway sử dụng phân quyền JWT.
      </p>

      {error && (
        <div className={styles.errorBox}>
          ⚠️ Đăng nhập thất bại: {error === 'OAuthSignin' ? 'Lỗi bắt đầu bắt tay SSO' : 'Vui lòng kiểm tra lại cấu hình SSO'}
        </div>
      )}

      <button
        className={styles.loginBtn}
        onClick={handleLogin}
        disabled={isLoading}
      >
        {isLoading ? (
          <span className={styles.loader}>Đang chuyển hướng...</span>
        ) : (
          'Đăng nhập với SSO Server'
        )}
      </button>

      <div className={styles.footer}>
        <span>SSO Platform Security Model • Spring Authorization Server v1.3</span>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <div className={styles.container}>
      <Suspense fallback={
        <div className={styles.card}>
          <p style={{ color: 'var(--text-secondary)' }}>Đang tải cấu hình xác thực...</p>
        </div>
      }>
        <LoginContent />
      </Suspense>
    </div>
  );
}

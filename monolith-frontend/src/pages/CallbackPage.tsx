import React, { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/useAuth';

export const CallbackPage: React.FC = () => {
  const { handleCallback } = useAuth();
  const [searchParams] = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const processed = useRef(false);

  useEffect(() => {
    // Avoid double trigger in React 18 Strict Mode
    if (processed.current) return;
    processed.current = true;

    const code = searchParams.get('code');
    const state = searchParams.get('state');

    if (!code || !state) {
      setError('Thiếu tham số code hoặc state từ SSO Server.');
      return;
    }

    const processAuth = async () => {
      try {
        await handleCallback(code, state);
        // Login success, redirect to dashboard
        navigate('/', { replace: true });
      } catch (err: any) {
        console.error('Callback handling failed', err);
        setError(err.message || 'Có lỗi xảy ra trong quá trình trao đổi Token.');
      }
    };

    processAuth();
  }, [handleCallback, navigate, searchParams]);

  if (error) {
    return (
      <div style={containerStyle}>
        <div style={cardStyle}>
          <div style={errorIconStyle}>✕</div>
          <h2 style={titleStyle}>Lỗi Xác Thực</h2>
          <p style={errorTextStyle}>{error}</p>
          <button style={btnStyle} onClick={() => navigate('/login', { replace: true })}>
            Quay lại Đăng nhập
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={containerStyle}>
      <div style={cardStyle}>
        <div style={spinnerStyle}></div>
        <h2 style={titleStyle}>Đang xử lý đăng nhập</h2>
        <p style={descStyle}>Hệ thống đang tiến hành trao đổi mã bảo mật và thiết lập phiên làm việc...</p>
      </div>
    </div>
  );
};

const containerStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  minHeight: '100vh',
  backgroundColor: '#09090b',
  color: '#fafafa',
  fontFamily: 'Inter, sans-serif',
  padding: '16px',
};

const cardStyle: React.CSSProperties = {
  width: '100%',
  maxWidth: '440px',
  backgroundColor: '#18181b',
  border: '1px solid #27272a',
  borderRadius: '16px',
  padding: '40px 32px',
  textAlign: 'center',
  boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.7)',
};

const titleStyle: React.CSSProperties = {
  fontSize: '1.25rem',
  fontWeight: 700,
  marginBottom: '12px',
};

const descStyle: React.CSSProperties = {
  fontSize: '0.875rem',
  color: '#a1a1aa',
  lineHeight: 1.6,
};

const errorTextStyle: React.CSSProperties = {
  fontSize: '0.875rem',
  color: '#ef4444',
  marginBottom: '24px',
  lineHeight: 1.6,
};

const errorIconStyle: React.CSSProperties = {
  width: '48px',
  height: '48px',
  borderRadius: '50%',
  backgroundColor: 'rgba(239, 68, 68, 0.1)',
  color: '#ef4444',
  fontSize: '20px',
  fontWeight: 'bold',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  margin: '0 auto 20px auto',
  border: '1px solid rgba(239, 68, 68, 0.2)',
};

const spinnerStyle: React.CSSProperties = {
  width: '32px',
  height: '32px',
  border: '3px solid rgba(99, 102, 241, 0.1)',
  borderTop: '3px solid #6366f1',
  borderRadius: '50%',
  animation: 'spin 1s linear infinite',
  margin: '0 auto 20px auto',
};

const btnStyle: React.CSSProperties = {
  backgroundColor: '#6366f1',
  color: 'white',
  border: 'none',
  padding: '10px 20px',
  borderRadius: '8px',
  fontSize: '0.875rem',
  fontWeight: 600,
  cursor: 'pointer',
};

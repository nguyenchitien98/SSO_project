'use client';

import React, { useState } from 'react';
import { useSession } from 'next-auth/react';
import styles from '../products/Products.module.css';

export default function ProfilePage() {
  const { data: session, update } = useSession();
  const user = session?.user;

  // Local states
  const [avatarUrl, setAvatarUrl] = useState(user?.image || 'https://api.dicebear.com/7.x/bottts/svg?seed=sso-platform');
  const [isUploading, setIsUploading] = useState(false);

  // 2FA Setup states
  const [is2faSetupOpen, setIs2faSetupOpen] = useState(false);
  const [totpData, setTotpData] = useState<{ secretKey: string; qrCodeUrl: string } | null>(null);
  const [otpCode, setOtpCode] = useState('');
  const [isActivating, setIsActivating] = useState(false);
  const [is2faEnabled, setIs2faEnabled] = useState(false);
  const [setupError, setSetupError] = useState<string | null>(null);

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsUploading(true);
    const formData = new FormData();
    formData.append('file', file);

    try {
      const res = await fetch('/api/mock/files/upload', {
        method: 'POST',
        body: formData,
      });

      if (!res.ok) {
        throw new Error('Lỗi upload file lên MinIO.');
      }

      const payload = await res.json();
      const uploadedUrl = payload.data.fileUrl;
      setAvatarUrl(uploadedUrl);

      // Trigger NextAuth session update to save new image url
      await update({
        ...session,
        user: {
          ...user,
          image: uploadedUrl,
        },
      });

      alert('Đã cập nhật ảnh đại diện thành công!');
    } catch (err: any) {
      console.error(err);
      alert(err.message || 'Lỗi tải lên ảnh.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleStart2faSetup = async () => {
    setSetupError(null);
    try {
      const res = await fetch('/api/mock/2fa/setup', {
        method: 'POST',
      });
      if (!res.ok) throw new Error('Không thể khởi tạo cấu hình 2FA.');
      const payload = await res.json();
      setTotpData(payload.data);
      setIs2faSetupOpen(true);
    } catch (err: any) {
      console.error(err);
      alert(err.message || 'Lỗi kết nối 2FA.');
    }
  };

  const handleVerify2fa = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!otpCode || otpCode.length !== 6) {
      setSetupError('Mã OTP phải có độ dài đúng 6 chữ số.');
      return;
    }

    setSetupError(null);
    setIsActivating(true);

    try {
      const res = await fetch('/api/mock/2fa/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: otpCode }),
      });

      if (!res.ok) {
        const payload = await res.json().catch(() => ({}));
        throw new Error(payload.message || 'Mã xác thực OTP không chính xác.');
      }

      setIs2faEnabled(true);
      setIs2faSetupOpen(false);
      setOtpCode('');
      alert('Đã kích hoạt bảo mật 2 lớp (TOTP/2FA) thành công!');
    } catch (err: any) {
      console.error(err);
      setSetupError(err.message || 'Lỗi xác thực mã OTP.');
    } finally {
      setIsActivating(false);
    }
  };

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Hồ sơ cá nhân</h1>
          <p className={styles.subtitle}>
            Quản lý thông tin định danh, ảnh đại diện MinIO và thiết lập bảo mật 2 lớp (2FA/TOTP).
          </p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '30px' }}>
        {/* Profile Card & Avatar */}
        <div className={styles.detailCard} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
          <div style={{ position: 'relative', marginBottom: '20px' }}>
            <img
              src={avatarUrl}
              alt="Avatar"
              style={{ width: '120px', height: '120px', borderRadius: '50%', border: '3px solid var(--color-brand)', objectFit: 'cover', backgroundColor: 'var(--bg-primary)' }}
            />
            <label
              htmlFor="avatar-upload"
              style={{ position: 'absolute', bottom: 0, right: 0, backgroundColor: 'var(--color-brand)', padding: '6px', borderRadius: '50%', cursor: 'pointer', display: 'flex', border: '2px solid var(--bg-surface)' }}
              title="Tải ảnh mới lên MinIO"
            >
              📷
              <input
                id="avatar-upload"
                type="file"
                accept="image/*"
                onChange={handleAvatarChange}
                disabled={isUploading}
                style={{ display: 'none' }}
              />
            </label>
          </div>
          
          <h2 style={{ fontSize: '18px', fontWeight: 700 }}>{user?.name || user?.email}</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>{user?.email}</p>

          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', justifyContent: 'center', marginBottom: '24px' }}>
            {(user?.roles || []).map((r) => (
              <span key={r} className={styles.categoryBadge} style={{ color: 'var(--color-brand)', borderColor: 'var(--color-brand)' }}>
                {r}
              </span>
            ))}
          </div>

          <div style={{ width: '100%', borderTop: '1px solid var(--border-color)', paddingTop: '20px', textAlign: 'left' }}>
            <div style={{ marginBottom: '14px' }}>
              <strong style={{ fontSize: '13px', color: 'var(--text-muted)' }}>MÃ ĐỊNH DANH (SUB)</strong>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', wordBreak: 'break-all', marginTop: '4px' }}>{user?.id}</div>
            </div>
          </div>
        </div>

        {/* Security Settings Card */}
        <div className={styles.detailCard}>
          <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '12px' }}>Bảo mật tài khoản</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '24px' }}>
            Kích hoạt các phương thức xác thực nâng cao để bảo vệ tài khoản khỏi các đòn tấn công brute-force.
          </p>

          <div style={{ border: '1px solid var(--border-color)', borderRadius: '8px', padding: '20px', backgroundColor: 'rgba(255,255,255,0.01)', display: 'flex', flexDirection: 'column', gap: '15px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <h4 style={{ fontSize: '14px', fontWeight: 600 }}>Xác thực 2 lớp (TOTP / 2FA)</h4>
                <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px' }}>
                  Yêu cầu nhập mã OTP sinh ra từ ứng dụng Authenticator khi đăng nhập.
                </p>
              </div>
              <span className={styles.stockStatus} style={{ color: is2faEnabled ? 'var(--color-success)' : 'var(--text-muted)' }}>
                {is2faEnabled ? 'Đã bật' : 'Chưa kích hoạt'}
              </span>
            </div>

            {!is2faEnabled ? (
              <button className={styles.btnPrimary} onClick={handleStart2faSetup} style={{ width: 'fit-content' }}>
                Cấu hình 2FA
              </button>
            ) : (
              <button className={styles.btnSecondary} onClick={() => setIs2faEnabled(false)} style={{ width: 'fit-content' }}>
                Hủy cấu hình 2FA
              </button>
            )}
          </div>
        </div>
      </div>

      {/* 2FA Setup Modal */}
      {is2faSetupOpen && totpData && (
        <div style={modalOverlayStyle}>
          <div style={modalStyle}>
            <h3 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '12px' }}>Thiết lập bảo mật 2 lớp</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
              Quét mã QR dưới đây bằng ứng dụng Google Authenticator hoặc Microsoft Authenticator, sau đó nhập mã OTP xác nhận.
            </p>

            <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '20px', padding: '10px', backgroundColor: '#fff', borderRadius: '8px', width: 'fit-content', margin: '0 auto 20px' }}>
              <img src={totpData.qrCodeUrl} alt="2FA QR Code" style={{ width: '200px', height: '200px' }} />
            </div>

            <div style={{ marginBottom: '20px', textAlign: 'center' }}>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>MÃ BÍ MẬT (BASE32 KEY)</span>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: '14px', fontWeight: 600, color: 'var(--color-brand)', marginTop: '4px', letterSpacing: '1px' }}>
                {totpData.secretKey}
              </div>
            </div>

            {setupError && (
              <div className={styles.errorBox} style={{ padding: '8px 12px', fontSize: '12px', marginBottom: '15px' }}>
                {setupError}
              </div>
            )}

            <form onSubmit={handleVerify2fa}>
              <div className={styles.formGroup} style={{ marginBottom: '20px' }}>
                <label className={styles.formLabel} htmlFor="otp">
                  Nhập mã OTP 6 chữ số *
                </label>
                <input
                  id="otp"
                  type="text"
                  required
                  maxLength={6}
                  pattern="\d{6}"
                  className={styles.formInput}
                  value={otpCode}
                  onChange={e => setOtpCode(e.target.value)}
                  placeholder="Ví dụ: 123456"
                  style={{ textAlign: 'center', fontSize: '18px', letterSpacing: '4px', fontWeight: 700 }}
                  disabled={isActivating}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <button
                  type="button"
                  className={styles.btnSecondary}
                  onClick={() => {
                    setIs2faSetupOpen(false);
                    setOtpCode('');
                  }}
                  disabled={isActivating}
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className={styles.btnPrimary}
                  disabled={isActivating}
                >
                  {isActivating ? 'Đang kích hoạt...' : 'Kích hoạt 2FA'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

const modalOverlayStyle: React.CSSProperties = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: 'rgba(0, 0, 0, 0.75)',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  zIndex: 100,
};

const modalStyle: React.CSSProperties = {
  backgroundColor: '#18181b',
  border: '1px solid #27272a',
  borderRadius: '12px',
  width: '100%',
  maxWidth: '440px',
  padding: '24px',
  boxShadow: '0 20px 25px -5px rgb(0 0 0 / 0.5)',
};

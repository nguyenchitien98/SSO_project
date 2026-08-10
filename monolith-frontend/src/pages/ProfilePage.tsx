import React, { useEffect, useState } from 'react';
import { useAuth } from '@/auth/useAuth';
import { userApi } from '@/services/userApi';
import { UserProfile } from '@/types/user';
import styles from './products/Products.module.css';

export const ProfilePage: React.FC = () => {
  const { user } = useAuth();
  
  // Profile state
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [profileForm, setProfileForm] = useState({
    displayName: '',
    phone: '',
    address: '',
  });
  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [profileSuccessMsg, setProfileSuccessMsg] = useState<string | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);

  // Password state
  const [passwordForm, setPasswordForm] = useState({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [pwSuccessMsg, setPwSuccessMsg] = useState<string | null>(null);
  const [pwErrorMsg, setPwErrorMsg] = useState<string | null>(null);

  const fetchProfile = async () => {
    setIsLoadingProfile(true);
    try {
      const data = await userApi.getMyProfile();
      setProfile(data);
      setProfileForm({
        displayName: data.displayName || '',
        phone: data.phone || '',
        address: data.address || '',
      });
    } catch (e: any) {
      console.error(e);
      setProfileError('Không thể tải thông tin hồ sơ cục bộ.');
    } finally {
      setIsLoadingProfile(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const handleProfileChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setProfileForm(prev => ({ ...prev, [name]: value }));
  };

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileSuccessMsg(null);
    setProfileError(null);
    try {
      const updated = await userApi.updateMyProfile(profileForm);
      setProfile(updated);
      setProfileSuccessMsg('Cập nhật hồ sơ cá nhân thành công!');
    } catch (e: any) {
      console.error(e);
      setProfileError(e.message || 'Lỗi cập nhật hồ sơ cá nhân.');
    }
  };

  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setPasswordForm(prev => ({ ...prev, [name]: value }));
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPwSuccessMsg(null);
    setPwErrorMsg(null);

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPwErrorMsg('Mật khẩu mới xác nhận không trùng khớp.');
      return;
    }

    if (passwordForm.newPassword.length < 6) {
      setPwErrorMsg('Mật khẩu mới phải từ 6 ký tự trở lên.');
      return;
    }

    setIsChangingPassword(true);
    try {
      await userApi.changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      });
      setPwSuccessMsg('Đổi mật khẩu thành công!');
      setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
    } catch (e: any) {
      console.error(e);
      setPwErrorMsg(e.message || 'Mật khẩu cũ không chính xác hoặc lỗi hệ thống.');
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div>
      <h1 className={styles.title} style={{ marginBottom: '5px' }}>Thông tin tài khoản</h1>
      <p style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '30px' }}>
        Quản lý thông tin cá nhân và mật khẩu xác thực liên kết SSO
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '30px' }}>
        {/* Left column: Local Monolith Profile */}
        <div>
          <div className={styles.formCard} style={{ maxWidth: '100%' }}>
            <h2 style={{ fontSize: '18px', fontWeight: 600, marginBottom: '20px', color: 'var(--text-primary)' }}>
              Hồ sơ cá nhân cục bộ {profile && profile.createdAt && `(Tạo từ: ${new Date(profile.createdAt).toLocaleDateString('vi-VN')})`}
            </h2>

            {isLoadingProfile ? (
              <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>Đang tải thông tin hồ sơ...</p>
            ) : (
              <form onSubmit={handleProfileSubmit}>
                {profileSuccessMsg && (
                  <div style={successBoxStyle}>{profileSuccessMsg}</div>
                )}
                {profileError && (
                  <div style={errorBoxStyle}>{profileError}</div>
                )}

                <div className={styles.formGroup}>
                  <label className={styles.formLabel} htmlFor="displayName">Tên hiển thị *</label>
                  <input
                    id="displayName"
                    name="displayName"
                    type="text"
                    required
                    className={styles.formInput}
                    value={profileForm.displayName}
                    onChange={handleProfileChange}
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.formLabel} htmlFor="phone">Số điện thoại</label>
                  <input
                    id="phone"
                    name="phone"
                    type="text"
                    className={styles.formInput}
                    value={profileForm.phone}
                    onChange={handleProfileChange}
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.formLabel} htmlFor="address">Địa chỉ liên hệ</label>
                  <textarea
                    id="address"
                    name="address"
                    rows={3}
                    style={{ fontFamily: 'inherit', resize: 'vertical' }}
                    className={styles.formInput}
                    value={profileForm.address}
                    onChange={handleProfileChange}
                  />
                </div>

                <button type="submit" className={styles.btnPrimary} style={{ width: '100%', justifyContent: 'center' }}>
                  Lưu hồ sơ cá nhân
                </button>
              </form>
            )}
          </div>
        </div>

        {/* Right column: SSO details & Change Password */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '30px' }}>
          {/* SSO Details Card */}
          <div className={styles.formCard} style={{ maxWidth: '100%' }}>
            <h2 style={{ fontSize: '18px', fontWeight: 600, marginBottom: '15px', color: 'var(--text-primary)' }}>
              Danh tính SSO Server
            </h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '14px' }}>
              <div style={rowStyle}>
                <span style={lblStyle}>Mã định danh (sub):</span>
                <span style={valStyle}>{user?.sub}</span>
              </div>
              <div style={rowStyle}>
                <span style={lblStyle}>Email tài khoản:</span>
                <span style={valStyle}>{user?.email}</span>
              </div>
              <div style={rowStyle}>
                <span style={lblStyle}>Họ và tên:</span>
                <span style={valStyle}>{user?.name}</span>
              </div>
            </div>
          </div>

          {/* Change Password Card */}
          <div className={styles.formCard} style={{ maxWidth: '100%' }}>
            <h2 style={{ fontSize: '18px', fontWeight: 600, marginBottom: '20px', color: 'var(--text-primary)' }}>
              Đổi mật khẩu liên kết
            </h2>

            <form onSubmit={handlePasswordSubmit}>
              {pwSuccessMsg && <div style={successBoxStyle}>{pwSuccessMsg}</div>}
              {pwErrorMsg && <div style={errorBoxStyle}>{pwErrorMsg}</div>}

              <div className={styles.formGroup}>
                <label className={styles.formLabel} htmlFor="oldPassword">Mật khẩu hiện tại *</label>
                <input
                  id="oldPassword"
                  name="oldPassword"
                  type="password"
                  required
                  className={styles.formInput}
                  value={passwordForm.oldPassword}
                  onChange={handlePasswordChange}
                  disabled={isChangingPassword}
                />
              </div>

              <div className={styles.formGroup}>
                <label className={styles.formLabel} htmlFor="newPassword">Mật khẩu mới *</label>
                <input
                  id="newPassword"
                  name="newPassword"
                  type="password"
                  required
                  className={styles.formInput}
                  value={passwordForm.newPassword}
                  onChange={handlePasswordChange}
                  disabled={isChangingPassword}
                />
              </div>

              <div className={styles.formGroup}>
                <label className={styles.formLabel} htmlFor="confirmPassword">Xác nhận mật khẩu mới *</label>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  required
                  className={styles.formInput}
                  value={passwordForm.confirmPassword}
                  onChange={handlePasswordChange}
                  disabled={isChangingPassword}
                />
              </div>

              <button
                type="submit"
                className={styles.btnPrimary}
                style={{ width: '100%', justifyContent: 'center' }}
                disabled={isChangingPassword}
              >
                {isChangingPassword ? 'Đang đổi mật khẩu...' : 'Xác nhận Đổi mật khẩu'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

const successBoxStyle: React.CSSProperties = {
  color: 'var(--color-success)',
  backgroundColor: 'rgba(34, 197, 94, 0.1)',
  padding: '10px 14px',
  borderRadius: '8px',
  marginBottom: '20px',
  border: '1px solid rgba(34, 197, 94, 0.2)',
  fontSize: '13px',
  fontWeight: 500,
};

const errorBoxStyle: React.CSSProperties = {
  color: 'var(--color-error)',
  backgroundColor: 'rgba(239, 68, 68, 0.1)',
  padding: '10px 14px',
  borderRadius: '8px',
  marginBottom: '20px',
  border: '1px solid rgba(239, 68, 68, 0.2)',
  fontSize: '13px',
  fontWeight: 500,
};

const rowStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '3px',
  paddingBottom: '8px',
  borderBottom: '1px solid var(--border-color)',
};

const lblStyle: React.CSSProperties = {
  color: 'var(--text-muted)',
  fontSize: '12px',
};

const valStyle: React.CSSProperties = {
  color: 'var(--text-primary)',
  fontWeight: 600,
  fontFamily: 'var(--font-sans)',
};

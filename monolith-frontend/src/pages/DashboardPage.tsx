import React from 'react';
import { useAuth } from '@/auth/useAuth';
import styles from './DashboardPage.module.css';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div>
      <h1 className={styles.title}>Tổng quan hệ thống</h1>
      <p className={styles.welcomeText}>
        Chào mừng quay trở lại, <strong>{user?.name || user?.email}</strong>!
      </p>

      {/* Quick Stats Grid */}
      <div className={styles.grid}>
        <div className={styles.statCard}>
          <span className={styles.statLabel}>Sản phẩm hệ thống</span>
          <span className={styles.statValue}>15</span>
          <span className={styles.statDesc}>PRODUCT_READ enabled</span>
        </div>

        <div className={styles.statCard}>
          <span className={styles.statLabel}>Đơn hàng đã đặt</span>
          <span className={styles.statValue}>8</span>
          <span className={styles.statDesc}>ORDER_READ enabled</span>
        </div>

        <div className={styles.statCard}>
          <span className={styles.statLabel}>Vai trò hiện tại</span>
          <span className={styles.statValue} style={{ color: 'var(--color-brand)' }}>
            {user?.roles?.[0] || 'USER'}
          </span>
          <span className={styles.statDesc}>Đồng bộ từ SSO Server</span>
        </div>

        <div className={styles.statCard}>
          <span className={styles.statLabel}>Kết nối SSO</span>
          <span className={styles.statValue} style={{ color: 'var(--color-success)' }}>ONLINE</span>
          <span className={styles.statDesc}>OIDC Session Active</span>
        </div>
      </div>

      {/* SSO Session Details */}
      <div className={styles.infoCard}>
        <h2 className={styles.cardTitle}>Thông tin phiên làm việc chéo (SSO Session)</h2>
        <div className={styles.detailsList}>
          <div className={styles.detailItem}>
            <span className={styles.detailLabel}>User UUID (sub)</span>
            <span className={styles.detailValue}>{user?.sub}</span>
          </div>

          <div className={styles.detailItem}>
            <span className={styles.detailLabel}>Email liên kết</span>
            <span className={styles.detailValue}>{user?.email}</span>
          </div>

          <div className={styles.detailItem}>
            <span className={styles.detailLabel}>Vai trò (Roles)</span>
            <div className={styles.badgeList}>
              {user?.roles?.map((role) => (
                <span key={role} className={styles.badge} style={{ color: 'var(--color-success)', borderColor: 'rgba(34, 197, 94, 0.3)' }}>
                  {role}
                </span>
              ))}
            </div>
          </div>

          <div className={styles.detailItem}>
            <span className={styles.detailLabel}>Quyền hạn giải mã (Permissions)</span>
            <div className={styles.badgeList}>
              {user?.permissions?.map((permission) => (
                <span key={permission} className={styles.badge}>
                  {permission}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

import React from 'react';
import { auth } from '@/auth';
import styles from './Dashboard.module.css';

export default async function DashboardPage() {
  const session = await auth();
  const user = session?.user;

  // Simple statistics cards
  const stats = [
    { label: 'Cụm Dịch vụ', value: '6 Services', desc: 'Đăng ký tại Eureka Server' },
    { label: 'Trạng thái Gateway', value: 'Hoạt động', desc: 'Port 8090 - Bảo mật JWT' },
    { label: 'Phiên kết nối OIDC', value: 'NextAuth.js v5', desc: 'Secure HttpOnly Cookie' },
  ];

  return (
    <div>
      <div className={styles.welcomeBanner}>
        <h1 className={styles.title}>Tổng quan Hệ thống</h1>
        <p className={styles.subtitle}>
          Bảng điều khiển giám sát kiến trúc Microservice & Phân quyền tập trung Single Sign-On.
        </p>
      </div>

      {/* Stats Cards */}
      <div className={styles.statsGrid}>
        {stats.map((stat, i) => (
          <div key={i} className={styles.statCard}>
            <div className={styles.statLabel}>{stat.label}</div>
            <div className={styles.statValue}>{stat.value}</div>
            <div className={styles.statDesc}>{stat.desc}</div>
          </div>
        ))}
      </div>

      {/* Decoded JWT Claims */}
      <div className={styles.card}>
        <h2 className={styles.cardTitle}>Chi tiết Token Identity (Decoded Claims)</h2>
        <p className={styles.cardSubtitle}>
          Dưới đây là thông tin định danh và phân quyền của tài khoản được giải mã trực tiếp từ Access Token JWT.
        </p>

        <div className={styles.claimsContainer}>
          <div className={styles.claimRow}>
            <span className={styles.claimKey}>Subject (User ID)</span>
            <span className={styles.claimValue} style={{ fontFamily: 'var(--font-mono)' }}>
              {user?.id || 'N/A'}
            </span>
          </div>

          <div className={styles.claimRow}>
            <span className={styles.claimKey}>Username</span>
            <span className={styles.claimValue}>{user?.name || 'N/A'}</span>
          </div>

          <div className={styles.claimRow}>
            <span className={styles.claimKey}>Email Address</span>
            <span className={styles.claimValue}>{user?.email || 'N/A'}</span>
          </div>

          <div className={styles.claimRow}>
            <span className={styles.claimKey}>Vai trò (Roles)</span>
            <div className={styles.badgeGroup}>
              {(user?.roles || []).map((role) => (
                <span key={role} className={`${styles.badge} ${styles.roleBadge}`}>
                  {role}
                </span>
              ))}
              {(user?.roles || []).length === 0 && <span className={styles.mutedText}>None</span>}
            </div>
          </div>

          <div className={styles.claimRow}>
            <span className={styles.claimKey}>Quyền hạn (Permissions)</span>
            <div className={styles.badgeGroup}>
              {(user?.permissions || []).map((perm) => (
                <span key={perm} className={`${styles.badge} ${styles.permBadge}`}>
                  {perm}
                </span>
              ))}
              {(user?.permissions || []).length === 0 && <span className={styles.mutedText}>None</span>}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

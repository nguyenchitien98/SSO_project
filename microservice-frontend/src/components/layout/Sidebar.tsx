import React from 'react';
import Link from 'next/link';
import { auth } from '@/auth';
import styles from './Layout.module.css';

export const Sidebar: React.FC = async () => {
  const session = await auth();
  const roles = session?.user?.roles || [];
  const isAdmin = roles.includes('ADMIN');

  return (
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <div className={styles.logoBadge}>MS</div>
        <div className={styles.brandText}>
          <span className={styles.brandName}>SSO Platform</span>
          <span className={styles.brandSubtitle}>Microservice Portal</span>
        </div>
      </div>

      <nav className={styles.nav}>
        <div className={styles.navGroup}>
          <div className={styles.navHeader}>Chính</div>
          <Link href="/" className={styles.navLink}>
            ⚡ Tổng quan
          </Link>
          <Link href="/products" className={styles.navLink}>
            📦 Sản phẩm
          </Link>
          <Link href="/orders" className={styles.navLink}>
            🛒 Đơn hàng
          </Link>
        </div>

        <div className={styles.navGroup}>
          <div className={styles.navHeader}>Cá nhân</div>
          <Link href="/profile" className={styles.navLink}>
            👤 Hồ sơ của tôi
          </Link>
        </div>

        {isAdmin && (
          <div className={styles.navGroup}>
            <div className={styles.navHeader}>Quản trị hệ thống</div>
            <Link href="/admin/users" className={styles.navLink}>
              👥 Quản lý Users
            </Link>
            <Link href="/admin/services" className={styles.navLink}>
              🔍 Eureka Services
            </Link>
            <Link href="/admin/reports" className={styles.navLink}>
              📊 Báo cáo Doanh thu
            </Link>
          </div>
        )}
      </nav>
      
      <div className={styles.footer}>
        <div className={styles.portLabel}>PORT: 3001</div>
      </div>
    </aside>
  );
};

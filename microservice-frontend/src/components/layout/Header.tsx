'use client';

import React from 'react';
import { signOut } from 'next-auth/react';
import styles from './Layout.module.css';

interface HeaderProps {
  user: {
    name?: string | null;
    email?: string | null;
    roles?: string[];
  };
}

export const Header: React.FC<HeaderProps> = ({ user }) => {
  const roles = user.roles || [];
  const primaryRole = roles[0] || 'GUEST';

  return (
    <header className={styles.header}>
      <div className={styles.headerLeft}>
        <span className={styles.welcomeText}>Xin chào, <strong style={{ color: 'var(--text-primary)' }}>{user.name || user.email}</strong></span>
        <span className={`${styles.roleBadge} ${styles[primaryRole.toLowerCase()] || ''}`}>
          {primaryRole}
        </span>
      </div>

      <div className={styles.headerRight}>
        <button
          className={styles.logoutBtn}
          onClick={() => signOut({ callbackUrl: '/login' })}
        >
          Đăng xuất
        </button>
      </div>
    </header>
  );
};

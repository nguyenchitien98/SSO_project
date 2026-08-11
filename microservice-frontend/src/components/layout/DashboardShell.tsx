import React from 'react';
import styles from './Layout.module.css';

interface DashboardShellProps {
  children: React.ReactNode;
}

export const DashboardShell: React.FC<DashboardShellProps> = ({ children }) => {
  return (
    <div className={styles.container}>
      {children}
    </div>
  );
};

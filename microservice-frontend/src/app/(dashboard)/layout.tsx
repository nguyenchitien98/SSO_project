import React from 'react';
import { auth } from '@/auth';
import { Sidebar } from '@/components/layout/Sidebar';
import { Header } from '@/components/layout/Header';
import { DashboardShell } from '@/components/layout/DashboardShell';
import styles from '@/components/layout/Layout.module.css';

export default async function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await auth();

  // NextAuth middleware ensures session exists here
  const user = session?.user || { name: '', email: 'unknown', roles: [] };

  return (
    <DashboardShell>
      <Sidebar />
      <div className={styles.mainWrapper}>
        <Header user={{ name: user.name, email: user.email, roles: user.roles }} />
        <main className={styles.content}>
          {children}
        </main>
      </div>
    </DashboardShell>
  );
}

import React from 'react';
import { userApi } from '@/lib/api/users';
import { AdminUsersTable } from './AdminUsersTable';
import styles from '../../products/Products.module.css';

export const dynamic = 'force-dynamic';

export default async function AdminUsersPage() {
  let users: any[] = [];
  let errorMsg = null;

  try {
    const data = await userApi.getUsers(0, 50);
    users = data.content || [];
  } catch (e: any) {
    console.error(e);
    errorMsg = 'Lỗi tải danh sách người dùng. Không thể kết nối đến User Service.';
  }

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Quản lý tài khoản</h1>
          <p className={styles.subtitle}>
            Trực quan hóa danh sách người dùng đăng ký qua SSO Server, chỉnh sửa hoạt động và phân quyền roles.
          </p>
        </div>
      </div>

      {errorMsg ? (
        <div className={styles.errorBox}>
          <p>{errorMsg}</p>
        </div>
      ) : (
        <AdminUsersTable initialUsers={users} />
      )}
    </div>
  );
}

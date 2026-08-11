'use client';

import React, { useState } from 'react';
import styles from '../../products/Products.module.css';

interface UserResponse {
  id: string;
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  enabled: boolean;
  locked: boolean;
  lockedReason: string | null;
  lastLoginAt: string | null;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

interface AdminUsersTableProps {
  initialUsers: UserResponse[];
}

export const AdminUsersTable: React.FC<AdminUsersTableProps> = ({ initialUsers }) => {
  const [users, setUsers] = useState<UserResponse[]>(initialUsers);
  
  // Modal roles assignment states
  const [editingUser, setEditingUser] = useState<UserResponse | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const [isSavingRoles, setIsSavingRoles] = useState(false);

  const reloadUsers = async () => {
    try {
      const res = await fetch('/api/mock/users');
      const payload = await res.json();
      if (payload.success) {
        setUsers(payload.data.content);
      }
    } catch (e) {
      console.error('Failed to reload users:', e);
    }
  };

  const handleToggleStatus = async (user: UserResponse) => {
    const actionText = user.enabled ? 'vô hiệu hóa' : 'kích hoạt';
    const reason = window.prompt(`Nhập lý do ${actionText} tài khoản "${user.username}":`, '');
    if (reason === null) return;

    try {
      const res = await fetch(`/api/mock/users/${user.id}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ enabled: !user.enabled, reason }),
      });

      if (!res.ok) {
        throw new Error('Cập nhật trạng thái người dùng thất bại.');
      }

      alert(`Đã ${actionText} tài khoản thành công!`);
      await reloadUsers();
    } catch (e: any) {
      alert(e.message || 'Lỗi xử lý.');
    }
  };

  const handleOpenRolesModal = (user: UserResponse) => {
    setEditingUser(user);
    setSelectedRoles(user.roles || []);
  };

  const handleToggleRoleSelection = (role: string) => {
    setSelectedRoles(prev => 
      prev.includes(role) ? prev.filter(r => r !== role) : [...prev, role]
    );
  };

  const handleSaveRoles = async () => {
    if (!editingUser) return;
    setIsSavingRoles(true);

    try {
      const res = await fetch(`/api/mock/users/${editingUser.id}/roles`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(selectedRoles),
      });

      if (!res.ok) {
        throw new Error('Gán vai trò thất bại.');
      }

      alert('Đã cập nhật phân quyền vai trò thành công!');
      setEditingUser(null);
      await reloadUsers();
    } catch (e: any) {
      alert(e.message || 'Lỗi cập nhật vai trò.');
    } finally {
      setIsSavingRoles(false);
    }
  };

  return (
    <div className={styles.tableCard}>
      {users.length === 0 ? (
        <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
          Không tìm thấy người dùng nào.
        </div>
      ) : (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Username</th>
              <th>Email</th>
              <th>Họ & Tên</th>
              <th>Trạng thái</th>
              <th>Vai trò (Roles)</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td style={{ fontWeight: 600 }}>{u.username}</td>
                <td>{u.email}</td>
                <td>{`${u.lastName || ''} ${u.firstName || ''}`}</td>
                <td>
                  <span
                    className={`${styles.badge} ${
                      u.enabled && !u.locked ? styles.inStock : styles.outOfStock
                    }`}
                    style={{
                      backgroundColor: u.enabled && !u.locked ? 'rgba(34, 197, 94, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                      padding: '2px 8px',
                      borderRadius: '9999px',
                      fontSize: '11px',
                    }}
                  >
                    {u.locked ? 'Bị khóa' : u.enabled ? 'Hoạt động' : 'Tạm khóa'}
                  </span>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                    {u.roles?.map(r => (
                      <span key={r} className={styles.categoryBadge} style={{ fontSize: '10px', color: 'var(--color-brand)', borderColor: 'var(--color-brand)' }}>
                        {r}
                      </span>
                    ))}
                  </div>
                </td>
                <td style={{ display: 'flex', gap: '8px' }}>
                  <button
                    className={styles.btnSecondary}
                    onClick={() => handleToggleStatus(u)}
                    style={{ padding: '6px 12px', fontSize: '12px' }}
                  >
                    {u.enabled ? 'Khóa' : 'Kích hoạt'}
                  </button>
                  <button
                    className={styles.btnSecondary}
                    onClick={() => handleOpenRolesModal(u)}
                    style={{ padding: '6px 12px', fontSize: '12px' }}
                  >
                    Phân quyền
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* Role Assignment Modal */}
      {editingUser && (
        <div style={modalOverlayStyle}>
          <div style={modalStyle}>
            <h3 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '15px' }}>
              Phân quyền cho: {editingUser.username}
            </h3>
            
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
              Chọn các vai trò để gán cho người dùng này. Quyền hạn chi tiết sẽ tự động đồng bộ chéo.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '30px' }}>
              {['ADMIN', 'MANAGER', 'STAFF', 'AUDITOR', 'USER', 'SUPPORT'].map((role) => (
                <label key={role} style={roleLabelStyle}>
                  <input
                    type="checkbox"
                    checked={selectedRoles.includes(role)}
                    onChange={() => handleToggleRoleSelection(role)}
                    style={{ width: '16px', height: '16px', cursor: 'pointer' }}
                  />
                  <span>{role}</span>
                </label>
              ))}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button
                className={styles.btnSecondary}
                onClick={() => setEditingUser(null)}
                disabled={isSavingRoles}
              >
                Hủy bỏ
              </button>
              <button
                className={styles.btnPrimary}
                onClick={handleSaveRoles}
                disabled={isSavingRoles}
              >
                {isSavingRoles ? 'Đang lưu...' : 'Lưu vai trò'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

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
  maxWidth: '460px',
  padding: '24px',
  boxShadow: '0 20px 25px -5px rgb(0 0 0 / 0.5)',
};

const roleLabelStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '10px',
  fontSize: '14px',
  fontWeight: 500,
  color: 'var(--text-primary)',
  cursor: 'pointer',
  padding: '8px 12px',
  backgroundColor: 'var(--bg-input)',
  borderRadius: '6px',
  border: '1px solid var(--border-color)',
};

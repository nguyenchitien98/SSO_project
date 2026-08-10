import React, { useEffect, useState } from 'react';
import { adminApi, UserResponse } from '@/services/adminApi';
import styles from '../products/Products.module.css';

export const AdminUsersPage: React.FC = () => {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Role Edit state
  const [editingUser, setEditingUser] = useState<UserResponse | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const [isSavingRoles, setIsSavingRoles] = useState(false);

  const fetchUsers = async (pageNumber: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await adminApi.getUsers(pageNumber, 10);
      setUsers(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (e: any) {
      console.error(e);
      setError(e.message || 'Lỗi tải danh sách người dùng từ SSO Server. Bạn cần đăng nhập với quyền ADMIN.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers(page);
  }, [page]);

  const handleToggleStatus = async (user: UserResponse) => {
    const actionText = user.enabled ? 'vô hiệu hóa' : 'kích hoạt';
    const reason = window.prompt(`Nhập lý do ${actionText} tài khoản "${user.username}":`, '');
    if (reason === null) return; // User cancelled prompt

    try {
      await adminApi.updateUserStatus(user.id, !user.enabled, reason);
      alert(`Đã ${actionText} tài khoản thành công`);
      fetchUsers(page);
    } catch (e: any) {
      console.error(e);
      alert(e.message || 'Cập nhật trạng thái tài khoản thất bại.');
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
      await adminApi.assignRoles(editingUser.id, selectedRoles);
      alert('Cập nhật vai trò người dùng thành công');
      setEditingUser(null);
      fetchUsers(page);
    } catch (e: any) {
      console.error(e);
      alert(e.message || 'Gán vai trò thất bại.');
    } finally {
      setIsSavingRoles(false);
    }
  };

  if (error) {
    return (
      <div>
        <h1 className={styles.title}>Quản lý người dùng</h1>
        <p style={{ color: 'var(--color-error)', marginTop: '20px' }}>{error}</p>
        <button className={styles.btnSecondary} onClick={() => fetchUsers(page)} style={{ marginTop: '10px' }}>
          Tải lại
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Quản trị Người dùng</h1>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
            Quản lý tài khoản đăng nhập, trạng thái hoạt động và phân quyền vai trò (SSO Server)
          </p>
        </div>
      </div>

      <div className={styles.tableCard}>
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            Đang tải dữ liệu người dùng...
          </div>
        ) : users.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            Không có người dùng nào.
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
                        u.enabled && !u.locked ? styles.activeBadge : styles.inactiveBadge
                      }`}
                    >
                      {u.locked ? 'Bị khóa' : u.enabled ? 'Hoạt động' : 'Tạm khóa'}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                      {u.roles?.map(r => (
                        <span key={r} className={styles.badge} style={{ fontSize: '10px', backgroundColor: 'rgba(99, 102, 241, 0.1)', color: 'var(--color-brand)' }}>
                          {r}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className={styles.actionCell}>
                    <button
                      className={styles.btnSecondary}
                      onClick={() => handleToggleStatus(u)}
                    >
                      {u.enabled ? 'Vô hiệu hóa' : 'Kích hoạt'}
                    </button>
                    <button
                      className={styles.btnSecondary}
                      onClick={() => handleOpenRolesModal(u)}
                    >
                      Gán vai trò
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {/* Pagination */}
        {!isLoading && totalPages > 1 && (
          <div className={styles.pagination}>
            <span className={styles.pageInfo}>
              Hiển thị trang {page + 1} / {totalPages} (Tổng số {totalElements} người dùng)
            </span>
            <div className={styles.pageBtns}>
              <button
                className={styles.pageBtn}
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Trước
              </button>
              <button
                className={styles.pageBtn}
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Role Assignment Modal */}
      {editingUser && (
        <div style={modalOverlayStyle}>
          <div style={modalStyle}>
            <h3 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '15px' }}>
              Phân quyền cho: {editingUser.username}
            </h3>
            
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '20px' }}>
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

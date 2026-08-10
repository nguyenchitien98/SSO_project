import React, { useEffect, useState } from 'react';
import { adminApi, AuditLogResponse } from '@/services/adminApi';
import styles from '../products/Products.module.css';

export const AuditLogPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchLogs = async (pageNumber: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await adminApi.getAuditLogs(pageNumber, 10);
      setLogs(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (e: any) {
      console.error(e);
      setError(e.message || 'Lỗi tải nhật ký thao tác hệ thống (Audit Logs).');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs(page);
  }, [page]);

  if (error) {
    return (
      <div>
        <h1 className={styles.title}>Nhật ký hệ thống (Audit Logs)</h1>
        <p style={{ color: 'var(--color-error)', marginTop: '20px' }}>{error}</p>
        <button className={styles.btnSecondary} onClick={() => fetchLogs(page)} style={{ marginTop: '10px' }}>
          Tải lại
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Nhật ký thao tác (Audit Logs)</h1>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
            Giám sát thời gian thực lịch sử các hành động tạo mới, cập nhật hoặc xóa tài nguyên chéo hệ thống
          </p>
        </div>
      </div>

      <div className={styles.tableCard}>
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            Đang tải dữ liệu nhật ký...
          </div>
        ) : logs.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            Không có bản ghi nhật ký nào.
          </div>
        ) : (
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Người thực hiện</th>
                <th>Email</th>
                <th>Hành động</th>
                <th>Tài nguyên</th>
                <th>Mã Tài nguyên</th>
                <th>IP Address</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                    {new Date(log.createdAt).toLocaleString('vi-VN')}
                  </td>
                  <td style={{ fontWeight: 600 }}>{log.actorName}</td>
                  <td style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{log.actorEmail || 'N/A'}</td>
                  <td>
                    <span
                      className={styles.badge}
                      style={{
                        backgroundColor: log.action.includes('FAIL') || log.action.includes('DELETE')
                          ? 'rgba(239, 68, 68, 0.1)'
                          : 'rgba(34, 197, 94, 0.1)',
                        color: log.action.includes('FAIL') || log.action.includes('DELETE')
                          ? 'var(--color-error)'
                          : 'var(--color-success)',
                      }}
                    >
                      {log.action}
                    </span>
                  </td>
                  <td>{log.entityType}</td>
                  <td style={{ fontFamily: 'var(--font-mono)', fontSize: '12px' }}>{log.entityId || 'N/A'}</td>
                  <td style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', color: 'var(--text-secondary)' }}>
                    {log.ipAddress || '127.0.0.1'}
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
              Hiển thị trang {page + 1} / {totalPages} (Tổng số {totalElements} bản ghi nhật ký)
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
    </div>
  );
};

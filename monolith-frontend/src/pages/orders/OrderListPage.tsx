import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { orderApi } from '@/services/orderApi';
import { Order } from '@/types/order';
import styles from './Orders.module.css';

export const OrderListPage: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();

  const fetchOrders = async (pageNumber: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await orderApi.getMyOrders(pageNumber, 10);
      setOrders(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (e: any) {
      console.error(e);
      setError(e.message || 'Lỗi tải danh sách đơn hàng');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders(page);
  }, [page]);

  const getStatusBadgeClass = (status: Order['status']) => {
    switch (status) {
      case 'PENDING':
        return `${styles.statusBadge} ${styles.statusPending}`;
      case 'PAID':
      case 'DELIVERED':
        return `${styles.statusBadge} ${styles.statusPaid}`;
      case 'CANCELLED':
        return `${styles.statusBadge} ${styles.statusCancelled}`;
      default:
        return `${styles.statusBadge} ${styles.statusDefault}`;
    }
  };

  const getStatusText = (status: Order['status']) => {
    switch (status) {
      case 'PENDING': return 'Chờ xử lý';
      case 'CONFIRMED': return 'Đã xác nhận';
      case 'PAID': return 'Đã thanh toán';
      case 'SHIPPED': return 'Đang giao hàng';
      case 'DELIVERED': return 'Đã giao hàng';
      case 'CANCELLED': return 'Đã hủy';
      default: return status;
    }
  };

  if (error) {
    return (
      <div>
        <h1 className={styles.title}>Đơn hàng của tôi</h1>
        <p style={{ color: 'var(--color-error)', marginTop: '20px' }}>{error}</p>
        <button className={styles.pageBtn} onClick={() => fetchOrders(page)} style={{ marginTop: '10px' }}>
          Tải lại
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Đơn hàng của tôi</h1>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
            Theo dõi danh sách đơn hàng đã đặt mua
          </p>
        </div>
        <button className={styles.btnPrimary} onClick={() => navigate('/orders/new')}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          Tạo đơn hàng mới
        </button>
      </div>

      <div className={styles.card}>
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            Đang tải dữ liệu đơn hàng...
          </div>
        ) : orders.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            Bạn chưa có đơn đặt hàng nào trong hệ thống.
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-color)', backgroundColor: 'rgba(39, 39, 42, 0.3)' }}>
                <th style={{ padding: '16px 24px', fontSize: '12px', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Mã đơn hàng</th>
                <th style={{ padding: '16px 24px', fontSize: '12px', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Thời gian đặt</th>
                <th style={{ padding: '16px 24px', fontSize: '12px', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Tổng giá trị</th>
                <th style={{ padding: '16px 24px', fontSize: '12px', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Trạng thái</th>
                <th style={{ padding: '16px 24px', fontSize: '12px', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                  <td style={{ padding: '16px 24px', fontWeight: 600, fontFamily: 'var(--font-mono)' }}>{order.orderCode}</td>
                  <td style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontSize: '14px' }}>
                    {new Date(order.createdAt).toLocaleString('vi-VN')}
                  </td>
                  <td style={{ padding: '16px 24px', fontWeight: 600, color: 'var(--color-accent)' }}>
                    {order.totalAmount.toLocaleString('vi-VN')} đ
                  </td>
                  <td style={{ padding: '16px 24px' }}>
                    <span className={getStatusBadgeClass(order.status)}>
                      {getStatusText(order.status)}
                    </span>
                  </td>
                  <td style={{ padding: '16px 24px' }}>
                    <button
                      className={styles.pageBtn}
                      onClick={() => navigate(`/orders/${order.id}`)}
                    >
                      Chi tiết đơn
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
              Hiển thị trang {page + 1} / {totalPages} (Tổng số {totalElements} đơn hàng)
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

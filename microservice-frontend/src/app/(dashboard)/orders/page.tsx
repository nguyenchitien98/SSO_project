import React from 'react';
import Link from 'next/link';
import { orderApi } from '@/lib/api/orders';
import styles from '../products/Products.module.css';

export const dynamic = 'force-dynamic';

export default async function OrdersPage() {
  let orders: any[] = [];
  let errorMsg = null;

  try {
    const data = await orderApi.getOrders(0, 50);
    orders = data.content || [];
  } catch (e: any) {
    console.error(e);
    errorMsg = 'Không thể tải lịch sử đơn hàng. Vui lòng kiểm tra lại dịch vụ Order Service.';
  }

  // Helper to color order status badges
  const getStatusStyle = (status: string) => {
    switch (status) {
      case 'PAID':
      case 'DELIVERED':
        return { backgroundColor: 'rgba(34, 197, 94, 0.1)', color: 'var(--color-success)' };
      case 'PENDING':
      case 'CONFIRMED':
        return { backgroundColor: 'rgba(245, 158, 11, 0.1)', color: 'var(--color-warning)' };
      case 'CANCELLED':
      case 'REFUNDED':
        return { backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--color-error)' };
      default:
        return { backgroundColor: 'rgba(255, 255, 255, 0.05)', color: 'var(--text-secondary)' };
    }
  };

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Quản lý Đơn hàng</h1>
          <p className={styles.subtitle}>
            Xem lịch sử giao dịch mua sắm, trạng thái thanh toán và địa chỉ giao nhận đơn hàng.
          </p>
        </div>
        <Link href="/orders/new" className={styles.btnPrimary}>
          Tạo đơn hàng mới
        </Link>
      </div>

      {errorMsg ? (
        <div className={styles.errorBox}>
          <p>{errorMsg}</p>
        </div>
      ) : orders.length === 0 ? (
        <div className={styles.emptyBox}>
          <p>Không có giao dịch đơn hàng nào.</p>
        </div>
      ) : (
        <div className={styles.tableCard}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Mã Đơn</th>
                <th>Khách hàng</th>
                <th>Tổng số tiền</th>
                <th>Địa chỉ giao hàng</th>
                <th>Trạng thái</th>
                <th>Thời gian đặt</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((o) => (
                <tr key={o.id}>
                  <td className={styles.monoId}>{o.id}</td>
                  <td>{o.userEmail}</td>
                  <td style={{ fontWeight: 600 }}>
                    {o.totalAmount.toLocaleString('vi-VN')} VND
                  </td>
                  <td style={{ maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {o.shippingAddress}
                  </td>
                  <td>
                    <span
                      className={styles.badge}
                      style={getStatusStyle(o.status)}
                    >
                      {o.status}
                    </span>
                  </td>
                  <td style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                    {new Date(o.createdAt).toLocaleDateString('vi-VN')}
                  </td>
                  <td>
                    <Link
                      href={`/orders/${o.id}`}
                      className={styles.btnSecondary}
                    >
                      Chi tiết
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

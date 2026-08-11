import React from 'react';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { orderApi } from '@/lib/api/orders';
import { CancelOrderButton } from './CancelOrderButton';
import styles from '../../products/Products.module.css';

export const dynamic = 'force-dynamic';


export default async function OrderDetailPage(props: { params: Promise<{ id: string }> }) {
  const params = await props.params;
  let order = null;

  try {
    order = await orderApi.getOrderById(params.id);
  } catch (e) {
    console.error(e);
  }

  if (!order) {
    notFound();
  }

  return (
    <div>
      <div className={styles.backLinkContainer}>
        <Link href="/orders" className={styles.backLink}>
          ← Quay lại danh sách đơn hàng
        </Link>
      </div>

      <div className={styles.detailCard}>
        <div className={styles.detailHeader}>
          <div>
            <span className={styles.detailCategory}>Chi tiết giao dịch</span>
            <h1 className={styles.detailTitle}>Đơn hàng {order.id}</h1>
            <p className={styles.detailId}>Khách hàng: {order.userEmail}</p>
          </div>
          
          {order.status === 'PENDING' && (
            <CancelOrderButton id={order.id} />
          )}
        </div>

        <div className={styles.detailGrid} style={{ marginBottom: '40px' }}>
          <div className={styles.detailItem}>
            <div className={styles.detailLabel}>Tổng thanh toán</div>
            <div className={styles.detailValue} style={{ color: 'var(--color-success)' }}>
              {order.totalAmount.toLocaleString('vi-VN')} VND
            </div>
          </div>

          <div className={styles.detailItem}>
            <div className={styles.detailLabel}>Trạng thái đơn</div>
            <div className={styles.detailValue} style={{ fontSize: 'var(--text-lg)' }}>
              {order.status}
            </div>
          </div>

          <div className={styles.detailItem}>
            <div className={styles.detailLabel}>Thời gian tạo</div>
            <div className={styles.detailValue} style={{ fontSize: 'var(--text-sm)', fontWeight: 500 }}>
              {new Date(order.createdAt).toLocaleString('vi-VN')}
            </div>
          </div>

          <div className={styles.detailItem}>
            <div className={styles.detailLabel}>Địa chỉ giao nhận</div>
            <div className={styles.detailValue} style={{ fontSize: 'var(--text-sm)', fontWeight: 500 }}>
              {order.shippingAddress}
            </div>
          </div>
        </div>

        <h3 style={{ fontSize: 'var(--text-base)', fontWeight: 700, marginBottom: '20px', borderBottom: '1px solid var(--border-color)', paddingBottom: '10px' }}>
          Danh sách mặt hàng mua
        </h3>
        
        <table className={styles.table} style={{ border: '1px solid var(--border-color)', borderRadius: '8px' }}>
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th>Đơn giá</th>
              <th>Số lượng</th>
              <th>Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            {order.items.map((item: any) => (
              <tr key={item.id}>
                <td style={{ fontWeight: 600 }}>{item.productName}</td>
                <td>{item.price.toLocaleString('vi-VN')} VND</td>
                <td>{item.quantity}</td>
                <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                  {(item.price * item.quantity).toLocaleString('vi-VN')} VND
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

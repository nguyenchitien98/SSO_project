import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { orderApi } from '@/services/orderApi';
import { Order } from '@/types/order';
import styles from './Orders.module.css';

export const OrderDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);
  const navigate = useNavigate();

  const fetchOrderDetail = async () => {
    if (!id) return;
    setIsLoading(true);
    setError(null);
    try {
      const data = await orderApi.getOrderById(Number(id));
      setOrder(data);
    } catch (e: any) {
      console.error(e);
      setError(e.message || 'Lỗi tải chi tiết đơn hàng. Bạn có thể không có quyền truy cập đơn hàng này.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchOrderDetail();
  }, [id]);

  const handleCancelOrder = async () => {
    if (!order) return;
    if (!window.confirm('Bạn có chắc chắn muốn yêu cầu hủy đơn hàng này không?')) {
      return;
    }
    
    setIsCancelling(true);
    try {
      await orderApi.cancelOrder(order.id);
      alert('Hủy đơn hàng thành công');
      fetchOrderDetail();
    } catch (e: any) {
      console.error(e);
      alert(e.message || 'Không thể hủy đơn hàng.');
    } finally {
      setIsCancelling(false);
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

  if (isLoading) {
    return <div style={{ color: 'var(--text-secondary)', padding: '20px' }}>Đang tải chi tiết đơn hàng...</div>;
  }

  if (error || !order) {
    return (
      <div>
        <p style={{ color: 'var(--color-error)', margin: '20px 0' }}>{error || 'Không tìm thấy thông tin đơn hàng.'}</p>
        <button className={styles.btnSecondary} onClick={() => navigate('/orders')}>
          Quay lại danh sách
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Đơn hàng: #{order.orderCode}</h1>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
            Đặt ngày: {new Date(order.createdAt).toLocaleString('vi-VN')}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button className={styles.btnSecondary} onClick={() => navigate('/orders')}>
            Quay lại danh sách
          </button>
          {order.status === 'PENDING' && (
            <button
              className={styles.btnDanger}
              onClick={handleCancelOrder}
              disabled={isCancelling}
            >
              {isCancelling ? 'Đang hủy...' : 'Hủy đơn hàng'}
            </button>
          )}
        </div>
      </div>

      <div className={styles.card}>
        <div className={styles.detailGrid}>
          <div className={styles.infoBlock}>
            <span className={styles.infoLabel}>Trạng thái đơn hàng</span>
            <span style={{ fontSize: '16px', fontWeight: 600, color: 'var(--color-brand)' }}>
              {getStatusText(order.status)}
            </span>
          </div>

          <div className={styles.infoBlock}>
            <span className={styles.infoLabel}>Địa chỉ giao hàng</span>
            <span className={styles.infoVal}>{order.shippingAddress}</span>
          </div>

          <div className={styles.infoBlock}>
            <span className={styles.infoLabel}>Ghi chú khách hàng</span>
            <span className={styles.infoVal}>{order.notes || 'Không có ghi chú'}</span>
          </div>

          <div className={styles.infoBlock}>
            <span className={styles.infoLabel}>ID người đặt hàng</span>
            <span className={styles.infoVal} style={{ fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
              {order.userId}
            </span>
          </div>
        </div>

        {/* Order items table */}
        <div style={{ borderTop: '1px solid var(--border-color)', padding: '24px' }}>
          <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '15px' }}>Sản phẩm đặt mua</h3>
          
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)' }}>
                <th style={{ padding: '8px 12px', fontSize: '12px', textTransform: 'uppercase' }}>Tên sản phẩm</th>
                <th style={{ padding: '8px 12px', fontSize: '12px', textTransform: 'uppercase', textAlign: 'right' }}>Đơn giá</th>
                <th style={{ padding: '8px 12px', fontSize: '12px', textTransform: 'uppercase', textAlign: 'center' }}>Số lượng</th>
                <th style={{ padding: '8px 12px', fontSize: '12px', textTransform: 'uppercase', textAlign: 'right' }}>Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              {order.items?.map((item) => (
                <tr key={item.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                  <td style={{ padding: '12px', fontWeight: 500 }}>{item.productName}</td>
                  <td style={{ padding: '12px', textAlign: 'right', fontFamily: 'var(--font-mono)' }}>
                    {item.unitPrice.toLocaleString('vi-VN')} đ
                  </td>
                  <td style={{ padding: '12px', textAlign: 'center' }}>{item.quantity}</td>
                  <td style={{ padding: '12px', textAlign: 'right', fontWeight: 600, color: 'var(--color-accent)' }}>
                    {item.subtotal.toLocaleString('vi-VN')} đ
                  </td>
                </tr>
              ))}
              <tr>
                <td colSpan={3} style={{ padding: '16px 12px', fontWeight: 700, fontSize: '16px', textAlign: 'right' }}>Tổng thanh toán:</td>
                <td style={{ padding: '16px 12px', fontWeight: 700, fontSize: '18px', color: 'var(--color-brand)', textAlign: 'right' }}>
                  {order.totalAmount.toLocaleString('vi-VN')} đ
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

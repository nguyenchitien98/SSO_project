'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import styles from '../../products/Products.module.css';

interface CancelOrderButtonProps {
  id: string;
}

export const CancelOrderButton: React.FC<CancelOrderButtonProps> = ({ id }) => {
  const router = useRouter();
  const [isCancelling, setIsCancelling] = useState(false);

  const handleCancel = async () => {
    if (!confirm('Bạn có chắc chắn muốn hủy đơn hàng này?')) {
      return;
    }

    setIsCancelling(true);
    try {
      const res = await fetch(`/api/mock/orders/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'CANCELLED' }),
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || 'Lỗi hủy đơn hàng.');
      }

      alert('Đã hủy đơn hàng thành công!');
      router.refresh();
    } catch (e: any) {
      console.error(e);
      alert(e.message || 'Lỗi xảy ra khi hủy đơn hàng.');
    } finally {
      setIsCancelling(false);
    }
  };

  return (
    <button
      className={styles.btnDanger}
      onClick={handleCancel}
      disabled={isCancelling}
    >
      {isCancelling ? 'Đang hủy...' : 'Hủy đơn hàng'}
    </button>
  );
};

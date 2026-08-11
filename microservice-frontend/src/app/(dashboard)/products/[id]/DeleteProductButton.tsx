'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import styles from '../Products.module.css';

interface DeleteProductButtonProps {
  id: string;
}

export const DeleteProductButton: React.FC<DeleteProductButtonProps> = ({ id }) => {
  const router = useRouter();
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDelete = async () => {
    if (!confirm('Bạn có chắc chắn muốn xóa sản phẩm này? Thao tác này không thể hoàn tác.')) {
      return;
    }

    setIsDeleting(true);
    try {
      const res = await fetch(`/api/mock/products/${id}`, {
        method: 'DELETE',
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || 'Lỗi xóa sản phẩm.');
      }

      alert('Đã xóa sản phẩm thành công!');
      router.push('/products');
      router.refresh();
    } catch (e: any) {
      console.error(e);
      alert(e.message || 'Lỗi xảy ra khi xóa sản phẩm.');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <button
      className={styles.btnDanger}
      onClick={handleDelete}
      disabled={isDeleting}
    >
      {isDeleting ? 'Đang xóa...' : 'Xóa sản phẩm'}
    </button>
  );
};

'use client';

import React, { useState, useTransition } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import styles from '../Products.module.css';

export default function NewProductPage() {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();

  const [form, setForm] = useState({
    name: '',
    price: '',
    stock: '',
    category: 'Điện thoại',
  });
  
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setIsSubmitting(true);

    try {
      const res = await fetch('/api/mock/products', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: form.name,
          price: Number(form.price),
          stock: Number(form.stock),
          category: form.category,
        }),
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || 'Lỗi thêm sản phẩm.');
      }

      alert('Đã thêm sản phẩm mới thành công!');
      
      startTransition(() => {
        router.push('/products');
        router.refresh();
      });
    } catch (e: any) {
      console.error(e);
      setErrorMsg(e.message || 'Có lỗi xảy ra khi gửi dữ liệu.');
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <div className={styles.backLinkContainer}>
        <Link href="/products" className={styles.backLink}>
          ← Quay lại danh sách sản phẩm
        </Link>
      </div>

      <div className={styles.formCard}>
        <h1 className={styles.title} style={{ marginBottom: '10px' }}>Thêm sản phẩm mới</h1>
        <p className={styles.subtitle} style={{ marginBottom: '24px' }}>
          Tạo sản phẩm mới trên hệ thống phân phối. Dữ liệu sẽ được truyền trực tiếp đến API Gateway.
        </p>

        {errorMsg && (
          <div className={styles.errorBox} style={{ marginBottom: '20px' }}>
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="name">
              Tên sản phẩm *
            </label>
            <input
              id="name"
              name="name"
              type="text"
              required
              className={styles.formInput}
              value={form.name}
              onChange={handleChange}
              placeholder="Ví dụ: iPad Pro M4"
              disabled={isSubmitting || isPending}
            />
          </div>

          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="price">
              Giá bán lẻ (VND) *
            </label>
            <input
              id="price"
              name="price"
              type="number"
              required
              min="0"
              className={styles.formInput}
              value={form.price}
              onChange={handleChange}
              placeholder="Ví dụ: 28990000"
              disabled={isSubmitting || isPending}
            />
          </div>

          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="stock">
              Số lượng nhập kho *
            </label>
            <input
              id="stock"
              name="stock"
              type="number"
              required
              min="0"
              className={styles.formInput}
              value={form.stock}
              onChange={handleChange}
              placeholder="Ví dụ: 100"
              disabled={isSubmitting || isPending}
            />
          </div>

          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="category">
              Danh mục nhóm sản phẩm
            </label>
            <select
              id="category"
              name="category"
              className={styles.formSelect}
              value={form.category}
              onChange={handleChange}
              disabled={isSubmitting || isPending}
            >
              <option value="Điện thoại">Điện thoại</option>
              <option value="Máy tính">Máy tính</option>
              <option value="Phụ kiện">Phụ kiện</option>
              <option value="Màn hình">Màn hình</option>
              <option value="Khác">Khác</option>
            </select>
          </div>

          <div style={{ display: 'flex', gap: '12px', marginTop: '30px' }}>
            <Link href="/products" className={styles.btnSecondary}>
              Hủy bỏ
            </Link>
            <button
              type="submit"
              className={styles.btnPrimary}
              disabled={isSubmitting || isPending}
            >
              {isSubmitting || isPending ? 'Đang lưu...' : 'Thêm sản phẩm'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

'use client';

import React, { useEffect, useState, useTransition } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Product } from '@/types/product';
import styles from '../../products/Products.module.css';

export default function CheckoutPage() {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();

  const [products, setProducts] = useState<Product[]>([]);
  const [isLoadingProducts, setIsLoadingProducts] = useState(true);
  const [selectedProduct, setSelectedProduct] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [shippingAddress, setShippingAddress] = useState('');

  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetch('/api/mock/products')
      .then(res => res.json())
      .then(payload => {
        if (payload.success) {
          const list = payload.data.content;
          setProducts(list);
          if (list.length > 0) {
            setSelectedProduct(list[0].id);
          }
        }
      })
      .catch(err => console.error(err))
      .finally(() => setIsLoadingProducts(false));
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProduct) {
      setErrorMsg('Vui lòng chọn ít nhất một sản phẩm.');
      return;
    }

    setErrorMsg(null);
    setIsSubmitting(true);

    const idempotencyKey = crypto.randomUUID();

    try {
      const res = await fetch('/api/mock/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': idempotencyKey,
        },
        body: JSON.stringify({
          shippingAddress,
          items: [
            {
              productId: selectedProduct,
              quantity: Number(quantity),
            },
          ],
        }),
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || 'Lỗi đặt hàng.');
      }

      alert('Đặt hàng thành công!');
      
      startTransition(() => {
        router.push('/orders');
        router.refresh();
      });
    } catch (e: any) {
      console.error(e);
      setErrorMsg(e.message || 'Có lỗi xảy ra khi gửi đơn đặt hàng.');
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <div className={styles.backLinkContainer}>
        <Link href="/orders" className={styles.backLink}>
          ← Quay lại danh sách đơn hàng
        </Link>
      </div>

      <div className={styles.formCard}>
        <h1 className={styles.title} style={{ marginBottom: '10px' }}>Tạo đơn hàng mới</h1>
        <p className={styles.subtitle} style={{ marginBottom: '24px' }}>
          Nhập thông tin sản phẩm và địa chỉ nhận hàng. Hệ thống đính kèm cơ chế chống gửi trùng lắp đơn (`Idempotency-Key`).
        </p>

        {errorMsg && (
          <div className={styles.errorBox} style={{ marginBottom: '20px' }}>
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="productId">
              Chọn mua sản phẩm
            </label>
            {isLoadingProducts ? (
              <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Đang tải danh sách sản phẩm...</p>
            ) : (
              <select
                id="productId"
                className={styles.formSelect}
                value={selectedProduct}
                onChange={e => setSelectedProduct(e.target.value)}
                disabled={isSubmitting || isPending}
              >
                {products.map(p => (
                  <option key={p.id} value={p.id}>
                    {p.name} - {p.price.toLocaleString('vi-VN')} VND (Tồn: {p.stock})
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="quantity">
              Số lượng mua *
            </label>
            <input
              id="quantity"
              type="number"
              required
              min="1"
              className={styles.formInput}
              value={quantity}
              onChange={e => setQuantity(e.target.value)}
              disabled={isSubmitting || isPending}
            />
          </div>

          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="shippingAddress">
              Địa chỉ nhận hàng *
            </label>
            <input
              id="shippingAddress"
              type="text"
              required
              className={styles.formInput}
              value={shippingAddress}
              onChange={e => setShippingAddress(e.target.value)}
              placeholder="Ví dụ: 102 Lê Thanh Nghị, Hai Bà Trưng, Hà Nội"
              disabled={isSubmitting || isPending}
            />
          </div>

          <div style={{ display: 'flex', gap: '12px', marginTop: '30px' }}>
            <Link href="/orders" className={styles.btnSecondary}>
              Hủy bỏ
            </Link>
            <button
              type="submit"
              className={styles.btnPrimary}
              disabled={isSubmitting || isPending || isLoadingProducts}
            >
              {isSubmitting || isPending ? 'Đang gửi...' : 'Xác nhận Đặt hàng'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

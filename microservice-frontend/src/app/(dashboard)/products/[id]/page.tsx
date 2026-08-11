import React from 'react';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { auth } from '@/auth';
import { productApi } from '@/lib/api/products';
import { DeleteProductButton } from './DeleteProductButton';
import styles from '../Products.module.css';

export const dynamic = 'force-dynamic';

export default async function ProductDetailPage(props: { params: Promise<{ id: string }> }) {
  const params = await props.params;
  const session = await auth();
  const permissions = session?.user?.permissions || [];
  const canDelete = permissions.includes('PRODUCT_DELETE');

  let product = null;

  try {
    product = await productApi.getProductById(params.id);
  } catch (e) {
    console.error(e);
  }

  if (!product) {
    notFound();
  }

  return (
    <div>
      <div className={styles.backLinkContainer}>
        <Link href="/products" className={styles.backLink}>
          ← Quay lại danh sách sản phẩm
        </Link>
      </div>

      <div className={styles.detailCard}>
        <div className={styles.detailHeader}>
          <div>
            <span className={styles.detailCategory}>{product.category}</span>
            <h1 className={styles.detailTitle}>{product.name}</h1>
            <p className={styles.detailId}>Mã sản phẩm: {product.id}</p>
          </div>
          
          {canDelete && (
            <DeleteProductButton id={product.id} />
          )}
        </div>

        <div className={styles.detailGrid}>
          <div className={styles.detailItem}>
            <div className={styles.detailLabel}>Giá bán niêm yết</div>
            <div className={styles.detailValue} style={{ color: 'var(--color-brand)' }}>
              {product.price.toLocaleString('vi-VN')} VND
            </div>
          </div>

          <div className={styles.detailItem}>
            <div className={styles.detailLabel}>Số lượng tồn kho</div>
            <div className={styles.detailValue}>
              {product.stock} sản phẩm
            </div>
          </div>
          
          <div className={styles.detailItem}>
            <div className={styles.detailLabel}>Ngày tạo bản ghi</div>
            <div className={styles.detailValue} style={{ fontSize: 'var(--text-sm)', fontWeight: 500 }}>
              {new Date(product.createdAt).toLocaleString('vi-VN')}
            </div>
          </div>

          <div className={styles.detailItem}>
            <div className={styles.detailLabel}>Cập nhật lần cuối</div>
            <div className={styles.detailValue} style={{ fontSize: 'var(--text-sm)', fontWeight: 500 }}>
              {new Date(product.updatedAt).toLocaleString('vi-VN')}
            </div>
          </div>
        </div>

        <div style={{ marginTop: '30px', borderTop: '1px solid var(--border-color)', paddingTop: '30px' }}>
          <Link href="/orders/new" className={styles.btnPrimary}>
            Đặt mua sản phẩm này
          </Link>
        </div>
      </div>
    </div>
  );
}

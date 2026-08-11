import React from 'react';
import Link from 'next/link';
import { auth } from '@/auth';
import { productApi } from '@/lib/api/products';
import styles from './Products.module.css';

export const dynamic = 'force-dynamic';

export default async function ProductsPage() {
  const session = await auth();
  const permissions = session?.user?.permissions || [];
  
  const canCreate = permissions.includes('PRODUCT_CREATE');

  let products: any[] = [];
  let errorMsg = null;

  try {
    const data = await productApi.getProducts(0, 50);
    products = data.content || [];
  } catch (e: any) {
    console.error(e);
    errorMsg = 'Không thể kết nối đến API Gateway. Vui lòng kiểm tra trạng thái cụm microservices.';
  }

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Sản phẩm hệ thống</h1>
          <p className={styles.subtitle}>
            Danh mục sản phẩm được đồng bộ thời gian thực từ Product Service qua API Gateway.
          </p>
        </div>
        {canCreate && (
          <Link href="/products/new" className={styles.btnPrimary}>
            Thêm sản phẩm
          </Link>
        )}
      </div>

      {errorMsg ? (
        <div className={styles.errorBox}>
          <p>{errorMsg}</p>
        </div>
      ) : products.length === 0 ? (
        <div className={styles.emptyBox}>
          <p>Không có sản phẩm nào trong hệ thống.</p>
        </div>
      ) : (
        <div className={styles.tableCard}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Mã</th>
                <th>Tên sản phẩm</th>
                <th>Danh mục</th>
                <th>Giá bán</th>
                <th>Tồn kho</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td className={styles.monoId}>{p.id}</td>
                  <td className={styles.productName}>{p.name}</td>
                  <td>
                    <span className={styles.categoryBadge}>{p.category}</span>
                  </td>
                  <td style={{ fontWeight: 600 }}>
                    {p.price.toLocaleString('vi-VN')} VND
                  </td>
                  <td>
                    <span
                      className={`${styles.stockStatus} ${
                        p.stock > 0 ? styles.inStock : styles.outOfStock
                      }`}
                    >
                      {p.stock > 0 ? `${p.stock} sản phẩm` : 'Hết hàng'}
                    </span>
                  </td>
                  <td>
                    <Link
                      href={`/products/${p.id}`}
                      className={styles.btnSecondary}
                    >
                      Xem chi tiết
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

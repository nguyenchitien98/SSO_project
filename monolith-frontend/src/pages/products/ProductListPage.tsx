import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productApi } from '@/services/productApi';
import { Product } from '@/types/product';
import { usePermission } from '@/auth/usePermission';
import styles from './Products.module.css';

export const ProductListPage: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { hasPermission, hasRole } = usePermission();
  const navigate = useNavigate();

  const canCreate = hasPermission('PRODUCT_CREATE');
  const canUpdate = hasPermission('PRODUCT_UPDATE');
  const canDelete = hasRole('ADMIN') || hasRole('MANAGER');

  const fetchProducts = async (pageNumber: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await productApi.getProducts(pageNumber, 10);
      setProducts(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (e: any) {
      console.error(e);
      setError(e.message || 'Lỗi tải danh sách sản phẩm');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts(page);
  }, [page]);

  const handleDelete = async (id: number, name: string) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa sản phẩm: "${name}" không?`)) {
      return;
    }
    try {
      await productApi.deleteProduct(id);
      alert('Xóa sản phẩm thành công');
      fetchProducts(page);
    } catch (e: any) {
      console.error(e);
      alert(e.message || 'Không thể xóa sản phẩm do lỗi phân quyền hoặc hệ thống.');
    }
  };

  if (error) {
    return (
      <div>
        <h1 className={styles.title}>Danh sách sản phẩm</h1>
        <p style={{ color: 'var(--color-error)', marginTop: '20px' }}>{error}</p>
        <button className={styles.btnSecondary} onClick={() => fetchProducts(page)} style={{ marginTop: '10px' }}>
          Tải lại
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Sản phẩm hệ thống</h1>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
            Xem và quản lý các mặt hàng sản phẩm kinh doanh
          </p>
        </div>
        {canCreate && (
          <button className={styles.btnPrimary} onClick={() => navigate('/products/new')}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            Thêm sản phẩm
          </button>
        )}
      </div>

      <div className={styles.tableCard}>
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            Đang tải dữ liệu sản phẩm...
          </div>
        ) : products.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            Không có sản phẩm nào trong hệ thống.
          </div>
        ) : (
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Tên sản phẩm</th>
                <th>Danh mục</th>
                <th>Giá bán</th>
                <th>Tồn kho</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.id}>
                  <td style={{ fontWeight: 600 }}>{product.name}</td>
                  <td>{product.category || 'N/A'}</td>
                  <td className={styles.price}>
                    {product.price.toLocaleString('vi-VN')} đ
                  </td>
                  <td className={styles.stock}>{product.stock}</td>
                  <td>
                    <span
                      className={`${styles.badge} ${
                        product.active ? styles.activeBadge : styles.inactiveBadge
                      }`}
                    >
                      {product.active ? 'Đang bán' : 'Ngừng bán'}
                    </span>
                  </td>
                  <td className={styles.actionCell}>
                    <button
                      className={styles.btnSecondary}
                      onClick={() => navigate(`/products/${product.id}`)}
                    >
                      Chi tiết
                    </button>
                    {canUpdate && (
                      <button
                        className={styles.btnSecondary}
                        onClick={() => navigate(`/products/${product.id}/edit`)}
                      >
                        Sửa
                      </button>
                    )}
                    {canDelete && (
                      <button
                        className={styles.btnDanger}
                        onClick={() => handleDelete(product.id, product.name)}
                      >
                        Xóa
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {/* Pagination controls */}
        {!isLoading && totalPages > 1 && (
          <div className={styles.pagination}>
            <span className={styles.pageInfo}>
              Hiển thị trang {page + 1} / {totalPages} (Tổng số {totalElements} sản phẩm)
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

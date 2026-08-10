import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { productApi } from '@/services/productApi';
import { Product } from '@/types/product';
import styles from './Products.module.css';

export const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!id) return;
    const fetchDetail = async () => {
      setIsLoading(true);
      try {
        const data = await productApi.getProductById(Number(id));
        setProduct(data);
      } catch (e: any) {
        console.error(e);
        setError(e.message || 'Lỗi tải chi tiết sản phẩm');
      } finally {
        setIsLoading(false);
      }
    };
    fetchDetail();
  }, [id]);

  if (isLoading) {
    return <div style={{ color: 'var(--text-secondary)' }}>Đang tải thông tin sản phẩm...</div>;
  }

  if (error || !product) {
    return (
      <div>
        <p style={{ color: 'var(--color-error)' }}>{error || 'Không tìm thấy sản phẩm'}</p>
        <button className={styles.btnSecondary} onClick={() => navigate('/products')}>
          Quay lại
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Chi tiết sản phẩm</h1>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>Mã số sản phẩm: #{product.id}</p>
        </div>
        <button className={styles.btnSecondary} onClick={() => navigate('/products')}>
          Quay lại danh sách
        </button>
      </div>

      <div className={styles.formCard} style={{ maxWidth: '100%' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '30px' }}>
          <div>
            <div style={imgContainerStyle}>
              {product.imageUrl ? (
                <img src={product.imageUrl} alt={product.name} style={imgStyle} />
              ) : (
                <div style={noImgStyle}>Không có ảnh sản phẩm</div>
              )}
            </div>
          </div>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div>
              <h2 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '8px' }}>{product.name}</h2>
              <span className={`${styles.badge} ${product.active ? styles.activeBadge : styles.inactiveBadge}`}>
                {product.active ? 'Đang bán' : 'Ngừng bán'}
              </span>
            </div>

            <div style={infoRowStyle}>
              <span style={labelStyle}>Giá bán:</span>
              <span style={valStyle} className={styles.price}>{product.price.toLocaleString('vi-VN')} đ</span>
            </div>

            <div style={infoRowStyle}>
              <span style={labelStyle}>Tồn kho:</span>
              <span style={valStyle}>{product.stock} sản phẩm</span>
            </div>

            <div style={infoRowStyle}>
              <span style={labelStyle}>Danh mục:</span>
              <span style={valStyle}>{product.category || 'Chưa phân loại'}</span>
            </div>

            <div>
              <span style={labelStyle}>Mô tả sản phẩm:</span>
              <p style={descStyle}>{product.description || 'Không có mô tả cho sản phẩm này.'}</p>
            </div>

            <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '20px', marginTop: '10px', fontSize: '12px', color: 'var(--text-muted)' }}>
              <p>Ngày tạo: {new Date(product.createdAt).toLocaleString('vi-VN')}</p>
              <p>Cập nhật cuối: {new Date(product.updatedAt).toLocaleString('vi-VN')}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const imgContainerStyle: React.CSSProperties = {
  width: '100%',
  aspectRatio: '1',
  backgroundColor: 'var(--bg-input)',
  border: '1px solid var(--border-color)',
  borderRadius: '12px',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  overflow: 'hidden',
};

const imgStyle: React.CSSProperties = {
  width: '100%',
  height: '100%',
  objectFit: 'cover',
};

const noImgStyle: React.CSSProperties = {
  color: 'var(--text-muted)',
  fontSize: '14px',
};

const infoRowStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  paddingBottom: '12px',
  borderBottom: '1px solid var(--border-color)',
};

const labelStyle: React.CSSProperties = {
  fontSize: '14px',
  color: 'var(--text-secondary)',
  fontWeight: 500,
};

const valStyle: React.CSSProperties = {
  fontSize: '16px',
  fontWeight: 600,
  color: 'var(--text-primary)',
};

const descStyle: React.CSSProperties = {
  fontSize: '14px',
  color: 'var(--text-secondary)',
  lineHeight: 1.6,
  marginTop: '8px',
};

import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { productApi } from '@/services/productApi';
import { CreateProductRequest, UpdateProductRequest } from '@/types/product';
import { ApiErrorResponse } from '@/types/api';
import styles from './Products.module.css';

export const ProductFormPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: '',
    price: '',
    stock: '',
    description: '',
    category: '',
    imageUrl: '',
    active: true,
  });

  const [isLoading, setIsLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);

  useEffect(() => {
    if (!isEdit) return;
    const fetchProduct = async () => {
      setIsLoading(true);
      try {
        const product = await productApi.getProductById(Number(id));
        setFormData({
          name: product.name,
          price: product.price.toString(),
          stock: product.stock.toString(),
          description: product.description || '',
          category: product.category || '',
          imageUrl: product.imageUrl || '',
          active: product.active,
        });
      } catch (e: any) {
        console.error(e);
        setGeneralError(e.message || 'Lỗi tải thông tin sản phẩm');
      } finally {
        setIsLoading(false);
      }
    };
    fetchProduct();
  }, [id, isEdit]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    const val = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;
    setFormData(prev => ({
      ...prev,
      [name]: val,
    }));
    // Clear validation error when editing
    if (fieldErrors[name]) {
      setFieldErrors(prev => {
        const copy = { ...prev };
        delete copy[name];
        return copy;
      });
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setGeneralError(null);
    setFieldErrors({});
    setIsLoading(true);

    const priceNum = parseFloat(formData.price);
    const stockNum = parseInt(formData.stock);

    if (isNaN(priceNum) || priceNum < 0) {
      setFieldErrors(prev => ({ ...prev, price: 'Giá bán phải là số dương lớn hơn hoặc bằng 0' }));
      setIsLoading(false);
      return;
    }

    if (isNaN(stockNum) || stockNum < 0) {
      setFieldErrors(prev => ({ ...prev, stock: 'Số lượng tồn kho phải lớn hơn hoặc bằng 0' }));
      setIsLoading(false);
      return;
    }

    try {
      if (isEdit) {
        const updateRequest: UpdateProductRequest = {
          name: formData.name,
          price: priceNum,
          stock: stockNum,
          description: formData.description || undefined,
          category: formData.category || undefined,
          imageUrl: formData.imageUrl || undefined,
          active: formData.active,
        };
        await productApi.updateProduct(Number(id), updateRequest);
        alert('Cập nhật sản phẩm thành công');
      } else {
        const createRequest: CreateProductRequest = {
          name: formData.name,
          price: priceNum,
          stock: stockNum,
          description: formData.description || undefined,
          category: formData.category || undefined,
          imageUrl: formData.imageUrl || undefined,
        };
        await productApi.createProduct(createRequest);
        alert('Tạo sản phẩm thành công');
      }
      navigate('/products');
    } catch (e: any) {
      console.error(e);
      if (e.errorCode && e.details) {
        const apiError: ApiErrorResponse = e;
        setFieldErrors(apiError.details || {});
        setGeneralError(apiError.message);
      } else {
        setGeneralError(e.message || 'Lỗi thực thi yêu cầu.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  if (generalError && isEdit && !formData.name) {
    return (
      <div>
        <p style={{ color: 'var(--color-error)' }}>{generalError}</p>
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
          <h1 className={styles.title}>{isEdit ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm mới'}</h1>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
            {isEdit ? 'Chỉnh sửa thông số sản phẩm hiện tại' : 'Đăng bán sản phẩm mới lên hệ thống'}
          </p>
        </div>
        <button className={styles.btnSecondary} onClick={() => navigate('/products')} disabled={isLoading}>
          Hủy bỏ
        </button>
      </div>

      <div className={styles.formCard}>
        {generalError && (
          <div style={{ color: 'var(--color-error)', backgroundColor: 'rgba(239, 68, 68, 0.1)', padding: '12px', borderRadius: '8px', marginBottom: '20px', border: '1px solid rgba(239, 68, 68, 0.2)', fontSize: '14px' }}>
            {generalError}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="name">Tên sản phẩm *</label>
            <input
              id="name"
              name="name"
              type="text"
              required
              className={`${styles.formInput} ${fieldErrors.name ? styles.formInputError : ''}`}
              value={formData.name}
              onChange={handleChange}
              disabled={isLoading}
            />
            {fieldErrors.name && <span className={styles.errorMsg}>{fieldErrors.name}</span>}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="price">Giá bán (VNĐ) *</label>
              <input
                id="price"
                name="price"
                type="number"
                step="0.01"
                required
                className={`${styles.formInput} ${fieldErrors.price ? styles.formInputError : ''}`}
                value={formData.price}
                onChange={handleChange}
                disabled={isLoading}
              />
              {fieldErrors.price && <span className={styles.errorMsg}>{fieldErrors.price}</span>}
            </div>

            <div className={styles.formGroup}>
              <label className={styles.formLabel} htmlFor="stock">Số lượng tồn kho *</label>
              <input
                id="stock"
                name="stock"
                type="number"
                required
                className={`${styles.formInput} ${fieldErrors.stock ? styles.formInputError : ''}`}
                value={formData.stock}
                onChange={handleChange}
                disabled={isLoading}
              />
              {fieldErrors.stock && <span className={styles.errorMsg}>{fieldErrors.stock}</span>}
            </div>
          </div>

          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="category">Danh mục</label>
            <input
              id="category"
              name="category"
              type="text"
              className={styles.formInput}
              value={formData.category}
              onChange={handleChange}
              disabled={isLoading}
              placeholder="VD: Điện tử, Thời trang..."
            />
          </div>

          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="imageUrl">Đường dẫn hình ảnh (URL)</label>
            <input
              id="imageUrl"
              name="imageUrl"
              type="text"
              className={styles.formInput}
              value={formData.imageUrl}
              onChange={handleChange}
              disabled={isLoading}
              placeholder="https://example.com/image.jpg"
            />
          </div>

          <div className={styles.formGroup}>
            <label className={styles.formLabel} htmlFor="description">Mô tả chi tiết</label>
            <textarea
              id="description"
              name="description"
              rows={4}
              style={{ fontFamily: 'inherit', resize: 'vertical' }}
              className={styles.formInput}
              value={formData.description}
              onChange={handleChange}
              disabled={isLoading}
            />
          </div>

          {isEdit && (
            <div className={styles.formGroup} style={{ flexDirection: 'row', alignItems: 'center', gap: '10px', marginTop: '10px' }}>
              <input
                id="active"
                name="active"
                type="checkbox"
                style={{ width: '16px', height: '16px', cursor: 'pointer' }}
                checked={formData.active}
                onChange={(e) => setFormData(prev => ({ ...prev, active: e.target.checked }))}
                disabled={isLoading}
              />
              <label className={styles.formLabel} htmlFor="active" style={{ cursor: 'pointer', marginBottom: 0 }}>Đang bán sản phẩm (Active)</label>
            </div>
          )}

          <div className={styles.formActions}>
            <button
              type="button"
              className={styles.btnSecondary}
              onClick={() => navigate('/products')}
              disabled={isLoading}
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              className={styles.btnPrimary}
              disabled={isLoading}
            >
              {isLoading ? 'Đang xử lý...' : isEdit ? 'Lưu thay đổi' : 'Thêm sản phẩm'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

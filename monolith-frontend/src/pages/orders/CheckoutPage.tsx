import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productApi } from '@/services/productApi';
import { orderApi } from '@/services/orderApi';
import { Product } from '@/types/product';
import styles from './Orders.module.css';

export const CheckoutPage: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedProductId, setSelectedProductId] = useState<string>('');
  const [quantity, setQuantity] = useState<number>(1);
  const [shippingAddress, setShippingAddress] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  
  const [cartItems, setCartItems] = useState<{ product: Product; quantity: number }[]>([]);
  const [isLoadingProducts, setIsLoadingProducts] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();

  useEffect(() => {
    const fetchActiveProducts = async () => {
      try {
        const data = await productApi.getProducts(0, 50); // Get first 50 active products
        const activeProducts = data.content.filter(p => p.active && p.stock > 0);
        setProducts(activeProducts);
        if (activeProducts.length > 0) {
          setSelectedProductId(activeProducts[0].id.toString());
        }
      } catch (e: any) {
        console.error(e);
        setError('Không thể tải danh sách sản phẩm phục vụ đặt hàng.');
      } finally {
        setIsLoadingProducts(false);
      }
    };
    fetchActiveProducts();
  }, []);

  const handleAddProduct = () => {
    if (!selectedProductId) return;
    const prod = products.find(p => p.id === Number(selectedProductId));
    if (!prod) return;

    // Check if product is already in cart
    const existingIndex = cartItems.findIndex(item => item.product.id === prod.id);
    if (existingIndex > -1) {
      const updated = [...cartItems];
      const newQty = updated[existingIndex].quantity + quantity;
      if (newQty > prod.stock) {
        alert(`Số lượng đặt mua vượt quá tồn kho hiện có (${prod.stock})`);
        return;
      }
      updated[existingIndex].quantity = newQty;
      setCartItems(updated);
    } else {
      if (quantity > prod.stock) {
        alert(`Số lượng đặt mua vượt quá tồn kho hiện có (${prod.stock})`);
        return;
      }
      setCartItems(prev => [...prev, { product: prod, quantity }]);
    }
  };

  const handleRemoveItem = (index: number) => {
    setCartItems(prev => prev.filter((_, i) => i !== index));
  };

  const calculateTotal = () => {
    return cartItems.reduce((sum, item) => sum + item.product.price * item.quantity, 0);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (cartItems.length === 0) {
      alert('Vui lòng thêm ít nhất một sản phẩm vào giỏ hàng');
      return;
    }
    if (!shippingAddress.trim()) {
      alert('Địa chỉ nhận hàng không được để trống');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    // Generate unique idempotency key for this submit
    const idempotencyKey = window.crypto.randomUUID();

    try {
      const createRequest = {
        items: cartItems.map(item => ({
          productId: item.product.id,
          quantity: item.quantity,
        })),
        shippingAddress,
        notes: notes || undefined,
        idempotencyKey,
      };

      await orderApi.createOrder(createRequest);
      alert('Đặt hàng thành công!');
      navigate('/orders');
    } catch (e: any) {
      console.error(e);
      setError(e.message || 'Đặt hàng thất bại. Vui lòng kiểm tra lại.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <div className={styles.headerContainer}>
        <div>
          <h1 className={styles.title}>Tạo đơn đặt hàng</h1>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
            Chọn sản phẩm, số lượng và thông tin giao hàng
          </p>
        </div>
        <button className={styles.btnSecondary} onClick={() => navigate('/orders')} disabled={isSubmitting}>
          Quay lại
        </button>
      </div>

      {error && (
        <div style={{ color: 'var(--color-error)', backgroundColor: 'rgba(239, 68, 68, 0.1)', padding: '12px', borderRadius: '8px', marginBottom: '20px', border: '1px solid rgba(239, 68, 68, 0.2)', fontSize: '14px' }}>
          {error}
        </div>
      )}

      <div className={styles.card}>
        <div className={styles.grid}>
          <div className={styles.checkoutForm}>
            {/* Step 1: Add product to cart */}
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '15px', color: 'var(--text-primary)' }}>
              1. Giỏ hàng đặt mua
            </h3>
            
            {isLoadingProducts ? (
              <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>Đang tải danh sách sản phẩm...</p>
            ) : products.length === 0 ? (
              <p style={{ fontSize: '14px', color: 'var(--color-warning)' }}>Không có sản phẩm nào khả dụng để đặt mua.</p>
            ) : (
              <div style={{ display: 'flex', gap: '15px', marginBottom: '20px', alignItems: 'flex-end' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '5px', flex: 1 }}>
                  <label style={{ fontSize: '12px', color: 'var(--text-secondary)' }} htmlFor="product">Sản phẩm</label>
                  <select
                    id="product"
                    className={styles.formInput}
                    style={{ width: '100%' }}
                    value={selectedProductId}
                    onChange={(e) => setSelectedProductId(e.target.value)}
                  >
                    {products.map(p => (
                      <option key={p.id} value={p.id}>
                        {p.name} - ({p.price.toLocaleString('vi-VN')} đ) - Tồn kho: {p.stock}
                      </option>
                    ))}
                  </select>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '5px', width: '100px' }}>
                  <label style={{ fontSize: '12px', color: 'var(--text-secondary)' }} htmlFor="quantity">Số lượng</label>
                  <input
                    id="quantity"
                    type="number"
                    min="1"
                    className={styles.formInput}
                    value={quantity}
                    onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                  />
                </div>

                <button type="button" className={styles.btnSecondary} onClick={handleAddProduct}>
                  Thêm
                </button>
              </div>
            )}

            {/* Cart display */}
            <div className={styles.cartList}>
              {cartItems.length === 0 ? (
                <div style={{ padding: '20px', border: '1px dashed var(--border-color)', borderRadius: '8px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>
                  Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm ở trên.
                </div>
              ) : (
                cartItems.map((item, index) => (
                  <div key={item.product.id} className={styles.cartItem}>
                    <div>
                      <p style={{ fontWeight: 600, fontSize: '14px' }}>{item.product.name}</p>
                      <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                        Đơn giá: {item.product.price.toLocaleString('vi-VN')} đ x {item.quantity}
                      </p>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                      <span className={styles.price}>
                        {(item.product.price * item.quantity).toLocaleString('vi-VN')} đ
                      </span>
                      <button
                        type="button"
                        className={styles.btnDanger}
                        style={{ padding: '4px 8px', fontSize: '12px' }}
                        onClick={() => handleRemoveItem(index)}
                      >
                        Xóa
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Step 2: Shipping details */}
            <h3 style={{ fontSize: '16px', fontWeight: 600, marginBottom: '15px', color: 'var(--text-primary)', borderTop: '1px solid var(--border-color)', paddingTop: '20px' }}>
              2. Thông tin giao hàng
            </h3>

            <form onSubmit={handleSubmit}>
              <div className={styles.formGroup}>
                <label className={styles.formLabel} htmlFor="shippingAddress">Địa chỉ giao hàng *</label>
                <input
                  id="shippingAddress"
                  type="text"
                  required
                  placeholder="Nhập địa chỉ nhận hàng chính xác"
                  className={styles.formInput}
                  value={shippingAddress}
                  onChange={(e) => setShippingAddress(e.target.value)}
                  disabled={isSubmitting}
                />
              </div>

              <div className={styles.formGroup}>
                <label className={styles.formLabel} htmlFor="notes">Ghi chú đơn hàng</label>
                <input
                  id="notes"
                  type="text"
                  placeholder="Ghi chú giao hàng nếu có (VD: Giao giờ hành chính)"
                  className={styles.formInput}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  disabled={isSubmitting}
                />
              </div>
            </form>
          </div>

          {/* Checkout summary panel */}
          <div className={styles.summaryPanel}>
            <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)' }}>
              Tóm tắt thanh toán
            </h3>
            
            <div className={styles.summaryRow}>
              <span>Tổng số lượng mặt hàng</span>
              <span>{cartItems.reduce((sum, item) => sum + item.quantity, 0)}</span>
            </div>

            <div className={styles.summaryRow}>
              <span>Phí vận chuyển</span>
              <span style={{ color: 'var(--color-success)', fontWeight: 500 }}>Miễn phí</span>
            </div>

            <div className={styles.totalRow}>
              <span>Thành tiền:</span>
              <span className={styles.price}>{calculateTotal().toLocaleString('vi-VN')} đ</span>
            </div>

            <button
              type="button"
              className={styles.btnPrimary}
              style={{ width: '100%', marginTop: '20px', padding: '12px', justifyContent: 'center' }}
              disabled={isSubmitting || cartItems.length === 0}
              onClick={handleSubmit}
            >
              {isSubmitting ? 'Đang xử lý đặt hàng...' : 'Xác nhận Đặt hàng'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

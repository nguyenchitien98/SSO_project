import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/auth/AuthContext';
import { ProtectedRoute } from '@/components/layout/ProtectedRoute';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { LoginPage } from '@/pages/LoginPage';
import { CallbackPage } from '@/pages/CallbackPage';
import { DashboardPage } from '@/pages/DashboardPage';
import { ProductListPage } from '@/pages/products/ProductListPage';
import { ProductDetailPage } from '@/pages/products/ProductDetailPage';
import { ProductFormPage } from '@/pages/products/ProductFormPage';
import { OrderListPage } from '@/pages/orders/OrderListPage';
import { CheckoutPage } from '@/pages/orders/CheckoutPage';
import { OrderDetailPage } from '@/pages/orders/OrderDetailPage';
import { ProfilePage } from '@/pages/ProfilePage';
import { AdminUsersPage } from '@/pages/admin/AdminUsersPage';
import { AuditLogPage } from '@/pages/admin/AuditLogPage';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Authentication routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/callback" element={<CallbackPage />} />

          {/* Secure authenticated routes */}
          <Route element={<ProtectedRoute />}>
            <Route element={<DashboardLayout />}>
              <Route path="/" element={<DashboardPage />} />
              
              {/* Product routes */}
              <Route path="/products" element={<ProductListPage />} />
              <Route path="/products/new" element={<ProductFormPage />} />
              <Route path="/products/:id" element={<ProductDetailPage />} />
              <Route path="/products/:id/edit" element={<ProductFormPage />} />
              
              {/* Order routes */}
              <Route path="/orders" element={<OrderListPage />} />
              <Route path="/orders/new" element={<CheckoutPage />} />
              <Route path="/orders/:id" element={<OrderDetailPage />} />
              
              {/* Profile routes */}
              <Route path="/profile" element={<ProfilePage />} />
              
              {/* Administrative routes */}
              <Route path="/admin/users" element={<AdminUsersPage />} />
              <Route path="/admin/audit-logs" element={<AuditLogPage />} />
            </Route>
          </Route>

          {/* Fallback to home */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;

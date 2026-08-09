# SSO Platform - Tiêu Chuẩn UI/UX & TypeScript (Next.js 15)

Tài liệu này đặc tả quy chuẩn thiết kế giao diện và viết code TypeScript/Next.js cho dự án **SSO Platform**. UI xoay quanh hai ứng dụng Next.js dùng React Server Components (RSC), TypeScript nghiêm ngặt (không `any`), và CSS Modules thuần.

---

## 1. Nguyên Tắc TypeScript — Không Dùng `any`

### 1.1 Rule bất di bất dịch

```typescript
// ❌ CẤM — Code bẩn, mất hết lợi ích TypeScript
const data: any = await response.json();
function handleError(err: any) { ... }
const user = {} as any;

// ✅ ĐÚNG — Luôn định nghĩa type cụ thể
const data: ProductResponse = await response.json();
function handleError(err: unknown) {
  if (err instanceof Error) {
    console.error(err.message);
  }
}
```

### 1.2 Xử Lý `unknown` Thay Vì `any`

```typescript
// ✅ Dùng unknown + type guard cho dữ liệu không chắc chắn
function isApiError(value: unknown): value is ApiErrorResponse {
  return (
    typeof value === 'object' &&
    value !== null &&
    'errorCode' in value &&
    'message' in value
  );
}

async function fetchUser(id: string): Promise<UserProfile> {
  try {
    const res = await fetch(`/api/users/${id}`);
    const json: unknown = await res.json();

    if (!res.ok && isApiError(json)) {
      throw new Error(json.message);
    }

    // Narrow type sau khi validate
    return json as UserProfile;
  } catch (err) {
    // err là unknown, không phải any
    if (err instanceof Error) throw err;
    throw new Error('Unknown error occurred');
  }
}
```

### 1.3 Khai Báo Types Rõ Ràng Cho API Response

```typescript
// ✅ types/api.ts — Central type definitions cho toàn dự án

/** Wrapper chuẩn cho mọi response từ Backend */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errorCode: string | null;
  timestamp: string;
}

/** Wrapper lỗi từ GlobalExceptionHandler */
export interface ApiErrorResponse {
  success: false;
  message: string;
  errorCode: string;
  details: Record<string, string> | null; // Validation field errors
  timestamp: string;
}

/** Pagination wrapper */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page (0-indexed)
  first: boolean;
  last: boolean;
}
```

### 1.4 Không Dùng Type Assertion `as` Bừa Bãi

```typescript
// ❌ CẤM — Force cast che giấu lỗi
const product = response as ProductResponse;

// ✅ ĐÚNG — Validate trước, cast sau khi chắc chắn
function parseProduct(json: unknown): ProductResponse {
  if (
    typeof json !== 'object' ||
    json === null ||
    !('id' in json) ||
    !('name' in json)
  ) {
    throw new Error('Invalid product data shape from API');
  }
  return json as ProductResponse; // OK vì đã validate
}
```

---

## 2. Cấu Trúc Project (Next.js 15 App Router)

```
sso-platform-ui/
├── src/
│   ├── app/                          # Next.js App Router
│   │   ├── layout.tsx                # Root layout (providers, fonts)
│   │   ├── page.tsx                  # Landing / redirect to login
│   │   ├── (auth)/                   # Auth route group
│   │   │   ├── login/
│   │   │   │   └── page.tsx          # Login page (redirect to SSO)
│   │   │   └── callback/
│   │   │       └── page.tsx          # OAuth2 callback handler
│   │   ├── (dashboard)/              # Protected route group
│   │   │   ├── layout.tsx            # Dashboard shell (Sidebar + Header)
│   │   │   ├── products/
│   │   │   │   ├── page.tsx          # Product list (RSC)
│   │   │   │   ├── [id]/
│   │   │   │   │   └── page.tsx      # Product detail (RSC)
│   │   │   │   └── new/
│   │   │   │       └── page.tsx      # Create product form (Client)
│   │   │   ├── orders/
│   │   │   │   ├── page.tsx
│   │   │   │   └── [id]/
│   │   │   │       └── page.tsx
│   │   │   └── admin/
│   │   │       ├── users/
│   │   │       │   └── page.tsx      # User management (ADMIN only)
│   │   │       └── roles/
│   │   │           └── page.tsx      # Role management (ADMIN only)
│   │   └── api/
│   │       └── auth/
│   │           └── [...nextauth]/
│   │               └── route.ts      # NextAuth API route
│   │
│   ├── components/
│   │   ├── common/                   # Tái sử dụng toàn dự án
│   │   │   ├── Button/
│   │   │   │   ├── Button.tsx
│   │   │   │   └── Button.module.css
│   │   │   ├── Input/
│   │   │   ├── Modal/
│   │   │   ├── Badge/                # Role badge (ADMIN, USER...)
│   │   │   ├── Skeleton/             # Loading placeholder
│   │   │   ├── Table/                # Generic data table
│   │   │   ├── Pagination/
│   │   │   └── ErrorBoundary/
│   │   │
│   │   ├── layout/                   # Layout components
│   │   │   ├── Sidebar/
│   │   │   │   ├── Sidebar.tsx
│   │   │   │   └── Sidebar.module.css
│   │   │   ├── Header/
│   │   │   │   ├── Header.tsx        # User info, logout button
│   │   │   │   └── Header.module.css
│   │   │   └── DashboardShell/       # Sidebar + Header wrapper
│   │   │
│   │   └── features/                 # Feature-specific components
│   │       ├── products/
│   │       │   ├── ProductCard.tsx
│   │       │   ├── ProductForm.tsx   # 'use client' form
│   │       │   └── ProductList.tsx
│   │       ├── orders/
│   │       └── auth/
│   │           └── LoginButton.tsx   # Trigger OAuth2 redirect
│   │
│   ├── hooks/                        # Custom React hooks
│   │   ├── useCurrentUser.ts         # Lấy user từ session
│   │   ├── usePermission.ts          # Check permission client-side
│   │   ├── useApi.ts                 # Generic API call hook
│   │   └── usePagination.ts
│   │
│   ├── lib/                          # Utilities, configs
│   │   ├── api/
│   │   │   ├── client.ts             # Base fetch wrapper (typed)
│   │   │   ├── products.ts           # Product API calls
│   │   │   ├── orders.ts
│   │   │   └── users.ts
│   │   ├── auth.ts                   # NextAuth config
│   │   └── utils.ts                  # Formatters, helpers
│   │
│   ├── types/                        # Global TypeScript types
│   │   ├── api.ts                    # ApiResponse, ApiError, Pagination
│   │   ├── auth.ts                   # Session, CurrentUser types
│   │   ├── product.ts                # Product, CreateProductRequest
│   │   ├── order.ts                  # Order, OrderItem
│   │   └── user.ts                   # UserProfile, Role
│   │
│   └── styles/
│       └── globals.css               # CSS Variables (design tokens)
│
├── public/
├── .env.local                        # LOCAL env (gitignored)
├── .env.example                      # Template env (committed)
├── next.config.ts
├── tsconfig.json
├── .prettierrc
└── .eslintrc.json
```

---

## 3. Design System — CSS Variables (Design Tokens)

```css
/* src/styles/globals.css */

:root {
  /* === Color Palette === */
  --bg-primary:       #09090b;    /* Nền tối chủ đạo */
  --bg-surface:       #18181b;    /* Nền card, sidebar, table */
  --bg-elevated:      #27272a;    /* Nền hover, tooltip */
  --bg-input:         #1f1f23;    /* Nền input field */

  --color-brand:      #6366f1;    /* Indigo — màu chủ đạo */
  --color-brand-hover:#4f46e5;
  --color-accent:     #8b5cf6;    /* Violet — nhấn */
  --color-success:    #22c55e;    /* Xanh lá — thành công, ADMIN */
  --color-warning:    #f59e0b;    /* Vàng — cảnh báo, MANAGER */
  --color-error:      #ef4444;    /* Đỏ — lỗi, bị khóa */
  --color-info:       #3b82f6;    /* Xanh dương — thông tin */

  --text-primary:     #fafafa;    /* Chữ chính */
  --text-secondary:   #a1a1aa;    /* Chữ phụ */
  --text-muted:       #71717a;    /* Caption, placeholder */
  --text-inverse:     #09090b;    /* Chữ trên nền sáng */

  --border-color:     #27272a;    /* Viền cards, dividers */
  --border-focus:     #6366f1;    /* Viền input khi focus */

  /* === Typography === */
  --font-sans: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', ui-monospace, monospace;

  --text-xs:   0.75rem;    /* 12px */
  --text-sm:   0.875rem;   /* 14px */
  --text-base: 1rem;       /* 16px */
  --text-lg:   1.125rem;   /* 18px */
  --text-xl:   1.25rem;    /* 20px */
  --text-2xl:  1.5rem;     /* 24px */
  --text-3xl:  1.875rem;   /* 30px */

  --font-normal: 400;
  --font-medium: 500;
  --font-semibold: 600;
  --font-bold:   700;

  /* === Spacing === */
  --space-1:  0.25rem;   /* 4px */
  --space-2:  0.5rem;    /* 8px */
  --space-3:  0.75rem;   /* 12px */
  --space-4:  1rem;      /* 16px */
  --space-5:  1.25rem;   /* 20px */
  --space-6:  1.5rem;    /* 24px */
  --space-8:  2rem;      /* 32px */
  --space-10: 2.5rem;    /* 40px */
  --space-12: 3rem;      /* 48px */

  /* === Border Radius === */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-full: 9999px;

  /* === Shadows === */
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.5);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.6);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.7);

  /* === Transitions === */
  --transition-fast:   150ms ease;
  --transition-normal: 200ms ease;
  --transition-slow:   300ms ease;

  /* === Z-index === */
  --z-dropdown: 100;
  --z-modal:    200;
  --z-toast:    300;
}

/* Light mode override */
[data-theme='light'] {
  --bg-primary:   #f4f4f5;
  --bg-surface:   #ffffff;
  --bg-elevated:  #f4f4f5;
  --bg-input:     #f4f4f5;
  --border-color: #e4e4e7;
  --text-primary: #18181b;
  --text-secondary:#71717a;
  --text-muted:   #a1a1aa;
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1);
}

/* Base reset */
*, *::before, *::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html {
  font-family: var(--font-sans);
  font-size: 16px;
  color: var(--text-primary);
  background-color: var(--bg-primary);
  -webkit-font-smoothing: antialiased;
}

body {
  min-height: 100vh;
  line-height: 1.5;
}
```

---

## 4. Quy Tắc React Server Component vs Client Component

```
Mặc định: Server Component (RSC)
→ Dùng khi: fetch data, render tĩnh, không cần state/event

Thêm 'use client':
→ Dùng khi: useState, useEffect, event handlers (onClick, onChange)
→ Dùng khi: hooks (useRouter, useSearchParams, useSession)
→ Dùng khi: browser API (localStorage, window)
```

### Pattern chuẩn — Page = RSC fetch + Client Component form

```tsx
// ✅ src/app/(dashboard)/products/page.tsx — SERVER COMPONENT
// Fetch data trực tiếp ở server, không cần useEffect

import { getProducts } from '@/lib/api/products';
import { ProductList } from '@/components/features/products/ProductList';
import type { PageResponse } from '@/types/api';
import type { Product } from '@/types/product';

interface ProductsPageProps {
  searchParams: Promise<{ page?: string; size?: string }>;
}

export default async function ProductsPage({ searchParams }: ProductsPageProps) {
  const params = await searchParams;
  const page = Number(params.page ?? '0');
  const size = Number(params.size ?? '10');

  // Fetch tại server → không cần useState, useEffect, loading state
  const products: PageResponse<Product> = await getProducts({ page, size });

  return (
    <main>
      <h1>Danh sách sản phẩm</h1>
      <ProductList initialData={products} />
    </main>
  );
}
```

```tsx
// ✅ src/components/features/products/ProductForm.tsx — CLIENT COMPONENT
'use client';

import { useState, useTransition } from 'react';
import { createProduct } from '@/lib/api/products';
import type { CreateProductRequest, Product } from '@/types/product';
import type { ApiErrorResponse } from '@/types/api';
import styles from './ProductForm.module.css';

interface ProductFormProps {
  onSuccess: (product: Product) => void;
}

export function ProductForm({ onSuccess }: ProductFormProps) {
  const [isPending, startTransition] = useTransition();
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  function handleSubmit(e: React.FormEvent<HTMLFormElement>): void {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);

    const request: CreateProductRequest = {
      name: formData.get('name') as string,
      price: Number(formData.get('price')),
      stock: Number(formData.get('stock')),
    };

    startTransition(async () => {
      try {
        const product = await createProduct(request);
        onSuccess(product);
      } catch (err: unknown) {
        // Xử lý lỗi validation từ backend
        if (isApiErrorWithDetails(err)) {
          setFieldErrors(err.details ?? {});
        }
      }
    });
  }

  return (
    <form onSubmit={handleSubmit} className={styles.form}>
      <div className={styles.field}>
        <label htmlFor="name">Tên sản phẩm</label>
        <input id="name" name="name" type="text" />
        {fieldErrors.name && <span className={styles.error}>{fieldErrors.name}</span>}
      </div>
      <button type="submit" disabled={isPending}>
        {isPending ? 'Đang lưu...' : 'Tạo sản phẩm'}
      </button>
    </form>
  );
}

// Type guard helper — tránh dùng any
function isApiErrorWithDetails(err: unknown): err is ApiErrorResponse {
  return (
    typeof err === 'object' &&
    err !== null &&
    'errorCode' in err &&
    'details' in err
  );
}
```

---

## 5. API Client — Typed Fetch Wrapper

```typescript
// src/lib/api/client.ts
import type { ApiResponse, ApiErrorResponse } from '@/types/api';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8090';

/**
 * Base fetch wrapper với typing đầy đủ.
 * Tại sao không dùng axios? → fetch native đã đủ mạnh với Next.js 15,
 * tránh dependency thừa, tận dụng Next.js caching built-in.
 *
 * @throws {ApiErrorResponse} khi server trả về lỗi (4xx, 5xx)
 */
async function request<T>(
  endpoint: string,
  options: RequestInit & { token?: string } = {}
): Promise<T> {
  const { token, ...fetchOptions } = options;

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...fetchOptions.headers,
  };

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...fetchOptions,
    headers,
  });

  // Parse JSON một lần
  const json: unknown = await response.json();

  if (!response.ok) {
    // Validate shape của error response
    if (isApiErrorResponse(json)) {
      throw json;
    }
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }

  // Validate shape của success response
  const apiResponse = json as ApiResponse<T>;
  return apiResponse.data;
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  return (
    typeof value === 'object' &&
    value !== null &&
    'success' in value &&
    (value as Record<string, unknown>).success === false &&
    'errorCode' in value
  );
}

// Exported typed helpers
export const apiClient = {
  get: <T>(endpoint: string, token?: string): Promise<T> =>
    request<T>(endpoint, { method: 'GET', token }),

  post: <T>(endpoint: string, body: unknown, token?: string): Promise<T> =>
    request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(body),
      token,
    }),

  put: <T>(endpoint: string, body: unknown, token?: string): Promise<T> =>
    request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body),
      token,
    }),

  delete: <T>(endpoint: string, token?: string): Promise<T> =>
    request<T>(endpoint, { method: 'DELETE', token }),
};
```

---

## 6. Type Definitions Chuẩn Cho Domain Objects

```typescript
// src/types/product.ts

export interface Product {
  id: number;
  name: string;
  description: string | null;
  price: number;
  stock: number;
  category: string | null;
  active: boolean;
  createdBy: string; // UUID
  createdAt: string; // ISO string
  updatedAt: string;
}

export interface CreateProductRequest {
  name: string;
  description?: string;
  price: number;
  stock: number;
  category?: string;
}

export interface UpdateProductRequest {
  name?: string;
  description?: string;
  price?: number;
  stock?: number;
  active?: boolean;
}

// src/types/auth.ts

export type Role = 'ADMIN' | 'MANAGER' | 'STAFF' | 'AUDITOR' | 'USER' | 'SUPPORT';

export type Permission =
  | 'USER_READ' | 'USER_CREATE' | 'USER_UPDATE' | 'USER_DELETE'
  | 'PRODUCT_READ' | 'PRODUCT_CREATE' | 'PRODUCT_UPDATE' | 'PRODUCT_DELETE'
  | 'ORDER_READ' | 'ORDER_CREATE' | 'ORDER_CANCEL' | 'ORDER_REFUND'
  | 'PAYMENT_READ' | 'PAYMENT_CREATE' | 'PAYMENT_REFUND'
  | 'AUDIT_READ';

export interface CurrentUser {
  id: string;         // UUID từ SSO sub claim
  email: string;
  name: string;
  roles: Role[];
  permissions: Permission[];
}

// src/types/order.ts

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface Order {
  id: number;
  orderCode: string;
  status: OrderStatus;
  totalAmount: number;
  items: OrderItem[];
  createdAt: string;
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}
```

---

## 7. Custom Hook Pattern

```typescript
// src/hooks/usePermission.ts
'use client';

import { useSession } from 'next-auth/react';
import type { Permission, Role } from '@/types/auth';

interface UsePermissionReturn {
  hasPermission: (permission: Permission) => boolean;
  hasRole: (role: Role) => boolean;
  isAdmin: boolean;
}

/**
 * Hook kiểm tra permissions của user hiện tại.
 * Dùng cho conditional rendering trên UI (ẩn/hiện button).
 *
 * Lưu ý: Đây chỉ là UI guard. Authorization thực sự được enforce tại Backend.
 * Không dùng để làm security decision — chỉ dùng cho UX.
 */
export function usePermission(): UsePermissionReturn {
  const { data: session } = useSession();
  const user = session?.user;

  return {
    hasPermission: (permission: Permission): boolean =>
      (user?.permissions ?? []).includes(permission),

    hasRole: (role: Role): boolean =>
      (user?.roles ?? []).includes(role),

    isAdmin: (user?.roles ?? []).includes('ADMIN'),
  };
}
```

```tsx
// Cách dùng trong component
'use client';

import { usePermission } from '@/hooks/usePermission';

export function ProductActions({ productId }: { productId: number }) {
  const { hasPermission } = usePermission();

  return (
    <div>
      {hasPermission('PRODUCT_UPDATE') && (
        <button>Chỉnh sửa</button>
      )}
      {hasPermission('PRODUCT_DELETE') && (
        <button>Xóa</button>
      )}
    </div>
  );
}
```

---

## 8. CSS Module Templates

### 8.1 Dashboard Layout

```css
/* src/components/layout/DashboardShell/DashboardShell.module.css */

.wrapper {
  display: flex;
  min-height: 100vh;
  background-color: var(--bg-primary);
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
  background-color: var(--bg-surface);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  padding: var(--space-6);
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
}

.main {
  flex: 1;
  min-width: 0; /* Prevent flex overflow */
  display: flex;
  flex-direction: column;
}

.header {
  height: 64px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-surface);
  display: flex;
  align-items: center;
  padding: 0 var(--space-6);
  position: sticky;
  top: 0;
  z-index: var(--z-dropdown);
}

.content {
  flex: 1;
  padding: var(--space-8);
  overflow-y: auto;
}

@media (max-width: 1024px) {
  .sidebar {
    display: none; /* Mobile: ẩn sidebar, dùng drawer */
  }
}
```

### 8.2 Button Component

```css
/* src/components/common/Button/Button.module.css */

.button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  font-family: var(--font-sans);
  border: 1px solid transparent;
  cursor: pointer;
  transition: background-color var(--transition-fast),
              border-color var(--transition-fast),
              transform var(--transition-fast);
  white-space: nowrap;
  text-decoration: none;
}

.button:active {
  transform: translateY(1px);
}

.button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

/* Variants */
.primary {
  background-color: var(--color-brand);
  color: white;
}
.primary:hover { background-color: var(--color-brand-hover); }

.secondary {
  background-color: var(--bg-elevated);
  color: var(--text-primary);
  border-color: var(--border-color);
}
.secondary:hover { background-color: var(--bg-surface); }

.danger {
  background-color: transparent;
  color: var(--color-error);
  border-color: var(--color-error);
}
.danger:hover { background-color: var(--color-error); color: white; }

/* Sizes */
.sm { padding: var(--space-1) var(--space-3); font-size: var(--text-xs); }
.lg { padding: var(--space-3) var(--space-6); font-size: var(--text-base); }
```

### 8.3 Skeleton Loading

```css
/* src/components/common/Skeleton/Skeleton.module.css */

@keyframes shimmer {
  0%   { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--bg-elevated) 25%,
    var(--bg-surface) 50%,
    var(--bg-elevated) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
}

.text { height: 1em; }
.heading { height: 1.5em; }
.card { height: 120px; border-radius: var(--radius-md); }
.avatar { width: 40px; height: 40px; border-radius: var(--radius-full); }
```

---

## 9. Quy Tắc ESLint & Prettier

### `.eslintrc.json`

```json
{
  "extends": [
    "next/core-web-vitals",
    "next/typescript"
  ],
  "rules": {
    "@typescript-eslint/no-explicit-any": "error",
    "@typescript-eslint/no-unsafe-assignment": "error",
    "@typescript-eslint/no-unsafe-member-access": "error",
    "@typescript-eslint/explicit-function-return-type": "warn",
    "@typescript-eslint/consistent-type-imports": "error",
    "no-console": ["warn", { "allow": ["warn", "error"] }],
    "prefer-const": "error",
    "no-var": "error"
  }
}
```

### `.prettierrc`

```json
{
  "semi": true,
  "singleQuote": true,
  "trailingComma": "es5",
  "tabWidth": 2,
  "printWidth": 100,
  "arrowParens": "always",
  "endOfLine": "lf"
}
```

### `tsconfig.json` — Strict Mode Bắt Buộc

```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "noUncheckedIndexedAccess": true,
    "exactOptionalPropertyTypes": true,
    "target": "ES2022",
    "module": "esnext",
    "moduleResolution": "bundler",
    "jsx": "preserve",
    "incremental": true,
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

---

## 10. Quy Tắc Micro-animations

```css
/* Hover card lift */
.card {
  background-color: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  transition: transform var(--transition-normal),
              box-shadow var(--transition-normal),
              border-color var(--transition-normal);
}
.card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-brand);
}

/* Focus ring chuẩn accessibility */
:focus-visible {
  outline: 2px solid var(--color-brand);
  outline-offset: 2px;
}
```

---

## 11 Responsive Breakpoints

```css
/* Mobile-first approach */
/* xs: 0–639px    (Mobile) */
/* sm: 640–767px  (Large Mobile) */
/* md: 768–1023px (Tablet) */
/* lg: 1024px+    (Desktop) */

.gridLayout {
  display: grid;
  grid-template-columns: 1fr;           /* Mobile: 1 cột */
  gap: var(--space-4);
}

@media (min-width: 640px) {
  .gridLayout { grid-template-columns: repeat(2, 1fr); }  /* Tablet: 2 cột */
}

@media (min-width: 1024px) {
  .gridLayout { grid-template-columns: repeat(3, 1fr); }  /* Desktop: 3 cột */
}

/* Bảng dữ liệu dài → scroll ngang trên mobile */
.tableWrapper {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
```

---

## 12. Design Tokens cho Status & Role Badges

Quy định mã màu CSS Variables cho các trạng thái và vai trò của người dùng dựa theo sơ đồ UI UX của dự án:

```css
:root {
  /* Role Badges colors (HSL) */
  --badge-admin-bg: hsl(0, 100%, 96%);
  --badge-admin-text: hsl(0, 100%, 45%);
  --badge-manager-bg: hsl(35, 100%, 95%);
  --badge-manager-text: hsl(35, 100%, 40%);
  --badge-staff-bg: hsl(200, 100%, 96%);
  --badge-staff-text: hsl(200, 100%, 40%);
  --badge-auditor-bg: hsl(150, 80%, 95%);
  --badge-auditor-text: hsl(150, 80%, 30%);
  --badge-support-bg: hsl(280, 80%, 96%);
  --badge-support-text: hsl(280, 80%, 45%);
  --badge-user-bg: hsl(0, 0%, 94%);
  --badge-user-text: hsl(0, 0%, 40%);

  /* Status Badges colors */
  --status-up-bg: hsl(145, 80%, 95%);
  --status-up-text: hsl(145, 80%, 25%);
  --status-down-bg: hsl(0, 100%, 96%);
  --status-down-text: hsl(0, 100%, 45%);
  --status-warning-bg: hsl(45, 100%, 94%);
  --status-warning-text: hsl(45, 100%, 30%);
  --status-processing-bg: hsl(210, 100%, 95%);
  --status-processing-text: hsl(210, 100%, 40%);
  --status-completed-bg: hsl(145, 80%, 90%);
  --status-completed-text: hsl(145, 80%, 20%);
  --status-cancelled-bg: hsl(0, 0%, 88%);
  --status-cancelled-text: hsl(0, 0%, 30%);
}
```

---

## 13. Đặc Tả Giao Diện Mới (UI Specifications)

### 13.1 Services Management Page (Dành riêng cho Microservice UI - Section 6.x)
- **Mục đích:** Hiển thị danh sách các microservice đăng ký với Eureka Server cùng thông số về trạng thái sức khỏe (health checks).
- **Thành phần:**
  - **Service Card/List:** Hiển thị cột Service Name, Service ID, Instances Count (số lượng pod/nốt), Heartbeat (ví dụ: "2s ago"), Status (Active/Up dùng `--status-up`, Down dùng `--status-down`).
  - **Metrics & Endpoints:** Mỗi service click vào hiển thị chi tiết các Port, URL endpoint API, và tab xem log trực tiếp thông qua Loki integration.

### 13.2 Centralized Reports & Analytics Dashboard (Section 9.x / 7.x)
- **Mục đích:** Theo dõi hiệu năng hệ thống và chỉ số kinh doanh.
- **Thành phần:**
  - **System Metrics:** CPU Usage, Memory Usage, Request Rate (req/s), Error Rate (%) dưới dạng Line Chart.
  - **Business Metrics:** Tổng doanh thu (Total Revenue), Số lượng đơn hàng (Total Orders), tỷ lệ chuyển đổi (Conversion Rate) vẽ dưới dạng cột hoặc biểu đồ hình tròn (Doughnut).

### 13.3 Profile Management & 2FA Setup Screen (Section 10.x / 1.6)
- **Mục đích:** Cho phép người dùng chỉnh sửa thông tin cá nhân và bật/tắt xác thực 2 lớp.
- **Thành phần:**
  - **Cá nhân hóa:** Cho phép upload ảnh đại diện (kết nối trực tiếp với upload API của file-service lưu trên MinIO).
  - **2FA Toggle:** Nút Switch bật/tắt 2FA. Khi nhấn bật, hiển thị Modal gồm:
    1. Hướng dẫn quét mã QR.
    2. Mã Secret Key Base32 (để copy).
    3. Input nhập mã 6 số OTP thử nghiệm.
    4. Nút bấm "Xác thực & Kích hoạt".


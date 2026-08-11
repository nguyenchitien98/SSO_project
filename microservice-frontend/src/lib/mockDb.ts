import { Product } from '@/types/product';
import { Order } from '@/types/order';

// Mock Database memory storage
declare global {
  var _mockProducts: Product[] | undefined;
  var _mockOrders: Order[] | undefined;
  var _mockUsers: any[] | undefined;
  var _idempotencyKeys: Set<string> | undefined;
}

if (!global._mockProducts) {
  global._mockProducts = [
    {
      id: 'p1',
      name: 'iPhone 15 Pro Max',
      price: 34990000,
      stock: 15,
      category: 'Điện thoại',
      imageUrl: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: 'p2',
      name: 'MacBook Pro M3 Max',
      price: 79990000,
      stock: 5,
      category: 'Máy tính',
      imageUrl: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: 'p3',
      name: 'Sony WH-1000XM5',
      price: 8490000,
      stock: 30,
      category: 'Phụ kiện',
      imageUrl: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: 'p4',
      name: 'Bàn phím cơ Keychron Q1 Pro',
      price: 4500000,
      stock: 25,
      category: 'Phụ kiện',
      imageUrl: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: 'p5',
      name: 'Dell UltraSharp U2723QE 4K',
      price: 13500000,
      stock: 10,
      category: 'Màn hình',
      imageUrl: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
  ];
}

if (!global._mockOrders) {
  global._mockOrders = [
    {
      id: 'ord-1',
      userId: 'b2d39f75-39c0-5d4f-9e0a-6f321e1a52d4',
      userEmail: 'user@sso.com',
      status: 'PAID',
      totalAmount: 43480000,
      shippingAddress: '123 Đường Láng, Đống Đa, Hà Nội',
      items: [
        { id: 1, productId: 'p1', productName: 'iPhone 15 Pro Max', quantity: 1, price: 34990000 },
        { id: 2, productId: 'p3', productName: 'Sony WH-1000XM5', quantity: 1, price: 8490000 },
      ],
      createdAt: new Date(Date.now() - 3600000 * 24).toISOString(),
      updatedAt: new Date(Date.now() - 3600000 * 24).toISOString(),
    },
    {
      id: 'ord-2',
      userId: 'b2d39f75-39c0-5d4f-9e0a-6f321e1a52d4',
      userEmail: 'user@sso.com',
      status: 'PENDING',
      totalAmount: 9000000,
      shippingAddress: '456 Lê Lợi, Quận 1, TP. HCM',
      items: [
        { id: 3, productId: 'p4', productName: 'Bàn phím cơ Keychron Q1 Pro', quantity: 2, price: 4500000 },
      ],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
  ];
}

if (!global._mockUsers) {
  global._mockUsers = [
    {
      id: 'a1c29e64-28b9-4c3e-8d99-5f210d0f41c3',
      username: 'admin',
      email: 'admin@sso.com',
      firstName: 'Hệ thống',
      lastName: 'Admin',
      enabled: true,
      locked: false,
      lockedReason: null,
      lastLoginAt: new Date().toISOString(),
      roles: ['ADMIN'],
      createdAt: new Date(Date.now() - 3600000 * 24 * 30).toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: 'b2d39f75-39c0-5d4f-9e0a-6f321e1a52d4',
      username: 'user',
      email: 'user@sso.com',
      firstName: 'Thành viên',
      lastName: 'User',
      enabled: true,
      locked: false,
      lockedReason: null,
      lastLoginAt: new Date().toISOString(),
      roles: ['USER'],
      createdAt: new Date(Date.now() - 3600000 * 24 * 29).toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: 'c3e40fa6-49d1-6e5f-0e1b-7f432f2b63e5',
      username: 'manager_test',
      email: 'manager@sso.com',
      firstName: 'Quản lý',
      lastName: 'Test',
      enabled: true,
      locked: false,
      lockedReason: null,
      lastLoginAt: null,
      roles: ['MANAGER'],
      createdAt: new Date(Date.now() - 3600000 * 24 * 5).toISOString(),
      updatedAt: new Date().toISOString(),
    },
  ];
}

if (!global._idempotencyKeys) {
  global._idempotencyKeys = new Set<string>();
}

export const mockProducts = global._mockProducts!;
export const mockOrders = global._mockOrders!;
export const mockUsers = global._mockUsers!;
export const idempotencyKeys = global._idempotencyKeys!;

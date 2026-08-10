import { apiClient } from './apiClient';
import { Order, CreateOrderRequest } from '@/types/order';
import { PageResponse } from '@/types/api';

export const orderApi = {
  getMyOrders: (page = 0, size = 10) =>
    apiClient.get<PageResponse<Order>>(`/api/orders?page=${page}&size=${size}`),

  getOrderById: (id: number) =>
    apiClient.get<Order>(`/api/orders/${id}`),

  createOrder: (request: CreateOrderRequest) => {
    // Generate idempotency key if not provided
    const key = request.idempotencyKey || window.crypto.randomUUID();
    return apiClient.post<Order>('/api/orders', request, { idempotencyKey: key });
  },

  cancelOrder: (id: number) =>
    apiClient.post<Order>(`/api/orders/${id}/cancel`),
};

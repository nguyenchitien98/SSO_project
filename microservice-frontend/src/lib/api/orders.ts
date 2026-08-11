import { auth } from '@/auth';
import { Order } from '@/types/order';
import { PageResponse } from '@/types/api';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090';

async function getAuthHeader(): Promise<Record<string, string>> {
  const session = await auth();
  if (session?.accessToken) {
    return { 'Authorization': `Bearer ${session.accessToken}` };
  }
  return {};
}

export const orderApi = {
  getOrders: async (page = 0, size = 10): Promise<PageResponse<Order>> => {
    const authHeader = await getAuthHeader();
    const res = await fetch(`${API_URL}/orders?page=${page}&size=${size}`, {
      headers: {
        ...authHeader,
        'Accept': 'application/json',
      },
      next: { revalidate: 0 },
    });

    if (!res.ok) {
      throw new Error(`Tải danh sách đơn hàng thất bại: ${res.status}`);
    }

    const payload = await res.json();
    return payload.data;
  },

  getOrderById: async (id: string): Promise<Order> => {
    const authHeader = await getAuthHeader();
    const res = await fetch(`${API_URL}/orders/${id}`, {
      headers: {
        ...authHeader,
        'Accept': 'application/json',
      },
      next: { revalidate: 0 },
    });

    if (!res.ok) {
      throw new Error(`Tải chi tiết đơn hàng thất bại: ${res.status}`);
    }

    const payload = await res.json();
    return payload.data;
  },
};

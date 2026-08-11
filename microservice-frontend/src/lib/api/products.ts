import { auth } from '@/auth';
import { Product } from '@/types/product';
import { PageResponse } from '@/types/api';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090';

async function getAuthHeader(): Promise<Record<string, string>> {
  const session = await auth();
  if (session?.accessToken) {
    return { 'Authorization': `Bearer ${session.accessToken}` };
  }
  return {};
}

export const productApi = {
  getProducts: async (page = 0, size = 10): Promise<PageResponse<Product>> => {
    const authHeader = await getAuthHeader();
    const res = await fetch(`${API_URL}/products?page=${page}&size=${size}`, {
      headers: {
        ...authHeader,
        'Accept': 'application/json',
      },
      next: { revalidate: 0 }, // Disable fetch caching
    });

    if (!res.ok) {
      throw new Error(`Tải danh sách sản phẩm thất bại: ${res.status}`);
    }

    const payload = await res.json();
    return payload.data;
  },

  getProductById: async (id: string): Promise<Product> => {
    const authHeader = await getAuthHeader();
    const res = await fetch(`${API_URL}/products/${id}`, {
      headers: {
        ...authHeader,
        'Accept': 'application/json',
      },
      next: { revalidate: 0 },
    });

    if (!res.ok) {
      throw new Error(`Tải chi tiết sản phẩm thất bại: ${res.status}`);
    }

    const payload = await res.json();
    return payload.data;
  },
};

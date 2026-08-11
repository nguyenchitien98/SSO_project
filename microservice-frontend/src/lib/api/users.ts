import { auth } from '@/auth';
import { PageResponse } from '@/types/api';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090';

async function getAuthHeader(): Promise<Record<string, string>> {
  const session = await auth();
  if (session?.accessToken) {
    return { 'Authorization': `Bearer ${session.accessToken}` };
  }
  return {};
}

export const userApi = {
  getUsers: async (page = 0, size = 10): Promise<PageResponse<any>> => {
    const authHeader = await getAuthHeader();
    const res = await fetch(`${API_URL}/users?page=${page}&size=${size}`, {
      headers: {
        ...authHeader,
        'Accept': 'application/json',
      },
      next: { revalidate: 0 },
    });

    if (!res.ok) {
      throw new Error(`Tải danh sách người dùng thất bại: ${res.status}`);
    }

    const payload = await res.json();
    return payload.data;
  },

  getEurekaServices: async (): Promise<any> => {
    // Eureka is proxied or mock endpoint
    const res = await fetch(`${API_URL}/eureka/apps`, {
      headers: {
        'Accept': 'application/json',
      },
      next: { revalidate: 0 },
    });

    if (!res.ok) {
      throw new Error(`Không thể kết nối đến Eureka Registry: ${res.status}`);
    }

    const payload = await res.json();
    return payload.applications?.application || [];
  },
};

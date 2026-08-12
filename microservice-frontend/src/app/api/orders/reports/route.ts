import { NextResponse } from 'next/server';
import { auth } from '@/auth';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090';

/**
 * BFF Endpoint proxy để lấy dữ liệu báo cáo doanh thu từ order-service.
 */
export async function GET() {
  try {
    const session = await auth();
    if (!session?.accessToken) {
      return NextResponse.json(
        { success: false, message: 'Chưa đăng nhập hệ thống' },
        { status: 401 }
      );
    }

    console.log('[Next.js BFF] Proxying GET /orders/reports to API Gateway.');

    const gatewayRes = await fetch(`${API_URL}/orders/reports`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${session.accessToken}`,
      },
    });

    const data = (await gatewayRes.json()) as { success?: boolean; message?: string };

    if (!gatewayRes.ok) {
      return NextResponse.json(
        { success: false, message: data.message || 'Lỗi lấy báo cáo tại Gateway' },
        { status: gatewayRes.status }
      );
    }

    return NextResponse.json(data);
  } catch (e: any) {
    console.error('Error proxying reports fetch:', e);
    return NextResponse.json(
      { success: false, message: e.message || 'Lỗi hệ thống khi tải báo cáo.' },
      { status: 500 }
    );
  }
}

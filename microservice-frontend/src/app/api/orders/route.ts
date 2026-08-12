import { NextResponse } from 'next/server';
import { auth } from '@/auth';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090';

/**
 * BFF Endpoint proxy để tạo đơn hàng an toàn.
 *
 * <p>Tại sao cần proxy này ở Next.js?
 * - Để đọc Access Token từ session (chỉ đọc được ở Server Side qua NextAuth).
 * - Đính kèm Access Token vào header Authorization gửi lên API Gateway.
 * - Hạn chế lộ Token ra client browser, phòng chống XSS.
 * - Nhận và chuyển tiếp header "Idempotency-Key" nguyên vẹn từ client.
 */
export async function POST(request: Request) {
  try {
    const session = await auth();
    if (!session?.accessToken) {
      return NextResponse.json(
        { success: false, message: 'Chưa đăng nhập hệ thống' },
        { status: 401 }
      );
    }

    const idempotencyKey = request.headers.get('idempotency-key');
    const body = (await request.json()) as unknown;

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${session.accessToken}`,
    };

    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }

    logProxyRequest(idempotencyKey);

    const gatewayRes = await fetch(`${API_URL}/orders`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    });

    const data = (await gatewayRes.json()) as { success?: boolean; message?: string };
    
    if (!gatewayRes.ok) {
      return NextResponse.json(
        { success: false, message: data.message || 'Lỗi đặt hàng tại Gateway' },
        { status: gatewayRes.status }
      );
    }

    return NextResponse.json(data);
  } catch (e: any) {
    console.error('Error proxying order creation:', e);
    return NextResponse.json(
      { success: false, message: e.message || 'Lỗi hệ thống khi chuyển tiếp yêu cầu đặt hàng.' },
      { status: 500 }
    );
  }
}

function logProxyRequest(key: string | null) {
  console.log(`[Next.js BFF] Proxying POST /orders to API Gateway. Idempotency-Key: ${key || 'NONE'}`);
}

import { NextResponse } from 'next/server';
import { mockOrders, idempotencyKeys, mockProducts } from '@/lib/mockDb';

export async function GET() {
  return NextResponse.json({
    success: true,
    message: 'Tải danh sách đơn hàng thành công (Mock)',
    data: {
      content: mockOrders,
      pageNumber: 0,
      pageSize: 10,
      totalElements: mockOrders.length,
      totalPages: 1,
      last: true,
    },
    timestamp: new Date().toISOString(),
  });
}

export async function POST(request: Request) {
  try {
    const idempotencyKey = request.headers.get('idempotency-key');
    if (idempotencyKey) {
      if (idempotencyKeys.has(idempotencyKey)) {
        return NextResponse.json(
          { success: false, message: 'Yêu cầu trùng lặp (Idempotency Key đã được xử lý).' },
          { status: 409 }
        );
      }
      idempotencyKeys.add(idempotencyKey);
    }

    const body = await request.json();
    if (!body.items || !Array.isArray(body.items) || body.items.length === 0) {
      return NextResponse.json(
        { success: false, message: 'Đơn hàng phải chứa ít nhất một sản phẩm.' },
        { status: 400 }
      );
    }

    let totalAmount = 0;
    const orderItems = body.items.map((item: any, index: number) => {
      const product = mockProducts.find(p => p.id === item.productId);
      const price = product ? product.price : 100000;
      const productName = product ? product.name : 'Sản phẩm không rõ';
      const qty = Number(item.quantity || 1);
      totalAmount += price * qty;
      
      return {
        id: index + 1,
        productId: item.productId,
        productName,
        quantity: qty,
        price,
      };
    });

    const newOrder = {
      id: 'ord-' + (mockOrders.length + 1),
      userId: 'b2d39f75-39c0-5d4f-9e0a-6f321e1a52d4',
      userEmail: 'user@sso.com',
      status: 'PENDING' as const,
      totalAmount,
      shippingAddress: body.shippingAddress || 'Chưa cung cấp',
      items: orderItems,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    mockOrders.unshift(newOrder); // Add to the top of list

    return NextResponse.json({
      success: true,
      message: 'Đặt hàng thành công (Mock)',
      data: newOrder,
      timestamp: new Date().toISOString(),
    });
  } catch (e: any) {
    return NextResponse.json(
      { success: false, message: e.message || 'Lỗi xử lý đặt hàng.' },
      { status: 500 }
    );
  }
}

import { NextResponse } from 'next/server';
import { mockOrders } from '@/lib/mockDb';

export async function GET(request: Request, props: { params: Promise<{ id: string }> }) {
  const params = await props.params;
  const order = mockOrders.find(o => o.id === params.id);
  if (!order) {
    return NextResponse.json(
      { success: false, message: 'Không tìm thấy đơn hàng.' },
      { status: 404 }
    );
  }

  return NextResponse.json({
    success: true,
    message: 'Tải chi tiết đơn hàng thành công (Mock)',
    data: order,
    timestamp: new Date().toISOString(),
  });
}

// Can be used to update status (e.g. CANCEL)
export async function PUT(request: Request, props: { params: Promise<{ id: string }> }) {
  const params = await props.params;
  try {
    const body = await request.json();
    const orderIdx = mockOrders.findIndex(o => o.id === params.id);
    if (orderIdx === -1) {
      return NextResponse.json(
        { success: false, message: 'Không tìm thấy đơn hàng.' },
        { status: 404 }
      );
    }

    if (body.status === 'CANCELLED') {
      if (mockOrders[orderIdx].status !== 'PENDING') {
        return NextResponse.json(
          { success: false, message: 'Chỉ có thể hủy đơn đặt hàng ở trạng thái PENDING.' },
          { status: 400 }
        );
      }
      mockOrders[orderIdx].status = 'CANCELLED';
    } else {
      mockOrders[orderIdx].status = body.status ?? mockOrders[orderIdx].status;
    }

    mockOrders[orderIdx].updatedAt = new Date().toISOString();

    return NextResponse.json({
      success: true,
      message: 'Cập nhật trạng thái đơn hàng thành công (Mock)',
      data: mockOrders[orderIdx],
      timestamp: new Date().toISOString(),
    });
  } catch (e: any) {
    return NextResponse.json(
      { success: false, message: e.message || 'Lỗi cập nhật đơn hàng.' },
      { status: 500 }
    );
  }
}

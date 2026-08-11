import { NextResponse } from 'next/server';
import { mockProducts } from '@/lib/mockDb';

export async function GET() {
  return NextResponse.json({
    success: true,
    message: 'Tải danh sách sản phẩm thành công (Mock)',
    data: {
      content: mockProducts,
      pageNumber: 0,
      pageSize: 10,
      totalElements: mockProducts.length,
      totalPages: 1,
      last: true,
    },
    timestamp: new Date().toISOString(),
  });
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    if (!body.name || !body.price) {
      return NextResponse.json(
        { success: false, message: 'Tên và giá sản phẩm là bắt buộc.' },
        { status: 400 }
      );
    }

    const newProduct = {
      id: 'p' + (mockProducts.length + 1),
      name: body.name,
      price: Number(body.price),
      stock: Number(body.stock || 0),
      category: body.category || 'Chung',
      imageUrl: body.imageUrl || '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    mockProducts.push(newProduct);

    return NextResponse.json({
      success: true,
      message: 'Thêm sản phẩm mới thành công (Mock)',
      data: newProduct,
      timestamp: new Date().toISOString(),
    });
  } catch (e: any) {
    return NextResponse.json(
      { success: false, message: e.message || 'Lỗi thêm sản phẩm.' },
      { status: 500 }
    );
  }
}

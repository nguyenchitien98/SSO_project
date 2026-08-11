import { NextResponse } from 'next/server';
import { mockProducts } from '@/lib/mockDb';

export async function GET(request: Request, props: { params: Promise<{ id: string }> }) {
  const params = await props.params;
  const product = mockProducts.find(p => p.id === params.id);
  if (!product) {
    return NextResponse.json(
      { success: false, message: 'Không tìm thấy sản phẩm.' },
      { status: 404 }
    );
  }

  return NextResponse.json({
    success: true,
    message: 'Tải chi tiết sản phẩm thành công (Mock)',
    data: product,
    timestamp: new Date().toISOString(),
  });
}

export async function PUT(request: Request, props: { params: Promise<{ id: string }> }) {
  const params = await props.params;
  try {
    const body = await request.json();
    const productIdx = mockProducts.findIndex(p => p.id === params.id);
    if (productIdx === -1) {
      return NextResponse.json(
        { success: false, message: 'Không tìm thấy sản phẩm.' },
        { status: 404 }
      );
    }

    mockProducts[productIdx] = {
      ...mockProducts[productIdx],
      name: body.name ?? mockProducts[productIdx].name,
      price: body.price !== undefined ? Number(body.price) : mockProducts[productIdx].price,
      stock: body.stock !== undefined ? Number(body.stock) : mockProducts[productIdx].stock,
      category: body.category ?? mockProducts[productIdx].category,
      imageUrl: body.imageUrl ?? mockProducts[productIdx].imageUrl,
      updatedAt: new Date().toISOString(),
    };

    return NextResponse.json({
      success: true,
      message: 'Cập nhật sản phẩm thành công (Mock)',
      data: mockProducts[productIdx],
      timestamp: new Date().toISOString(),
    });
  } catch (e: any) {
    return NextResponse.json(
      { success: false, message: e.message || 'Lỗi cập nhật sản phẩm.' },
      { status: 500 }
    );
  }
}

export async function DELETE(request: Request, props: { params: Promise<{ id: string }> }) {
  const params = await props.params;
  const productIdx = mockProducts.findIndex(p => p.id === params.id);
  if (productIdx === -1) {
    return NextResponse.json(
      { success: false, message: 'Không tìm thấy sản phẩm.' },
      { status: 404 }
    );
  }

  const deletedProduct = mockProducts.splice(productIdx, 1)[0];

  return NextResponse.json({
    success: true,
    message: 'Xóa sản phẩm thành công (Mock)',
    data: deletedProduct,
    timestamp: new Date().toISOString(),
  });
}

import { NextResponse } from 'next/server';
import { mockUsers } from '@/lib/mockDb';

export async function POST(request: Request, props: { params: Promise<{ id: string }> }) {
  const params = await props.params;
  try {
    const roles = await request.json();
    if (!Array.isArray(roles)) {
      return NextResponse.json(
        { success: false, message: 'Roles phải là một mảng tên vai trò.' },
        { status: 400 }
      );
    }

    const userIdx = mockUsers.findIndex(u => u.id === params.id);
    if (userIdx === -1) {
      return NextResponse.json(
        { success: false, message: 'Không tìm thấy người dùng.' },
        { status: 404 }
      );
    }

    mockUsers[userIdx].roles = roles;
    mockUsers[userIdx].updatedAt = new Date().toISOString();

    return NextResponse.json({
      success: true,
      message: 'Cập nhật vai trò người dùng thành công (Mock)',
      data: mockUsers[userIdx],
      timestamp: new Date().toISOString(),
    });
  } catch (e: any) {
    return NextResponse.json(
      { success: false, message: e.message || 'Lỗi cập nhật vai trò.' },
      { status: 500 }
    );
  }
}

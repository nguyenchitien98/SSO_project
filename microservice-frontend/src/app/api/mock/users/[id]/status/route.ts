import { NextResponse } from 'next/server';
import { mockUsers } from '@/lib/mockDb';

export async function PUT(request: Request, props: { params: Promise<{ id: string }> }) {
  const params = await props.params;
  try {
    const body = await request.json();
    const userIdx = mockUsers.findIndex(u => u.id === params.id);
    if (userIdx === -1) {
      return NextResponse.json(
        { success: false, message: 'Không tìm thấy người dùng.' },
        { status: 404 }
      );
    }

    mockUsers[userIdx].enabled = body.enabled ?? mockUsers[userIdx].enabled;
    mockUsers[userIdx].lockedReason = body.reason || null;
    mockUsers[userIdx].updatedAt = new Date().toISOString();

    return NextResponse.json({
      success: true,
      message: 'Cập nhật trạng thái người dùng thành công (Mock)',
      data: mockUsers[userIdx],
      timestamp: new Date().toISOString(),
    });
  } catch (e: any) {
    return NextResponse.json(
      { success: false, message: e.message || 'Lỗi cập nhật trạng thái.' },
      { status: 500 }
    );
  }
}

import { NextResponse } from 'next/server';
import { mockUsers } from '@/lib/mockDb';

export async function GET() {
  return NextResponse.json({
    success: true,
    message: 'Tải danh sách người dùng thành công (Mock)',
    data: {
      content: mockUsers,
      pageNumber: 0,
      pageSize: 10,
      totalElements: mockUsers.length,
      totalPages: 1,
      last: true,
    },
    timestamp: new Date().toISOString(),
  });
}

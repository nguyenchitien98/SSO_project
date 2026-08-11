import { NextResponse } from 'next/server';

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const code = body.code;

    // Accept 123456 or any 6 digit code for convenience in testing
    if (code === '123456' || (code && code.length === 6 && /^\d+$/.test(code))) {
      return NextResponse.json({
        success: true,
        message: 'Kích hoạt bảo mật 2 lớp thành công (Mock)',
        timestamp: new Date().toISOString(),
      });
    }

    return NextResponse.json(
      { success: false, message: 'Mã xác thực OTP không chính xác.' },
      { status: 400 }
    );
  } catch (e: any) {
    return NextResponse.json(
      { success: false, message: e.message || 'Lỗi kiểm tra OTP.' },
      { status: 500 }
    );
  }
}

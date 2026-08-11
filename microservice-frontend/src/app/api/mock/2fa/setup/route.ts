import { NextResponse } from 'next/server';

export async function POST() {
  const secretKey = 'NBSWY3DPEB3W64TBNQ'; // Mock Base32 Secret Key
  const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=otpauth://totp/SSO-Platform:user@sso.com?secret=${secretKey}%26issuer=SSO-Platform`;

  return NextResponse.json({
    success: true,
    message: 'Khởi tạo cấu hình 2FA thành công (Mock)',
    data: {
      secretKey,
      qrCodeUrl,
    },
    timestamp: new Date().toISOString(),
  });
}

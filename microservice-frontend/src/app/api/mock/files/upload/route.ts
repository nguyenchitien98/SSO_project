import { NextResponse } from 'next/server';

export async function POST(request: Request) {
  try {
    const formData = await request.formData();
    const file = formData.get('file') as File;

    if (!file) {
      return NextResponse.json(
        { success: false, message: 'Không tìm thấy file để upload.' },
        { status: 400 }
      );
    }

    // Generate a mock URL representing the object uploaded to MinIO
    const mockFileUrl = `https://api.dicebear.com/7.x/bottts/svg?seed=${encodeURIComponent(file.name)}`;

    return NextResponse.json({
      success: true,
      message: 'Upload file lên MinIO thành công (Mock)',
      data: {
        fileName: file.name,
        fileUrl: mockFileUrl,
        fileSize: file.size,
        contentType: file.type,
      },
      timestamp: new Date().toISOString(),
    });
  } catch (e: any) {
    return NextResponse.json(
      { success: false, message: e.message || 'Lỗi upload file.' },
      { status: 500 }
    );
  }
}

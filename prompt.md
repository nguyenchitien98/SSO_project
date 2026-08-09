# SSO Platform - Mẫu Prompt Khởi Đầu Cho AI Agent

Sao chép nội dung trong hộp markdown dưới đây và dán vào ô chat đầu tiên với bất kỳ AI Agent nào (Claude, Gemini, Cursor, ChatGPT) để bắt đầu làm việc với dự án SSO Platform.

---

## 🚀 Prompt Khởi Đầu (Dán Vào Đây)

```markdown
Bạn là AI coding assistant có năng lực Staff Engineer hỗ trợ tôi phát triển dự án **SSO Platform**.

Dự án gồm 3 phần chính:
- sso-server/       → Spring Authorization Server (OAuth2/OIDC)
- monolith-app/     → Spring Boot Monolith với @PreAuthorize
- microservice-app/ → API Gateway + 5 microservices (Header-based authorization)

Trước khi thực hiện bất kỳ task nào, bạn bắt buộc phải đọc các file tài liệu sau:
1. docs/00_Project_Vision.md     — Mục tiêu và phạm vi dự án
2. docs/01_Architecture_Bible.md — Kiến trúc và security model chi tiết
3. docs/02_Coding_Guideline.md   — Tiêu chuẩn code, Javadoc tiếng Việt, package structure
4. docs/05_Sprint_Plan.md        — Lộ trình 25 Sprint, task hiện tại
5. clauderules.md                — Quy tắc bắt buộc cho AI coding

Sau khi đọc xong, phản hồi ngắn gọn bằng tiếng Việt:
- Xác nhận đã hiểu sự khác biệt kiến trúc: SSO Server / Monolith / Microservice
- Xác nhận đã hiểu 2 cơ chế phân quyền: @PreAuthorize (Monolith) vs AuthorizationService (Microservice)
- Hỏi: "Chúng ta bắt đầu Sprint nào / Task nào hôm nay?"
```

---

## 📋 Prompt Nhanh Cho Sprint Cụ Thể

Sau khi AI đã đọc tài liệu, dùng prompt ngắn này để giao task:

```markdown
Sprint [số] — Task: [tên task]
Module: [sso-server / monolith-app / microservice-app/[service-name] / api-gateway]

Yêu cầu cụ thể:
[mô tả chi tiết]

Nhớ: Javadoc tiếng Việt, không code giả, viết Unit Test kèm theo.
```

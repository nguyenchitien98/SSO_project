# SSO Platform - Quy Chuẩn Định Dạng Code (Formatting & Linting)

Tài liệu này đặc tả quy tắc tự động định dạng code và kiểm tra lỗi cú pháp cho **Backend Java** và **Frontend Next.js/TypeScript** trong **SSO Platform**.

---

## 1. Backend Java — Spotless + Google Java Format

```xml
<!-- Trong parent pom.xml của dự án -->
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.43.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.17.0</version>
                <style>GOOGLE</style> <!-- Thụt lề 2 spaces -->
            </googleJavaFormat>
            <removeUnusedImports/>   <!-- Xóa import thừa -->
            <endWithNewline/>        <!-- Xuống dòng cuối file -->
            <trimTrailingWhitespace/>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>   <!-- Fail build nếu code chưa format -->
            </goals>
            <phase>compile</phase>
        </execution>
    </executions>
</plugin>
```

```bash
# Format tất cả Java files trước khi commit
mvn spotless:apply

# Chỉ check (không sửa) — dùng trong CI/CD
mvn spotless:check
```

---

## 2. Frontend — ESLint + Prettier + TypeScript Strict

### `.eslintrc.json`

```json
{
  "extends": [
    "next/core-web-vitals",
    "next/typescript",
    "plugin:@typescript-eslint/strict-type-checked",
    "plugin:@typescript-eslint/stylistic-type-checked"
  ],
  "parserOptions": {
    "project": true
  },
  "rules": {
    "@typescript-eslint/no-explicit-any": "error",
    "@typescript-eslint/no-unsafe-assignment": "error",
    "@typescript-eslint/no-unsafe-member-access": "error",
    "@typescript-eslint/no-unsafe-argument": "error",
    "@typescript-eslint/no-unsafe-return": "error",
    "@typescript-eslint/explicit-function-return-type": "warn",
    "@typescript-eslint/consistent-type-imports": ["error", { "prefer": "type-imports" }],
    "@typescript-eslint/no-unused-vars": ["error", { "argsIgnorePattern": "^_" }],
    "no-console": ["warn", { "allow": ["warn", "error"] }],
    "prefer-const": "error",
    "no-var": "error",
    "eqeqeq": ["error", "always"]
  }
}
```

### `.prettierrc`

```json
{
  "semi": true,
  "singleQuote": true,
  "trailingComma": "es5",
  "tabWidth": 2,
  "printWidth": 100,
  "arrowParens": "always",
  "endOfLine": "lf",
  "bracketSpacing": true,
  "jsxSingleQuote": false
}
```

### `package.json` scripts

```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "lint": "next lint",
    "lint:fix": "next lint --fix",
    "format": "prettier --write \"src/**/*.{ts,tsx,css,json}\"",
    "format:check": "prettier --check \"src/**/*.{ts,tsx,css,json}\"",
    "type-check": "tsc --noEmit"
  }
}
```

---

## 3. Git Pre-commit Hook (Husky + lint-staged)

```json
// package.json
{
  "lint-staged": {
    "src/**/*.{ts,tsx}": [
      "eslint --fix",
      "prettier --write"
    ],
    "src/**/*.{css,json,md}": [
      "prettier --write"
    ]
  }
}
```

```bash
# Setup
npx husky init
echo "npx lint-staged" > .husky/pre-commit
```

---

## 4. Quy Tắc Bắt Buộc Cho AI Agent

### Backend Java:
1. Thụt lề **2 spaces** (Google Java Format)
2. **Không** dùng wildcard imports (`import java.util.*` → cấm)
3. File kết thúc bằng newline
4. Line length tối đa 100 chars
5. Chạy `mvn spotless:apply` nếu phát hiện format lỗi

### Frontend TypeScript:
1. **Tuyệt đối không** dùng `any` — ESLint báo lỗi error, không phải warning
2. **Dùng `unknown`** cho dữ liệu không chắc chắn, sau đó type guard
3. Tất cả function phải có return type annotation (explicit)
4. Dùng `type` imports: `import type { Foo } from './foo'`
5. Không dùng `var` — chỉ dùng `const` và `let`
6. Props interface cho mọi component (không dùng object literal inline)

---

## 5. TypeScript Checklists Cho AI

```typescript
// ✅ ĐÚNG — Clean TypeScript

// 1. Explicit return type
async function getUser(id: string): Promise<UserProfile | null> { ... }

// 2. Typed props
interface ButtonProps {
  label: string;
  variant?: 'primary' | 'secondary' | 'danger';
  onClick: () => void;
  disabled?: boolean;
}

// 3. Type imports
import type { Product } from '@/types/product';
import type { ApiResponse } from '@/types/api';

// 4. Record thay vì { [key: string]: any }
const fieldErrors: Record<string, string> = {};

// 5. Discriminated union thay vì optional fields lẫn lộn
type ApiResult<T> =
  | { success: true; data: T }
  | { success: false; error: string; errorCode: string };
```

```typescript
// ❌ SAI — Bị ESLint chặn

const data: any = await fetch(...).then(r => r.json()); // no-explicit-any
function handleError(err: any) { }                       // no-explicit-any
const user = {} as any;                                   // no-explicit-any
var count = 0;                                            // no-var
let name = 'John';                                        // prefer-const nếu không reassign
```

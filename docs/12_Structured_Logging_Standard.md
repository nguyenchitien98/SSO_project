# SSO Platform - Tiêu Chuẩn Structured Logging

Tài liệu này đặc tả quy chuẩn ghi log tập trung cho toàn bộ **SSO Platform** — cả Backend Java và Frontend Next.js.

---

## 1. Backend Java — Logback Structured JSON Logging

### 1.1 Nguyên Tắc

1. **Không dùng `System.out.println`** — blocking I/O, không hỗ trợ log level
2. **Bắt buộc dùng `@Slf4j`** (Lombok) — không tự tạo Logger
3. **Log kèm context**: `userId`, `correlationId`, `orderId` trong mọi log có liên quan
4. **Log level đúng mục đích**:
   - `INFO`: Sự kiện bình thường quan trọng (login success, order created)
   - `WARN`: Bất thường nhưng hệ thống tự xử lý (brute force attempt, cache miss)
   - `ERROR`: Cần can thiệp (DB connection lost, payment gateway down)
   - `DEBUG`: Chỉ bật ở local dev, không commit `DEBUG` log trong production code

### 1.2 Pattern Log Chuẩn

```java
@Slf4j
@Service
public class AuthService {

    public AuthResponse login(LoginRequest request) {
        // ✅ ĐÚNG — Log có context, tránh log sensitive data
        log.info("Yêu cầu đăng nhập từ email: {}", maskEmail(request.email()));

        // ✅ ĐÚNG — Warn cho sự kiện bất thường
        log.warn("Đăng nhập thất bại lần {} cho user: {}",
            failCount, maskEmail(request.email()));

        // ✅ ĐÚNG — Error kèm exception (stacktrace)
        log.error("Lỗi kết nối database khi xác thực user: {}",
            maskEmail(request.email()), ex);

        // ❌ SAI — Log password
        log.info("User {} login với password: {}", email, password);

        // ❌ SAI — Dùng System.out
        System.out.println("Login: " + email);
    }

    private String maskEmail(String email) {
        // user@example.com → u***@example.com
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***@" + email.substring(atIndex + 1);
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
```

### 1.3 MDC — Correlation ID Tự Động

```java
/**
 * Filter tự động set correlationId vào MDC cho mỗi request.
 * Correlate tất cả log của một request xuyên suốt qua mọi service.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;

        String correlationId = httpReq.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put(MDC_KEY, correlationId);
        MDC.put("userId", Optional.ofNullable(httpReq.getHeader("X-User-Id")).orElse("anonymous"));
        MDC.put("service", System.getProperty("spring.application.name", "unknown"));

        httpRes.setHeader(CORRELATION_ID_HEADER, correlationId); // Trả về client

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear(); // Bắt buộc: tránh MDC leak sang request khác
        }
    }
}
```

### 1.4 Logback JSON Config (`logback-spring.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="appName"
        source="spring.application.name" defaultValue="sso-service"/>

    <!-- Dev: Console dễ đọc -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%-5level] [%X{correlationId}] [%X{userId}] %logger{30} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- Production: JSON format để Loki/ELK parse -->
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${appName}"}</customFields>
        </encoder>
    </appender>

    <!-- File rolling theo ngày -->
    <appender name="ROLLING_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/${appName}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/archived/${appName}-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%X{correlationId}] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <springProfile name="dev,default">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod,staging">
        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE"/>
            <appender-ref ref="ROLLING_FILE"/>
        </root>
    </springProfile>
</configuration>
```

---

## 2. Frontend Next.js — Structured Client-Side Logging

### 2.1 Không Dùng `console.log` Tùy Tiện

```typescript
// src/lib/logger.ts
type LogLevel = 'info' | 'warn' | 'error';

interface LogEntry {
  level: LogLevel;
  message: string;
  context?: Record<string, unknown>;
  timestamp: string;
}

/**
 * Structured logger cho Next.js frontend.
 * Production: chỉ warn và error (info bị suppress).
 * Dev: tất cả log levels.
 */
const isDev = process.env.NODE_ENV === 'development';

export const logger = {
  info: (message: string, context?: Record<string, unknown>): void => {
    if (!isDev) return;
    const entry: LogEntry = { level: 'info', message, context, timestamp: new Date().toISOString() };
    console.log(JSON.stringify(entry));
  },

  warn: (message: string, context?: Record<string, unknown>): void => {
    const entry: LogEntry = { level: 'warn', message, context, timestamp: new Date().toISOString() };
    console.warn(JSON.stringify(entry));
  },

  error: (message: string, error?: unknown, context?: Record<string, unknown>): void => {
    const entry: LogEntry = {
      level: 'error',
      message,
      context: {
        ...context,
        errorMessage: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
      },
      timestamp: new Date().toISOString(),
    };
    console.error(JSON.stringify(entry));
  },
};

// Sử dụng
logger.info('User navigated to products page', { userId: session.user.id });
logger.warn('API call retried', { endpoint: '/api/products', attempt: 2 });
logger.error('Failed to create order', err, { userId, productIds });
```

### 2.2 Error Boundary Logging

```tsx
// src/components/common/ErrorBoundary/ErrorBoundary.tsx
'use client';

import { Component, type ReactNode } from 'react';
import { logger } from '@/lib/logger';

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
}

/**
 * React Error Boundary bắt runtime errors trong component tree.
 * Log error tập trung và hiển thị fallback UI thay vì crash.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: { componentStack: string }): void {
    logger.error('React component error', error, {
      componentStack: info.componentStack,
    });
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return this.props.fallback ?? <p>Có lỗi xảy ra. Vui lòng tải lại trang.</p>;
    }
    return this.props.children;
  }
}
```

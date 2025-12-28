# =============================================================================
# Stage 1: Build Frontend (SvelteKit)
# =============================================================================
FROM node:20-alpine AS frontend-build
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install

COPY frontend/ .
RUN npm run build

# =============================================================================
# Stage 2: Build Backend (Spring Boot)
# =============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-build
WORKDIR /app

COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

COPY backend/src ./src
RUN mvn package -DskipTests -B

# =============================================================================
# Stage 3: Runtime - nginx + supervisord + both apps
# =============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install nginx, nodejs, npm, and supervisor
RUN apk add --no-cache nginx nodejs npm supervisor gettext

# Create necessary directories
RUN mkdir -p /run/nginx /var/log/supervisor /app/frontend /app/backend

# Copy backend JAR
COPY --from=backend-build /app/target/*.jar /app/backend/app.jar

# Copy frontend build
COPY --from=frontend-build /app/build /app/frontend/build
COPY --from=frontend-build /app/package.json /app/frontend/

# Create nginx config template (will be processed at runtime for PORT)
RUN cat > /etc/nginx/nginx.conf.template << 'EOF'
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /run/nginx/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent"';

    access_log /var/log/nginx/access.log main;
    sendfile on;
    keepalive_timeout 65;

    # Increase header buffer sizes significantly to prevent "Request Header Or Cookie Too Large" errors
    # These large values ensure nginx can read the full request before deciding to reject it
    # This allows the @request_too_large handler to send Set-Cookie headers to clear cookies
    client_header_buffer_size 16k;
    large_client_header_buffers 16 32k;

    # Also set max body size
    client_max_body_size 10m;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    upstream backend {
        server 127.0.0.1:8081 max_fails=0;
    }

    upstream frontend {
        server 127.0.0.1:3000 max_fails=0;
    }

    server {
        listen ${PORT};
        server_name _;

        # Security headers
        add_header X-Frame-Options "DENY" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header X-XSS-Protection "1; mode=block" always;
        add_header Permissions-Policy "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()" always;
        add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'self'; object-src 'none'" always;

        # Simple health endpoint - nginx itself responds immediately
        # This is the primary Railway health check endpoint
        # Must return 200 right away for Railway replica health checks
        location = /health {
            access_log off;
            default_type application/json;
            return 200 '{"status":"UP","nginx":"healthy","message":"proxy ready"}';
        }

        # Readiness check - verifies backend is also ready
        location = /ready {
            access_log off;
            default_type application/json;
            proxy_pass http://backend/actuator/health;
            proxy_connect_timeout 2s;
            proxy_read_timeout 5s;
            error_page 502 503 504 = @backend_not_ready;
        }

        location @backend_not_ready {
            default_type application/json;
            return 503 '{"status":"STARTING","message":"backend initializing"}';
        }

        # Special endpoint to clear session cookies - handles large header cases
        # This endpoint responds directly from nginx with Set-Cookie headers to clear JSESSIONID
        # This is the fallback when headers are too large to reach the backend
        location = /api/auth/clear-session-fallback {
            default_type application/json;

            # Clear JSESSIONID cookies on ALL possible paths
            add_header Set-Cookie "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" always;

            return 200 '{"message":"Session cleared successfully","details":"Cookies have been cleared by nginx fallback. Please try logging in again.","timestamp":"$time_iso8601","fallback":true}';
        }

        # Static HTML page to clear cookies when JavaScript/SvelteKit can't help
        # This works even when the main app is inaccessible due to large headers
        location = /clear-cookies {
            default_type text/html;

            # Clear all session cookies on every possible path
            add_header Set-Cookie "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Cache-Control "no-cache, no-store, must-revalidate" always;

            return 200 '<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Session Cleared - BPM Demo</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: system-ui, -apple-system, sans-serif; background: linear-gradient(135deg, #eff6ff 0%, #eef2ff 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 1rem; }
        .card { background: white; border-radius: 1rem; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); padding: 2rem; max-width: 28rem; width: 100%; text-align: center; }
        .icon { width: 4rem; height: 4rem; background: #22c55e; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.5rem; }
        .icon svg { width: 2rem; height: 2rem; color: white; }
        h1 { color: #1f2937; font-size: 1.5rem; margin-bottom: 0.5rem; }
        p { color: #6b7280; margin-bottom: 1.5rem; }
        .btn { display: inline-block; background: #2563eb; color: white; padding: 0.75rem 1.5rem; border-radius: 0.5rem; text-decoration: none; font-weight: 500; transition: background 0.2s; }
        .btn:hover { background: #1d4ed8; }
        .note { font-size: 0.875rem; color: #9ca3af; margin-top: 1rem; }
    </style>
</head>
<body>
    <div class="card">
        <div class="icon">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
        </div>
        <h1>Session Cleared Successfully</h1>
        <p>Your browser cookies and session data have been cleared. You can now return to the login page.</p>
        <a href="/login" class="btn">Go to Login</a>
        <p class="note">If you still see errors, try using an incognito/private browser window.</p>
    </div>
</body>
</html>';
        }

        # API requests -> Spring Boot backend
        location /api/ {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header Cookie $http_cookie;
            proxy_pass_header Set-Cookie;
            proxy_connect_timeout 10s;
            proxy_read_timeout 60s;

            # Return friendly error when backend is starting
            error_page 502 = @api_starting;

            # Return JSON for request header/cookie too large errors
            error_page 400 = @request_too_large;
            error_page 413 = @request_too_large;
            error_page 431 = @request_too_large;
        }

        # Handle request header/cookie too large errors with JSON response
        # Also include Set-Cookie headers to clear JSESSIONID automatically
        location @request_too_large {
            default_type application/json;

            # Automatically clear ALL session cookies when this error occurs
            add_header Set-Cookie "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Cache-Control "no-cache, no-store, must-revalidate" always;

            return 400 '{"error":"Request headers too large","message":"Your browser has sent request headers that are too large","details":"Session cookies have been automatically cleared. Please refresh the page and try again. If the problem persists, visit /clear-cookies directly.","status":400,"cookiesCleared":true,"clearCookiesUrl":"/clear-cookies"}';
        }

        location @api_starting {
            default_type application/json;
            return 503 '{"error":"Service starting","message":"Backend is initializing, please retry in a few seconds"}';
        }

        # Actuator endpoints -> Spring Boot backend
        location /actuator/ {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_connect_timeout 10s;
            proxy_read_timeout 30s;
        }

        # H2 Console -> Spring Boot backend (for dev)
        location /h2-console/ {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # Everything else -> SvelteKit frontend
        location / {
            proxy_pass http://frontend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection 'upgrade';
            proxy_cache_bypass $http_upgrade;

            # Increase proxy buffer sizes to handle large headers from frontend
            proxy_buffer_size 32k;
            proxy_buffers 8 32k;
            proxy_busy_buffers_size 64k;

            # Return HTML error page for request header/cookie too large errors
            # This redirects to clear-cookies page which works without JS
            error_page 400 = @frontend_too_large;
            error_page 413 = @frontend_too_large;
            error_page 431 = @frontend_too_large;
        }

        # Special error handler for frontend that shows HTML with redirect
        location @frontend_too_large {
            default_type text/html;

            # Clear cookies immediately
            add_header Set-Cookie "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Set-Cookie "SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" always;
            add_header Cache-Control "no-cache, no-store, must-revalidate" always;

            return 400 '<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="refresh" content="2;url=/clear-cookies">
    <title>Clearing Session - BPM Demo</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: system-ui, -apple-system, sans-serif; background: linear-gradient(135deg, #eff6ff 0%, #eef2ff 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 1rem; }
        .card { background: white; border-radius: 1rem; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); padding: 2rem; max-width: 28rem; width: 100%; text-align: center; }
        .spinner { width: 3rem; height: 3rem; border: 4px solid #e5e7eb; border-top-color: #2563eb; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 1.5rem; }
        @keyframes spin { to { transform: rotate(360deg); } }
        h1 { color: #1f2937; font-size: 1.25rem; margin-bottom: 0.5rem; }
        p { color: #6b7280; margin-bottom: 1rem; }
        a { color: #2563eb; text-decoration: underline; }
    </style>
</head>
<body>
    <div class="card">
        <div class="spinner"></div>
        <h1>Session Cookies Too Large</h1>
        <p>Clearing your session cookies automatically...</p>
        <p>If you are not redirected, <a href="/clear-cookies">click here</a>.</p>
    </div>
</body>
</html>';
        }
    }
}
EOF

# Create supervisor config
RUN cat > /etc/supervisord.conf << 'EOF'
[supervisord]
nodaemon=true
logfile=/var/log/supervisor/supervisord.log
pidfile=/run/supervisord.pid
user=root

[program:nginx]
command=/app/start-nginx.sh
autostart=true
autorestart=true
stdout_logfile=/dev/stdout
stdout_logfile_maxbytes=0
stderr_logfile=/dev/stderr
stderr_logfile_maxbytes=0
startsecs=0
priority=10

[program:backend]
command=java -XX:+UseContainerSupport -Xms32m -Xmx128m -XX:MaxMetaspaceSize=64m -XX:CompressedClassSpaceSize=24m -XX:ReservedCodeCacheSize=24m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss256k -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Dspring.jmx.enabled=false -Dserver.address=0.0.0.0 -Dserver.port=8081 -jar /app/backend/app.jar
directory=/app/backend
autostart=true
autorestart=true
stdout_logfile=/dev/stdout
stdout_logfile_maxbytes=0
stderr_logfile=/dev/stderr
stderr_logfile_maxbytes=0
priority=100

[program:frontend]
command=/app/start-frontend.sh
directory=/app/frontend
environment=NODE_ENV="production",PORT="3000"
autostart=true
autorestart=true
stdout_logfile=/dev/stdout
stdout_logfile_maxbytes=0
stderr_logfile=/dev/stderr
stderr_logfile_maxbytes=0
priority=100
EOF

# Create nginx wrapper script - starts immediately for health checks
# Railway health checks need nginx to respond right away
RUN cat > /app/start-nginx.sh << 'EOF'
#!/bin/sh
echo "Starting nginx immediately for health checks..."
echo "Backend will be available once Spring Boot finishes starting."

# Start nginx immediately - health checks must respond right away
# The /health endpoint responds with nginx status
# The /api/ endpoints will return 502 until backend is ready (expected behavior)
exec /usr/sbin/nginx -g "daemon off;"
EOF

RUN chmod +x /app/start-nginx.sh

# Create frontend wrapper script to set ORIGIN dynamically
RUN cat > /app/start-frontend.sh << 'EOF'
#!/bin/sh
# Set ORIGIN dynamically from Railway environment or fallback
if [ -n "$RAILWAY_PUBLIC_DOMAIN" ]; then
    export ORIGIN="https://${RAILWAY_PUBLIC_DOMAIN}"
elif [ -n "$RAILWAY_STATIC_URL" ]; then
    export ORIGIN="${RAILWAY_STATIC_URL}"
else
    export ORIGIN="http://localhost:3000"
fi
echo "Starting frontend with ORIGIN=$ORIGIN"
exec node /app/frontend/build
EOF

RUN chmod +x /app/start-frontend.sh

# Create startup script
RUN cat > /app/start.sh << 'EOF'
#!/bin/sh
set -e

# Default PORT if not set
export PORT=${PORT:-8080}

# Generate nginx config with the correct PORT
envsubst '${PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

echo "Starting services on port $PORT..."
echo "  - nginx (reverse proxy) on port $PORT"
echo "  - backend (Spring Boot) on port 8081"
echo "  - frontend (SvelteKit) on port 3000"

# Start supervisor which manages all processes
exec /usr/bin/supervisord -c /etc/supervisord.conf
EOF

RUN chmod +x /app/start.sh

# Expose default port (Railway will override with PORT env var)
EXPOSE 8080

CMD ["/app/start.sh"]

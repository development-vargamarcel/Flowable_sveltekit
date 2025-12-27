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

    # Increase header buffer sizes to prevent "Request Header Or Cookie Too Large" errors
    client_header_buffer_size 4k;
    large_client_header_buffers 8 16k;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    upstream backend {
        server 127.0.0.1:8080 max_fails=0;
    }

    upstream frontend {
        server 127.0.0.1:3000 max_fails=0;
    }

    server {
        listen ${PORT};
        server_name _;

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

            # Clear JSESSIONID cookies on multiple paths
            add_header Set-Cookie "JSESSIONID=; Path=/; Max-Age=0; HttpOnly" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api; Max-Age=0; HttpOnly" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api/; Max-Age=0; HttpOnly" always;

            return 200 '{"message":"Session cleared successfully","details":"Cookies have been cleared by nginx fallback. Please try logging in again.","timestamp":"$time_iso8601","fallback":true}';
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

            # Automatically clear JSESSIONID cookies when this error occurs
            add_header Set-Cookie "JSESSIONID=; Path=/; Max-Age=0; HttpOnly" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api; Max-Age=0; HttpOnly" always;
            add_header Set-Cookie "JSESSIONID=; Path=/api/; Max-Age=0; HttpOnly" always;

            return 400 '{"error":"Request headers too large","message":"Your browser has sent request headers that are too large","details":"Session cookies have been automatically cleared. Please refresh the page and try again.","status":400,"cookiesCleared":true}';
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

            # Return JSON for request header/cookie too large errors
            error_page 400 = @request_too_large;
            error_page 413 = @request_too_large;
            error_page 431 = @request_too_large;
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
command=java -XX:+UseContainerSupport -XX:MaxRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom -Dspring.jmx.enabled=false -Dserver.address=0.0.0.0 -Dserver.port=8080 -jar /app/backend/app.jar
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
echo "  - backend (Spring Boot) on port 8080"
echo "  - frontend (SvelteKit) on port 3000"

# Start supervisor which manages all processes
exec /usr/bin/supervisord -c /etc/supervisord.conf
EOF

RUN chmod +x /app/start.sh

# Expose default port (Railway will override with PORT env var)
EXPOSE 8080

CMD ["/app/start.sh"]

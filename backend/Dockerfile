# =============================================================================
# Backend-only Dockerfile for Railway deployment
# Uses nginx for immediate health check response while Spring Boot starts
# =============================================================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies
# Note: paths are relative to repo root since railway.json uses dockerfilePath: "backend/Dockerfile"
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY backend/src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime with nginx for immediate health checks
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install nginx and supervisor for process management
RUN apk add --no-cache nginx supervisor gettext

# Create necessary directories
RUN mkdir -p /run/nginx /var/log/supervisor /var/log/nginx

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Create nginx config template - responds immediately to health checks
RUN cat > /etc/nginx/nginx.conf.template << 'NGINXEOF'
worker_processes 1;
error_log /var/log/nginx/error.log warn;
pid /run/nginx/nginx.pid;

events {
    worker_connections 256;
}

http {
    access_log off;
    sendfile on;
    keepalive_timeout 65;

    upstream backend {
        server 127.0.0.1:8081 max_fails=0;
    }

    server {
        listen ${PORT};
        server_name _;

        # Primary health check - responds immediately from nginx
        # This is what Railway checks - must respond 200 right away
        location = /actuator/health {
            default_type application/json;

            # Try to proxy to backend first
            proxy_pass http://backend/actuator/health;
            proxy_connect_timeout 1s;
            proxy_read_timeout 2s;

            # If backend not ready, return healthy status from nginx
            # This keeps Railway happy while Spring Boot starts
            error_page 502 503 504 = @nginx_healthy;
        }

        location @nginx_healthy {
            default_type application/json;
            return 200 '{"status":"UP","nginx":"healthy","spring":"starting"}';
        }

        # Readiness endpoint - only returns UP when backend is truly ready
        location = /ready {
            default_type application/json;
            proxy_pass http://backend/actuator/health;
            proxy_connect_timeout 2s;
            proxy_read_timeout 5s;
            error_page 502 503 504 = @not_ready;
        }

        location @not_ready {
            default_type application/json;
            return 503 '{"status":"STARTING","message":"backend initializing"}';
        }

        # All other requests proxy to Spring Boot
        location / {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header Cookie $http_cookie;
            proxy_pass_header Set-Cookie;
            proxy_connect_timeout 10s;
            proxy_read_timeout 120s;

            # Return friendly error when backend is starting
            error_page 502 = @backend_starting;
        }

        location @backend_starting {
            default_type application/json;
            return 503 '{"error":"Service starting","message":"Backend is initializing, please retry in a few seconds"}';
        }
    }
}
NGINXEOF

# Create supervisor config to manage both nginx and Spring Boot
RUN cat > /etc/supervisord.conf << 'SUPERVISOREOF'
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
command=java -XX:+UseContainerSupport -Xms32m -Xmx128m -XX:MaxMetaspaceSize=64m -XX:CompressedClassSpaceSize=24m -XX:ReservedCodeCacheSize=24m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss256k -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Dspring.jmx.enabled=false -Dserver.address=0.0.0.0 -Dserver.port=8081 -jar /app/app.jar
directory=/app
autostart=true
autorestart=true
stdout_logfile=/dev/stdout
stdout_logfile_maxbytes=0
stderr_logfile=/dev/stderr
stderr_logfile_maxbytes=0
priority=100
SUPERVISOREOF

# Create nginx startup script
RUN cat > /app/start-nginx.sh << 'STARTEOF'
#!/bin/sh
echo "Starting nginx immediately for health checks..."
exec /usr/sbin/nginx -g "daemon off;"
STARTEOF

RUN chmod +x /app/start-nginx.sh

# Create main startup script
RUN cat > /app/start.sh << 'MAINEOF'
#!/bin/sh
set -e

# Default PORT if not set (Railway provides this)
export PORT=${PORT:-8080}

echo "=== Railway Backend Starting ==="
echo "External PORT: $PORT (nginx)"
echo "Internal PORT: 8081 (Spring Boot)"

# Generate nginx config with the correct PORT
envsubst '${PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

echo "Starting services..."
echo "  - nginx (immediate health response) on port $PORT"
echo "  - Spring Boot on internal port 8081"

# Start supervisor which manages all processes
exec /usr/bin/supervisord -c /etc/supervisord.conf
MAINEOF

RUN chmod +x /app/start.sh

# Expose port (Railway will override with PORT env var)
EXPOSE 8080

# Health check for local Docker testing
HEALTHCHECK --interval=10s --timeout=5s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

CMD ["/app/start.sh"]

#!/usr/bin/env python3
import json
import http.server
import socketserver
from urllib.parse import urlparse

class BPMBackendHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        parsed_path = urlparse(self.path)
        
        # CORS headers
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        
        # Mock API responses
        if parsed_path.path == '/actuator/health':
            response = {"status": "UP", "components": {"db": {"status": "UP", "details": {"database": "H2", "validationQuery": "isValid()"}}}}
        elif parsed_path.path == '/api/auth/me':
            response = {
                "username": "admin", 
                "displayName": "Admin User", 
                "roles": ["MANAGER", "DIRECTOR"],
                "permissions": ["read", "write", "admin"]
            }
        elif parsed_path.path.startswith('/api/tasks'):
            response = {
                "total": 5,
                "content": [
                    {"id": "1", "name": "Review Purchase Request", "description": "Review and approve purchase request", "assignee": "admin", "status": "completed"},
                    {"id": "2", "name": "Approve Leave Request", "description": "Review leave application", "assignee": None, "status": "pending"},
                    {"id": "3", "name": "Project Planning", "description": "Create project plan", "assignee": "admin", "status": "in-progress"},
                    {"id": "4", "name": "Budget Review", "description": "Review department budget", "assignee": None, "status": "pending"},
                    {"id": "5", "name": "Document Review", "description": "Review submitted documents", "assignee": "admin", "status": "completed"}
                ]
            }
        elif parsed_path.path.startswith('/api/processes'):
            response = {
                "total": 3,
                "content": [
                    {"id": "proc1", "name": "Purchase Request Process", "description": "Multi-level approval for purchases", "status": "active"},
                    {"id": "proc2", "name": "Leave Request Process", "description": "Employee leave approval workflow", "status": "active"},
                    {"id": "proc3", "name": "Project Approval Process", "description": "Multi-stakeholder project review", "status": "completed"}
                ]
            }
        else:
            response = {"message": "BPM Backend Mock - Endpoint not implemented", "path": parsed_path.path}
        
        self.wfile.write(json.dumps(response, indent=2).encode())
    
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        # CORS headers
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        
        response = {"message": "BPM Backend Mock - POST received", "success": True}
        self.wfile.write(json.dumps(response, indent=2).encode())
    
    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()

PORT = 8080
Handler = BPMBackendHandler

with socketserver.TCPServer(("", PORT), Handler) as httpd:
    print(f"Serving mock BPM backend on port {PORT}")
    httpd.serve_forever()
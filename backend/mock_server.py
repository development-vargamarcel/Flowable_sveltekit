#!/usr/bin/env python3
import json
import http.server
import socketserver
from urllib.parse import urlparse, parse_qs

class BPMBackendHandler(http.server.SimpleHTTPRequestHandler):
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
        elif parsed_path.path.endswith('/comments'):
            # Task comments
            taskId = parsed_path.path.split('/')[-2]
            response = [
                {"id": "c1", "message": "Can you please clarify the budget?", "authorId": "user2", "timestamp": "2024-02-01T10:00:00Z"},
                {"id": "c2", "message": "Budget is attached in the documents.", "authorId": "admin", "timestamp": "2024-02-01T10:30:00Z"}
            ]
        elif parsed_path.path.endswith('/documents'):
            # Task documents
            taskId = parsed_path.path.split('/')[-2]
            if self.command == 'GET':
                response = [
                    {"id": "d1", "name": "invoice_feb.pdf", "type": "application/pdf", "size": 102400, "createdBy": "user1", "createdAt": "2024-02-01T09:00:00Z"},
                    {"id": "d2", "name": "specs_v2.docx", "type": "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "size": 2048000, "createdBy": "admin", "createdAt": "2024-02-01T11:00:00Z"}
                ]
            elif self.command == 'POST':
                response = {"id": "d_new_" + str(parsed_path.path), "name": "uploaded_file.png", "type": "image/png", "size": 5000, "createdBy": "me", "createdAt": "2024-02-01T12:00:00Z"}
            elif self.command == 'DELETE':
                 response = {"message": "Document deleted"}
        elif parsed_path.path.endswith('/history'):
            # Task history/audit log
            taskId = parsed_path.path.split('/')[-2]
            response = [
                {"id": "h1", "taskId": taskId, "type": "CREATED", "userId": "system", "userName": "System", "timestamp": "2024-02-01T09:00:00Z", "details": "Task created"},
                {"id": "h2", "taskId": taskId, "type": "ASSIGNED", "userId": "manager1", "userName": "Manager", "timestamp": "2024-02-01T09:05:00Z", "details": "Assigned to user"},
                {"id": "h3", "taskId": taskId, "type": "COMMENT", "userId": "user2", "userName": "User Two", "timestamp": "2024-02-01T10:00:00Z", "details": "Added a comment"},
                {"id": "h4", "taskId": taskId, "type": "DOCUMENT_UPLOAD", "userId": "user1", "userName": "User One", "timestamp": "2024-02-01T11:00:00Z", "details": "Uploaded invoice_feb.pdf"}
            ]
        elif parsed_path.path.startswith('/api/tasks'):
            response = {
                "total": 5,
                "content": [
                    {"id": "1", "name": "Review Purchase Request", "description": "Review and approve purchase request", "assignee": "admin", "status": "completed"},
                    {"id": "2", "name": "Approve Leave Request", "description": "Review leave application", "assignee": null, "status": "pending"},
                    {"id": "3", "name": "Project Planning", "description": "Create project plan", "assignee": "admin", "status": "in-progress"},
                    {"id": "4", "name": "Budget Review", "description": "Review department budget", "assignee": null, "status": "pending"},
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
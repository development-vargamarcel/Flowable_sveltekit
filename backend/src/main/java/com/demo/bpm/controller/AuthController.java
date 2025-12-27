package com.demo.bpm.controller;

import com.demo.bpm.dto.LoginRequest;
import com.demo.bpm.dto.UserDTO;
import com.demo.bpm.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.getUsername();
        String clientIp = getClientIpAddress(httpRequest);

        log.info("Login attempt for user '{}' from IP: {}", username, clientIp);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UserDTO user = userService.getUserInfo(userDetails);

            log.info("User '{}' logged in successfully from IP: {}, session: {}",
                    username, clientIp, session.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "user", user
            ));
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user '{}' from IP {}: Bad credentials", username, clientIp);
            return ResponseEntity.status(401).body(buildErrorResponse(
                    "Invalid username or password",
                    "Please check your credentials and try again"
            ));
        } catch (UsernameNotFoundException e) {
            log.warn("Login failed for user '{}' from IP {}: User not found", username, clientIp);
            // Return same message as bad credentials to prevent user enumeration
            return ResponseEntity.status(401).body(buildErrorResponse(
                    "Invalid username or password",
                    "Please check your credentials and try again"
            ));
        } catch (DisabledException e) {
            log.warn("Login failed for user '{}' from IP {}: Account disabled", username, clientIp);
            return ResponseEntity.status(401).body(buildErrorResponse(
                    "Account disabled",
                    "Your account has been disabled. Please contact an administrator."
            ));
        } catch (LockedException e) {
            log.warn("Login failed for user '{}' from IP {}: Account locked", username, clientIp);
            return ResponseEntity.status(401).body(buildErrorResponse(
                    "Account locked",
                    "Your account has been locked. Please try again later or contact an administrator."
            ));
        } catch (AuthenticationException e) {
            log.warn("Login failed for user '{}' from IP {}: {} - {}",
                    username, clientIp, e.getClass().getSimpleName(), e.getMessage());
            return ResponseEntity.status(401).body(buildErrorResponse(
                    "Authentication failed",
                    e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error during login for user '{}' from IP {}: {} - {}",
                    username, clientIp, e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(500).body(buildErrorResponse(
                    "Login failed",
                    "An unexpected error occurred. Please try again later."
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        HttpSession session = request.getSession(false);

        if (session != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : "unknown";
            log.info("User '{}' logging out from IP: {}, session: {}", username, clientIp, session.getId());
            session.invalidate();
        } else {
            log.debug("Logout requested from IP {} but no active session found", clientIp);
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully",
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Clear all session cookies to resolve "Request Header Or Cookie Too Large" errors.
     * This endpoint clears HttpOnly cookies that JavaScript cannot access.
     */
    @PostMapping("/clear-session")
    public ResponseEntity<?> clearSession(HttpServletRequest request, HttpServletResponse response) {
        String clientIp = getClientIpAddress(request);
        log.info("Session clear requested from IP: {}", clientIp);

        // Invalidate any existing session
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.debug("Invalidating session: {}", session.getId());
            session.invalidate();
        }

        // Clear Spring Security context
        SecurityContextHolder.clearContext();

        // Explicitly clear the JSESSIONID cookie by setting it with maxAge=0
        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(0);
        sessionCookie.setHttpOnly(true);
        response.addCookie(sessionCookie);

        // Also clear with /api path in case it was set there
        Cookie apiSessionCookie = new Cookie("JSESSIONID", "");
        apiSessionCookie.setPath("/api");
        apiSessionCookie.setMaxAge(0);
        apiSessionCookie.setHttpOnly(true);
        response.addCookie(apiSessionCookie);

        log.info("Session and cookies cleared for IP: {}", clientIp);

        return ResponseEntity.ok(Map.of(
                "message", "Session cleared successfully",
                "details", "All session cookies have been removed. You can now try logging in again.",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
        if (userDetails == null) {
            String clientIp = getClientIpAddress(request);
            log.debug("Unauthenticated request to /me from IP: {}", clientIp);
            return ResponseEntity.status(401).body(buildErrorResponse(
                    "Not authenticated",
                    "Please log in to access this resource"
            ));
        }

        try {
            UserDTO user = userService.getUserInfo(userDetails);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error fetching user info for '{}': {}", userDetails.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(500).body(buildErrorResponse(
                    "Error fetching user info",
                    "Could not retrieve user information. Please try again."
            ));
        }
    }

    /**
     * Build a consistent error response
     */
    private Map<String, Object> buildErrorResponse(String error, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", Instant.now().toString());
        return response;
    }

    /**
     * Extract client IP address from request, considering proxies
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}

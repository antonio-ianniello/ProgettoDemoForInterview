package com.example.usermanagement.config.audit;

import com.example.usermanagement.model.AccessLog;
import com.example.usermanagement.service.AccessLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogFilter.class);

    private final AccessLogService accessLogService;

    public AccessLogFilter(AccessLogService accessLogService) {
        this.accessLogService = accessLogService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            int statusCode = responseWrapper.getStatus();
            String method = request.getMethod();
            String path = request.getRequestURI();
            String ipAddress = resolveClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String username = resolveUsername();

            logAccess(method, path, statusCode, username, ipAddress);
            persistAccessLog(method, path, statusCode, username, ipAddress, userAgent);

            responseWrapper.copyBodyToResponse();
        }
    }

    private void logAccess(String method, String path, int statusCode, String username, String ipAddress) {
        if (statusCode == 401) {
            LOGGER.warn("UNAUTHORIZED attempt: {} {} from ip={} — no valid token", method, path, ipAddress);
        } else if (statusCode == 403) {
            LOGGER.warn("FORBIDDEN attempt: {} {} from ip={} user={} — insufficient permissions", method, path, ipAddress, username);
        } else {
            LOGGER.info("ACCESS: {} {} status={} user={} ip={}", method, path, statusCode, username, ipAddress);
        }
    }

    private void persistAccessLog(String method, String path, int statusCode, String username, String ipAddress, String userAgent) {
        AccessLog log = new AccessLog();
        log.setMethod(method);
        log.setPath(path);
        log.setStatusCode(statusCode);
        log.setUsername(username);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent != null && userAgent.length() > 500
            ? userAgent.substring(0, 500)
            : userAgent);
        accessLogService.save(log);
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        // Evita di loggare "anonymousUser" come username
        return "anonymousUser".equals(name) ? null : name;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Prende il primo IP della catena (quello originale del client)
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs");
    }
}

package com.quradar.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter limiter;
    private final int eventsPerMinute;
    private final int authPerMinute;

    public RateLimitInterceptor(RateLimiter limiter,
                                @Value("${quradar.rate-limit.events-per-minute:60}") int eventsPerMinute,
                                @Value("${quradar.rate-limit.auth-per-minute:10}") int authPerMinute) {
        this.limiter = limiter;
        this.eventsPerMinute = eventsPerMinute;
        this.authPerMinute = authPerMinute;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth")) {
            if (!limiter.allow("rl:auth:" + request.getRemoteAddr(), authPerMinute, 60)) {
                throw new RateLimitExceededException();
            }
        } else if (path.startsWith("/api/v1/events")) {
            if (!limiter.allow("rl:events:" + caller(request), eventsPerMinute, 60)) {
                throw new RateLimitExceededException();
            }
        }
        return true;
    }

    private static String caller(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return request.getRemoteAddr();
    }
}

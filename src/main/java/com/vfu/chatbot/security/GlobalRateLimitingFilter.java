package com.vfu.chatbot.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component("resilience4jFilter")
public class GlobalRateLimitingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        RateLimiter rateLimiter = RateLimiter.ofDefaults("default");
        if (rateLimiter.acquirePermission()) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).setStatus(429);
            ((HttpServletResponse) response).setHeader("Retry-After", "30");
            response.getWriter().write("{\"error\":\"Global rate limit exceeded\"}");
        }
    }
}


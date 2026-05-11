package com.example.AavieApp.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter implements Filter {
    
    @Value("${rate.limit.max-requests:100}")
    private int maxRequests;
    
    @Value("${rate.limit.time-window-minutes:1}")
    private long timeWindowMinutes;
    
    private final Map<String, RequestCount> requestCounts = new ConcurrentHashMap<>();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String clientIp = getClientIP(httpRequest);
        
        if (isRateLimited(clientIp)) {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\": \"Rate limit exceeded\", \"message\": \"Please try again later.\"}"
            );
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean isRateLimited(String clientIp) {
        long currentTime = System.currentTimeMillis();
        long windowMillis = timeWindowMinutes * 60 * 1000;
        
        RequestCount count = requestCounts.computeIfAbsent(clientIp, 
            k -> new RequestCount(currentTime, 1));
        
        synchronized (count) {
            if (currentTime - count.firstRequestTime > windowMillis) {
                count.firstRequestTime = currentTime;
                count.count = 1;
                return false;
            }
            
            count.count++;
            return count.count > maxRequests;
        }
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
    
    private static class RequestCount {
        long firstRequestTime;
        int count;
        
        RequestCount(long firstRequestTime, int count) {
            this.firstRequestTime = firstRequestTime;
            this.count = count;
        }
    }
}
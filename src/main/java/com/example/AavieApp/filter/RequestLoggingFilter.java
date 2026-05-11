package com.example.AavieApp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Generate unique request ID
        String requestId = UUID.randomUUID().toString();
        request.setAttribute("requestId", requestId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Log request
            logger.info("Request [{}] {} {} from IP: {}", 
                requestId, 
                request.getMethod(), 
                request.getRequestURI(),
                getClientIP(request));
            
            filterChain.doFilter(request, response);
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log response
            logger.info("Response [{}] Status: {} Duration: {}ms", 
                requestId, 
                response.getStatus(), 
                duration);
        }
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
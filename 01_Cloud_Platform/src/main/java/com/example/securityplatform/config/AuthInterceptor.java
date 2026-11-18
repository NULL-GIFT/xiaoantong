package com.example.securityplatform.config;

import com.example.securityplatform.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Claims c = jwtUtil.parseClaims(token);
                request.setAttribute("authClaims", c);
                return true;
            } catch (Exception e) {
                unauthorized(response);
                return false;
            }
        }
        unauthorized(response);
        return false;
    }

    private void unauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        byte[] body = "{\"code\":401,\"msg\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(body);
    }
}

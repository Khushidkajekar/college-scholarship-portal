package com.college.scholarship;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CsrfGuard extends OncePerRequestFilter {
    private static final SecureRandom RANDOM=new SecureRandom();
    static String issue(HttpSession session){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);session.setAttribute("csrfToken",token);return token;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String method=request.getMethod();String uri=request.getRequestURI();boolean mutation=!Set.of("GET","HEAD","OPTIONS","TRACE").contains(method);
        boolean publicAuth=uri.equals("/api/auth/register");
        if(mutation&&uri.startsWith("/api/")&&!publicAuth){HttpSession session=request.getSession(false);String expected=session==null?null:(String)session.getAttribute("csrfToken");String actual=request.getHeader("X-CSRF-Token");if(expected==null||actual==null||!java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),actual.getBytes(java.nio.charset.StandardCharsets.UTF_8))){response.setStatus(403);response.setContentType("application/json");response.getWriter().write("{\"message\":\"Missing or invalid CSRF token.\"}");return;}}
        chain.doFilter(request,response);
    }
}

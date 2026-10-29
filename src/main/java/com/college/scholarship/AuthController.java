package com.college.scholarship;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    public AuthController(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> register(@RequestBody Registration request) {
        if (request.fullName()==null || request.fullName().isBlank() || request.email()==null || !request.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || request.password()==null || request.password().length()<10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a name, valid email, and password of at least 10 characters.");
        try { jdbc.update("INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,'STUDENT')",request.fullName().trim(),request.email().trim().toLowerCase(),passwords.encode(request.password())); }
        catch (org.springframework.dao.DuplicateKeyException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists."); }
        return Map.of("message","Student account created. Please log in.");
    }

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Login request,HttpServletRequest requestContext) {
        var rows=jdbc.query("SELECT id,full_name,password_hash,role FROM users WHERE email=? AND active=TRUE",(rs,n)->new Account(rs.getLong("id"),rs.getString("full_name"),rs.getString("password_hash"),rs.getString("role")),request.email()==null?"":request.email().trim().toLowerCase());
        if (rows.isEmpty() || request.password()==null || !passwords.matches(request.password(),rows.get(0).hash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email or password is incorrect.");
        Account account=rows.get(0);HttpSession session=requestContext.getSession(true);requestContext.changeSessionId();session=requestContext.getSession(false);session.setAttribute("userId",account.id()); session.setAttribute("role",account.role()); session.setAttribute("fullName",account.name());
        return Map.of("id",account.id(),"fullName",account.name(),"role",account.role(),"csrfToken",CsrfGuard.issue(session));
    }
    @GetMapping("/csrf") public Map<String,String> csrf(HttpSession session){return Map.of("csrfToken",CsrfGuard.issue(session));}
    @PostMapping("/logout") public Map<String,String> logout(HttpSession session) { session.invalidate(); return Map.of("message","Logged out."); }
    @GetMapping("/me") public Map<String,Object> me(HttpSession session) {
        if (session.getAttribute("userId")==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Login required.");
        return Map.of("id",session.getAttribute("userId"),"fullName",session.getAttribute("fullName"),"role",session.getAttribute("role"));
    }
    record Registration(String fullName,String email,String password) {}
    record Login(String email,String password) {}
    record Account(long id,String name,String hash,String role) {}
}

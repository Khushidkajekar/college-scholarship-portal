package com.college.scholarship;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc; private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
    public AdminAccountInitializer(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override public void run(String... args){String email=System.getenv("ADMIN_EMAIL"),password=System.getenv("ADMIN_PASSWORD");if(email==null||email.isBlank()||password==null||password.length()<14)return;Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email=?",Integer.class,email.toLowerCase());if(count==0)jdbc.update("INSERT INTO users(full_name,email,password_hash,role) VALUES('Portal Administrator',?,?, 'ADMIN')",email.toLowerCase(),encoder.encode(password));}
}

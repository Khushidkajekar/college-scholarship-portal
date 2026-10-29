package com.college.scholarship;

import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/dashboard")
public class DashboardController {
    private final JdbcTemplate jdbc; private final ScholarshipRepository repository; private final ScholarshipEligibilityService engine;
    public DashboardController(JdbcTemplate jdbc,ScholarshipRepository repository,ScholarshipEligibilityService engine){this.jdbc=jdbc;this.repository=repository;this.engine=engine;}
    @GetMapping("/student") public Map<String,Object> student(@RequestParam(defaultValue="2026-27") String year,HttpSession session){Object id=session.getAttribute("userId");if(id==null||!"STUDENT".equals(session.getAttribute("role")))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Student login required.");long uid=((Number)id).longValue();var profile=repository.profile(uid);var results=repository.findScholarships(year).stream().map(s->engine.evaluate(profile,s,year)).toList();Map<String,Object> counts=new LinkedHashMap<>();for(String status:List.of("SUBMITTED","UNDER_REVIEW","NEEDS_CORRECTION","APPROVED","REJECTED"))counts.put(status,jdbc.queryForObject("SELECT COUNT(*) FROM scholarship_applications WHERE user_id=? AND status=?",Integer.class,uid,status));return Map.of("availableScholarships",results.size(),"matchingScholarships",results.stream().filter(r->r.eligible().equals("ELIGIBLE")).count(),"provisionalOrIncomplete",results.stream().filter(r->r.eligible().equals("INDETERMINATE")).count(),"applications",counts,"profileComplete",profile!=null&&profile.courseType()!=null&&profile.currentYear()!=null&&profile.previousAcademicPercentage()!=null&&profile.annualFamilyIncome()!=null&&profile.socialCategory()!=null&&profile.gender()!=null&&profile.stateOfResidence()!=null);}
}

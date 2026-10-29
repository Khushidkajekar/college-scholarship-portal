package com.college.scholarship;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/profile")
public class ProfileController {
    private final JdbcTemplate jdbc;
    public ProfileController(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @GetMapping public Map<String,Object> get(HttpSession session) {
        long id=studentId(session);
        var rows=jdbc.queryForList("SELECT academic_year AS academicYear,course_type AS courseType,current_year AS currentYear,previous_academic_percentage AS previousAcademicPercentage,cgpa,annual_family_income AS annualFamilyIncome,social_category AS socialCategory,gender,state_of_residence AS stateOfResidence,course_name AS courseName,institution_name AS institutionName,institution_approval_status AS institutionApprovalStatus,admission_route AS admissionRoute,qualifying_board AS qualifyingBoard,qualifying_merit_position AS qualifyingMeritPosition,qualifying_conditions_verified AS qualifyingConditionsVerified FROM student_profiles WHERE user_id=?",id);
        return rows.isEmpty()?Map.of():rows.get(0);
    }
    @PutMapping public Map<String,String> save(@RequestBody Profile p,HttpSession session) {
        long id=studentId(session);
        if (p.academicYear()==null || !p.academicYear().matches("20\\d{2}-\\d{2}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Academic year must use YYYY-YY format.");
        if (p.currentYear()!=null && (p.currentYear()<1 || p.currentYear()>4)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Study year must be from 1 to 4.");
        if (p.previousAcademicPercentage()!=null && (p.previousAcademicPercentage().signum()<0 || p.previousAcademicPercentage().compareTo(BigDecimal.valueOf(100))>0)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Percentage must be from 0 to 100.");
        if (p.cgpa()!=null && (p.cgpa().signum()<0 || p.cgpa().compareTo(BigDecimal.TEN)>0)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CGPA must be from 0 to 10.");
        if (p.annualFamilyIncome()!=null && p.annualFamilyIncome().signum()<0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Family income cannot be negative.");
        String courseType=clean(p.courseType()),category=clean(p.socialCategory()),gender=clean(p.gender()),state=clean(p.stateOfResidence()),courseName=clean(p.courseName()),institution=clean(p.institutionName()),route=clean(p.admissionRoute()),board=clean(p.qualifyingBoard());
        checkEnum(courseType,Set.of("UG","PG","DIPLOMA"),"Course type");checkEnum(category,Set.of("GENERAL","OBC","SC","ST","MINORITY"),"Social category");checkEnum(gender,Set.of("MALE","FEMALE","OTHER"),"Gender");
        jdbc.update("INSERT INTO student_profiles(user_id,academic_year,course_type,current_year,previous_academic_percentage,cgpa,annual_family_income,social_category,gender,state_of_residence,course_name,institution_name,institution_approval_status,admission_route,qualifying_board,qualifying_merit_position,institution_approval_verified,admission_route_verified,qualifying_conditions_verified) VALUES(?,?,?,?,?,?,?,?,?,?,?, ?,NULL,?,?,?,FALSE,FALSE,FALSE) ON DUPLICATE KEY UPDATE academic_year=VALUES(academic_year),course_type=VALUES(course_type),current_year=VALUES(current_year),previous_academic_percentage=VALUES(previous_academic_percentage),cgpa=VALUES(cgpa),annual_family_income=VALUES(annual_family_income),social_category=VALUES(social_category),gender=VALUES(gender),state_of_residence=VALUES(state_of_residence),course_name=VALUES(course_name),institution_name=VALUES(institution_name),institution_approval_status=NULL,admission_route=VALUES(admission_route),qualifying_board=VALUES(qualifying_board),qualifying_merit_position=VALUES(qualifying_merit_position),institution_approval_verified=FALSE,admission_route_verified=FALSE,qualifying_conditions_verified=FALSE",id,p.academicYear(),courseType,p.currentYear(),p.previousAcademicPercentage(),p.cgpa(),p.annualFamilyIncome(),category,gender,state,courseName,institution,route,board,p.qualifyingMeritPosition());
        return Map.of("message","Profile saved.");
    }
    private void checkEnum(String v,Set<String> allowed,String label){if(v!=null&&!v.isBlank()&&!allowed.contains(v))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,label+" is not supported.");}
    private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private long studentId(HttpSession session) {
        Object id=session.getAttribute("userId");
        if (id==null || !"STUDENT".equals(session.getAttribute("role"))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Student login required.");
        return ((Number)id).longValue();
    }
    record Profile(String academicYear,String courseType,Integer currentYear,BigDecimal previousAcademicPercentage,BigDecimal cgpa,BigDecimal annualFamilyIncome,String socialCategory,String gender,String stateOfResidence,String courseName,String institutionName,String institutionApprovalStatus,String admissionRoute,String qualifyingBoard,Integer qualifyingMeritPosition,boolean qualifyingConditionsVerified) {}
}

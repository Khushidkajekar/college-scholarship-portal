package com.college.scholarship;

import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/scholarships")
public class ScholarshipController {
    private final ScholarshipRepository repository;
    private final ScholarshipEligibilityService eligibility;
    public ScholarshipController(ScholarshipRepository repository, ScholarshipEligibilityService eligibility) { this.repository=repository; this.eligibility=eligibility; }
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();}

    @GetMapping
    public List<Map<String,Object>> catalogue(@RequestParam(defaultValue="2026-27") String academicYear,@RequestParam(required=false) String q,@RequestParam(required=false) String courseType,@RequestParam(required=false) Integer studyYear,@RequestParam(required=false) BigDecimal minAmount,@RequestParam(required=false) BigDecimal studentIncome,@RequestParam(required=false) Boolean applicationOpenOnly) {return repository.publicCatalogue(academicYear,blank(q),blank(courseType),studyYear,minAmount,studentIncome,applicationOpenOnly);}

    @GetMapping("/{id}") public Map<String,Object> details(@PathVariable long id,@RequestParam(defaultValue="2026-27") String academicYear,HttpSession session){Map<String,Object> detail=repository.catalogueDetail(id,academicYear);if(detail==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Scholarship not found.");ScholarshipCriteria criteria=repository.findScholarships(academicYear).stream().filter(s->s.id()==id).findFirst().orElseThrow();detail.put("courseTypes",criteria.courseTypes());detail.put("courseNames",criteria.courseNames());detail.put("studyYears",criteria.years());detail.put("categories",criteria.categories());detail.put("genders",criteria.genders());detail.put("states",criteria.states());detail.put("requiredDocuments",criteria.requiredDocuments());detail.put("additionalRules",criteria.additionalRules());Object user=session.getAttribute("userId");if(user!=null&&"STUDENT".equals(session.getAttribute("role")))detail.put("eligibility",eligibility.evaluate(repository.profile(((Number)user).longValue()),criteria,academicYear));return detail;}

    @GetMapping("/matching")
    public List<EligibilityResult> matching(@RequestParam(defaultValue="2026-27") String academicYear, HttpSession session) {
        Object id=session.getAttribute("userId");
        if (id == null || !"STUDENT".equals(session.getAttribute("role"))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Student login required");
        StudentEligibilityProfile profile=repository.profile(((Number)id).longValue());
        return repository.findScholarships(academicYear).stream().map(s->eligibility.evaluate(profile,s,academicYear)).toList();
    }

    @GetMapping("/compare")
    public List<Map<String,Object>> compare(@RequestParam List<Long> ids,@RequestParam(defaultValue="2026-27") String academicYear,HttpSession session){
        if(ids.size()<2||ids.size()>4)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select between two and four scholarships to compare.");
        var records=repository.findScholarships(academicYear).stream().filter(s->ids.contains(s.id())).toList();
        if(records.size()!=ids.stream().distinct().count())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"One or more active scholarships were not found for this year.");
        Object userId=session.getAttribute("userId");StudentEligibilityProfile profile=userId!=null&&"STUDENT".equals(session.getAttribute("role"))?repository.profile(((Number)userId).longValue()):null;
        return records.stream().map(s->{Map<String,Object> row=new LinkedHashMap<>();row.put("id",s.id());row.put("schemeCode",s.schemeCode());row.put("name",s.name());row.put("authority",s.administeringAuthority());row.put("academicYear",s.academicYear());row.put("courseTypes",s.courseTypes());row.put("courseNames",s.courseNames());row.put("years",s.years());row.put("categories",s.categories());row.put("genders",s.genders());row.put("states",s.states());row.put("maximumIncome",s.maximumIncome());row.put("minimumPercentage",s.minimumPercentage());row.put("deadline",s.deadline());row.put("verified",s.verified());row.put("documents",s.requiredDocuments());row.put("additionalRules",s.additionalRules());row.put("amount",s.scholarshipAmount());if(profile!=null)row.put("eligibility",eligibility.evaluate(profile,s,academicYear));return row;}).toList();
    }
}

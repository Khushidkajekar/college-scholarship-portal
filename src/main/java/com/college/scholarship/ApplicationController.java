package com.college.scholarship;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/applications")
public class ApplicationController {
    private final JdbcTemplate jdbc; private final ScholarshipRepository repo; private final ScholarshipEligibilityService engine;
    public ApplicationController(JdbcTemplate jdbc,ScholarshipRepository repo,ScholarshipEligibilityService engine){this.jdbc=jdbc;this.repo=repo;this.engine=engine;}
    @GetMapping public List<Map<String,Object>> mine(HttpSession session){long uid=student(session);return jdbc.queryForList("SELECT a.id,a.scholarship_id,s.name AS scholarship_name,a.application_date,a.status,a.student_statement,a.admin_remarks FROM scholarship_applications a JOIN scholarships s ON s.id=a.scholarship_id WHERE a.user_id=? ORDER BY a.application_date DESC",uid);}
    @GetMapping("/{id}/history") public List<Map<String,Object>> history(@PathVariable long id,HttpSession session){long uid=student(session);return jdbc.queryForList("SELECT h.previous_status,h.new_status,h.remarks,h.changed_at FROM scholarship_application_history h JOIN scholarship_applications a ON a.id=h.application_id WHERE h.application_id=? AND a.user_id=? ORDER BY h.changed_at",id,uid);}
    @PostMapping(value="/{id}/documents",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> upload(@PathVariable long id,@RequestParam String documentType,@RequestParam MultipartFile file,HttpSession session)throws IOException{long uid=student(session);var owned=jdbc.query("SELECT a.id,a.scholarship_id FROM scholarship_applications a WHERE a.id=? AND a.user_id=? AND a.status IN ('SUBMITTED','UNDER_REVIEW','NEEDS_CORRECTION')",(r,n)->new long[]{r.getLong(1),r.getLong(2)},id,uid);if(owned.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Editable application not found.");if(documentType==null||documentType.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Document type is required.");List<String> required=jdbc.query("SELECT document_name FROM scholarship_documents WHERE scholarship_id=?",(r,n)->r.getString(1),owned.get(0)[1]);if(!required.isEmpty()&&!required.contains(documentType))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a document type required by this scholarship.");if(file.isEmpty()||file.getSize()>10_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a file up to 10 MB.");String type=file.getContentType()==null?"":file.getContentType();if(!Set.of("application/pdf","image/jpeg","image/png").contains(type))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Allowed file types are PDF, JPG, and PNG.");String original=Path.of(file.getOriginalFilename()==null?"document":file.getOriginalFilename()).getFileName().toString();String key=UUID.randomUUID().toString()+extension(original);Path root=Path.of("uploads").toAbsolutePath().normalize();Files.createDirectories(root);Path destination=root.resolve(key).normalize();if(!destination.startsWith(root))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid file name.");file.transferTo(destination);jdbc.update("INSERT INTO application_documents(application_id,document_type,original_filename,stored_filename,content_type,file_size) VALUES(?,?,?,?,?,?)",id,documentType,original,key,type,file.getSize());return Map.of("message","Document uploaded.");}
    @GetMapping("/{id}/documents") public List<Map<String,Object>> documents(@PathVariable long id,HttpSession session){long uid=student(session);return jdbc.queryForList("SELECT d.id,d.document_type,d.original_filename,d.file_size,d.verification_status,d.verification_remarks,d.uploaded_at FROM application_documents d JOIN scholarship_applications a ON a.id=d.application_id WHERE a.id=? AND a.user_id=?",id,uid);}
    private String extension(String f){int i=f.lastIndexOf('.');return i<0?"":f.substring(i).replaceAll("[^A-Za-z0-9.]","");}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional public Map<String,Object> apply(@RequestBody ApplyRequest request,HttpSession session){
        if(request==null||request.academicYear()==null||request.academicYear().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Academic year is required.");
        long uid=student(session); var scholarship=repo.findScholarships(request.academicYear()).stream().filter(s->s.id()==request.scholarshipId()).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Active scholarship not found for the selected academic year."));
        EligibilityResult result=engine.evaluate(repo.profile(uid),scholarship,request.academicYear());
        if("INELIGIBLE".equals(result.eligible()) || !result.applicationOpen())throw new ResponseStatusException(HttpStatus.CONFLICT,"Application cannot be submitted: "+result.explanation());
        if("INDETERMINATE".equals(result.eligible()) && !request.acceptProvisional())throw new ResponseStatusException(HttpStatus.CONFLICT,"Eligibility is provisional or incomplete. Explicit confirmation is required to submit a demonstration application.");
        try{jdbc.update("INSERT INTO scholarship_applications(user_id,scholarship_id,student_statement) VALUES(?,?,?)",uid,scholarship.id(),request.studentStatement());}
        catch(org.springframework.dao.DuplicateKeyException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"An application already exists for this scholarship.");}
        Long appId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO scholarship_application_history(application_id,previous_status,new_status,changed_by,remarks) VALUES(?,NULL,'SUBMITTED',?,?)",appId,uid,"Initial submission; eligibility state: "+result.eligible());
        return Map.of("id",appId,"status","SUBMITTED","eligibility",result.eligible());
    }
    static long student(HttpSession s){Object id=s.getAttribute("userId");if(id==null||!"STUDENT".equals(s.getAttribute("role")))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Student login required.");return ((Number)id).longValue();}
    record ApplyRequest(long scholarshipId,String academicYear,String studentStatement,boolean acceptProvisional){}
}

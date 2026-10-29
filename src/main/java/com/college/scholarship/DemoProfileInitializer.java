package com.college.scholarship;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoProfileInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc; private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
    public DemoProfileInitializer(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override public void run(String... args){
        if(!"true".equalsIgnoreCase(System.getenv("LOAD_DEMO_STUDENTS")))return;
        String password=System.getenv("DEMO_STUDENT_PASSWORD");if(password==null||password.length()<14)throw new IllegalStateException("Set DEMO_STUDENT_PASSWORD to at least 14 characters to load demo students.");
        List<DemoStudent> students=List.of(
            new DemoStudent("obc@college.test","Demo OBC Student","OBC","FEMALE","Karnataka","UG","B.Tech",2,"55","120000","APPROVED","REGULAR",null,null),
            new DemoStudent("scst@college.test","Demo SC/ST Student","SC","MALE","Karnataka","UG","B.Tech",1,"70","200000","APPROVED","REGULAR",null,null),
            new DemoStudent("csss@college.test","Demo CSSS Student","GENERAL","OTHER","Maharashtra","UG","B.Tech",1,"90","400000","APPROVED","REGULAR","Demo Board",10),
            new DemoStudent("pragati@college.test","Demo Pragati Student","GENERAL","FEMALE","Karnataka","UG","B.Tech",1,"70","300000","APPROVED","REGULAR",null,null),
            new DemoStudent("income.fail@college.test","Demo Income Failure","OBC","MALE","Karnataka","UG","B.Tech",2,"75","180000","APPROVED","REGULAR",null,null),
            new DemoStudent("incomplete@college.test","Demo Incomplete Profile",null,null,null,null,null,null,null,null,null,null,null,null));
        for(DemoStudent d:students){Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email=?",Integer.class,d.email());if(exists!=0)continue;jdbc.update("INSERT INTO users(full_name,email,password_hash,role) VALUES(?,?,?,'STUDENT')",d.name(),d.email(),encoder.encode(password));Long id=jdbc.queryForObject("SELECT id FROM users WHERE email=?",Long.class,d.email());jdbc.update("INSERT INTO student_profiles(user_id,academic_year,course_type,current_year,previous_academic_percentage,annual_family_income,social_category,gender,state_of_residence,course_name,institution_name,institution_approval_status,admission_route,qualifying_board,qualifying_merit_position,qualifying_conditions_verified) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,FALSE)",id,"2026-27",d.courseType(),d.year(),decimal(d.percentage()),decimal(d.income()),d.category(),d.gender(),d.state(),d.courseName(),"Demo College",d.approval(),d.route(),d.board(),d.position());}
    }
    private BigDecimal decimal(String value){return value==null?null:new BigDecimal(value);}
    record DemoStudent(String email,String name,String category,String gender,String state,String courseType,String courseName,Integer year,String percentage,String income,String approval,String route,String board,Integer position){}
}

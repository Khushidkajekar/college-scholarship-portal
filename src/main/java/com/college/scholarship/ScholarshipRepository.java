package com.college.scholarship;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ScholarshipRepository {
    private final JdbcTemplate jdbc;
    public ScholarshipRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ScholarshipCriteria> findScholarships(String year) {
        List<BaseScholarship> base=jdbc.query("SELECT * FROM scholarships WHERE academic_year=? AND active=TRUE ORDER BY name",(rs,n)->new BaseScholarship(
                rs.getLong("id"),rs.getString("scheme_code"),rs.getString("name"),rs.getString("administering_authority"),rs.getString("academic_year"),
                rs.getBigDecimal("maximum_annual_family_income"),rs.getBigDecimal("minimum_previous_academic_percentage"),rs.getBigDecimal("scholarship_amount"),
                rs.getDate("application_deadline")==null?null:rs.getDate("application_deadline").toLocalDate(),rs.getBoolean("active"),rs.getBoolean("criteria_verified")),year);
        if(base.isEmpty())return List.of();
        List<Long> ids=base.stream().map(BaseScholarship::id).toList();
        Map<Long,Set<String>> courseTypes=loadStringSets("scholarship_courses","course_type",ids);
        Map<Long,Set<String>> courseNames=loadStringSets("scholarship_courses","course_name",ids);
        Map<Long,Set<Integer>> years=loadIntegerSets(ids);
        Map<Long,Set<String>> categories=loadStringSets("scholarship_categories","category",ids);
        Map<Long,Set<String>> genders=loadStringSets("scholarship_genders","gender",ids);
        Map<Long,Set<String>> states=loadStringSets("scholarship_states","state_name",ids);
        Map<Long,List<String>> documents=loadLists("SELECT scholarship_id,document_name FROM scholarship_documents WHERE scholarship_id IN ",ids);
        Map<Long,List<EligibilityRule>> rules=loadRules(ids);
        return base.stream().map(b->new ScholarshipCriteria(b.id(),b.code(),b.name(),b.authority(),b.year(),set(courseTypes,b.id()),set(courseNames,b.id()),set(years,b.id()),set(categories,b.id()),set(genders,b.id()),set(states,b.id()),b.income(),b.percentage(),b.amount(),b.deadline(),b.active(),b.verified(),list(documents,b.id()),list(rules,b.id()))).toList();
    }
    private String in(List<Long> ids){return "("+String.join(",",Collections.nCopies(ids.size(),"?"))+")";}
    private Map<Long,Set<String>> loadStringSets(String table,String column,List<Long> ids){Map<Long,Set<String>> values=new HashMap<>();jdbc.query("SELECT scholarship_id,"+column+" FROM "+table+" WHERE scholarship_id IN "+in(ids),rs->{while(rs.next())values.computeIfAbsent(rs.getLong(1),k->new HashSet<>()).add(rs.getString(2));},ids.toArray());return values;}
    private Map<Long,Set<Integer>> loadIntegerSets(List<Long> ids){Map<Long,Set<Integer>> values=new HashMap<>();jdbc.query("SELECT scholarship_id,study_year FROM scholarship_years WHERE scholarship_id IN "+in(ids),rs->{while(rs.next())values.computeIfAbsent(rs.getLong(1),k->new HashSet<>()).add(rs.getInt(2));},ids.toArray());return values;}
    private Map<Long,List<String>> loadLists(String prefix,List<Long> ids){Map<Long,List<String>> values=new HashMap<>();jdbc.query(prefix+in(ids),rs->{while(rs.next())values.computeIfAbsent(rs.getLong(1),k->new ArrayList<>()).add(rs.getString(2));},ids.toArray());return values;}
    private Map<Long,List<EligibilityRule>> loadRules(List<Long> ids){Map<Long,List<EligibilityRule>> values=new HashMap<>();jdbc.query("SELECT scholarship_id,rule_code,description,required,verified FROM scholarship_rules WHERE scholarship_id IN "+in(ids),rs->{while(rs.next())values.computeIfAbsent(rs.getLong("scholarship_id"),k->new ArrayList<>()).add(new EligibilityRule(rs.getString("rule_code"),rs.getString("description"),rs.getBoolean("required"),rs.getBoolean("verified")));},ids.toArray());return values;}
    private <T> Set<T> set(Map<Long,Set<T>> map,long id){return map.getOrDefault(id,Set.of());}
    private <T> List<T> list(Map<Long,List<T>> map,long id){return map.getOrDefault(id,List.of());}
    public StudentEligibilityProfile profile(long userId) {
        List<StudentEligibilityProfile> rows=jdbc.query("SELECT * FROM student_profiles WHERE user_id=?",(rs,n)->{
            int yearValue=rs.getInt("current_year");Integer currentYear=rs.wasNull()?null:yearValue;
            int meritValue=rs.getInt("qualifying_merit_position");Integer meritPosition=rs.wasNull()?null:meritValue;
            return new StudentEligibilityProfile(rs.getString("academic_year"),rs.getString("course_type"),rs.getString("course_name"),currentYear,
                rs.getBigDecimal("previous_academic_percentage"),rs.getBigDecimal("annual_family_income"),rs.getString("social_category"),rs.getString("gender"),
                rs.getString("state_of_residence"),rs.getString("institution_approval_status"),rs.getString("admission_route"),rs.getBoolean("institution_approval_verified"),rs.getBoolean("admission_route_verified"),rs.getBoolean("qualifying_conditions_verified"),
                rs.getString("qualifying_board"),meritPosition);
        },userId);
        return rows.isEmpty()?null:rows.get(0);
    }
    public List<Map<String,Object>> publicCatalogue(String year,String q,String courseType,Integer studyYear,BigDecimal minAmount,BigDecimal studentIncome,Boolean openOnly) {
        return jdbc.queryForList("SELECT s.id,s.scheme_code,s.name,s.administering_authority,s.description,s.academic_year,s.maximum_annual_family_income,s.minimum_previous_academic_percentage,s.scholarship_amount,s.application_deadline,s.official_information_url,s.criteria_verified,s.active FROM scholarships s WHERE s.academic_year=? AND s.active=TRUE AND (? IS NULL OR LOWER(s.name) LIKE CONCAT('%',LOWER(?),'%') OR LOWER(s.administering_authority) LIKE CONCAT('%',LOWER(?),'%')) AND (? IS NULL OR EXISTS(SELECT 1 FROM scholarship_courses c WHERE c.scholarship_id=s.id AND (c.course_type='*' OR c.course_type=?))) AND (? IS NULL OR EXISTS(SELECT 1 FROM scholarship_years y WHERE y.scholarship_id=s.id AND y.study_year=?)) AND (? IS NULL OR s.scholarship_amount>=?) AND (? IS NULL OR s.maximum_annual_family_income IS NULL OR s.maximum_annual_family_income>=?) AND (? IS NULL OR ?=FALSE OR s.application_deadline IS NULL OR s.application_deadline>=CURRENT_DATE()) ORDER BY s.name LIMIT 100",year,q,q,q,courseType,courseType,studyYear,studyYear,minAmount,minAmount,studentIncome,studentIncome,openOnly,openOnly);
    }
    public Map<String,Object> catalogueDetail(long id,String year){List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,scheme_code,name,administering_authority,description,academic_year,maximum_annual_family_income,minimum_previous_academic_percentage,scholarship_amount,application_deadline,official_information_url,criteria_verified,active FROM scholarships WHERE id=? AND academic_year=? AND active=TRUE",id,year);return rows.isEmpty()?null:rows.get(0);}
    private record BaseScholarship(long id,String code,String name,String authority,String year,BigDecimal income,BigDecimal percentage,BigDecimal amount,java.time.LocalDate deadline,boolean active,boolean verified){}
}

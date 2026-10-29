package com.college.scholarship;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScholarshipEligibilityServiceTest {
    private final ScholarshipEligibilityService service = new ScholarshipEligibilityService(List.of(new CsssBoardMeritRuleEvaluator(),new PragatiInstitutionRuleEvaluator(),new PragatiAdmissionRouteRuleEvaluator()));
    private ScholarshipCriteria scholarship(String code, BigDecimal income, BigDecimal marks, Set<String> categories, Set<String> genders, Set<String> states, boolean verified, List<EligibilityRule> rules) {
        return new ScholarshipCriteria(1,code,"Demo", "Authority", "2026-27",Set.of("UG"),Set.of("B.Tech"),Set.of(1),categories,genders,states,income,marks,null,null,true,verified,List.of(),rules);
    }
    private StudentEligibilityProfile profile(BigDecimal income,BigDecimal marks,String category,String gender,String state) {
        return new StudentEligibilityProfile("2026-27","UG","B.Tech",1,marks,income,category,gender,state,"APPROVED","REGULAR",true,true,true,"Board",1);
    }
    @Test void inclusiveIncomeAndMarksBoundariesPass() {
        var s=scholarship("SSP-OBC",new BigDecimal("150000"),new BigDecimal("50"),Set.of("OBC"),Set.of("*"),Set.of("Karnataka"),true,List.of());
        assertEquals("ELIGIBLE",service.evaluate(profile(new BigDecimal("150000"),new BigDecimal("50"),"OBC","OTHER","Karnataka"),s,"2026-27").eligible());
    }
    @Test void incomeJustAboveLimitFails() {
        var s=scholarship("SSP-OBC",new BigDecimal("150000"),new BigDecimal("50"),Set.of("OBC"),Set.of("*"),Set.of("Karnataka"),true,List.of());
        assertEquals("INELIGIBLE",service.evaluate(profile(new BigDecimal("150000.01"),new BigDecimal("50"),"OBC","FEMALE","Karnataka"),s,"2026-27").eligible());
    }
    @Test void percentageJustBelowMinimumFails() {
        var s=scholarship("SSP-SCST",new BigDecimal("250000"),new BigDecimal("50"),Set.of("SC","ST"),Set.of("*"),Set.of("Karnataka"),true,List.of());
        var result=service.evaluate(profile(new BigDecimal("100000"),new BigDecimal("49.99"),"SC","OTHER","Karnataka"),s,"2026-27");
        assertTrue(result.failedCriteria().stream().anyMatch(x->x.startsWith("Previous academic percentage")));
    }
    @Test void missingValuesAndUnverifiedRulesAreIndeterminate() {
        var s=scholarship("NSP-CSSS",new BigDecimal("450000"),new BigDecimal("85"),Set.of("*"),Set.of("*"),Set.of("*"),false,List.of(new EligibilityRule("CSSS_BOARD_MERIT","Board merit position",true,false)));
        var p=new StudentEligibilityProfile("2026-27",null,null,null,null,null,null,null,null,null,false,false,false,null,null);
        var result=service.evaluate(p,s,"2026-27");
        assertEquals("INDETERMINATE",result.eligible()); assertTrue(result.verificationRequired()); assertFalse(result.missingInformation().isEmpty());
    }
    @Test void academicYearAndGenderMismatchFail() {
        var s=scholarship("AICTE-PRAGATI",new BigDecimal("800000"),new BigDecimal("50"),Set.of("*"),Set.of("FEMALE"),Set.of("*"),true,List.of());
        var result=service.evaluate(profile(new BigDecimal("100000"),new BigDecimal("80"),"GENERAL","MALE","Kerala"),s,"2025-26");
        assertEquals("INELIGIBLE",result.eligible()); assertFalse(result.failedCriteria().isEmpty());
    }
    @Test void categoryAndStateMismatchAreExplained() {
        var s=scholarship("SSP-SCST",new BigDecimal("250000"),new BigDecimal("50"),Set.of("SC","ST"),Set.of("*"),Set.of("Karnataka"),true,List.of());
        var result=service.evaluate(profile(new BigDecimal("100000"),new BigDecimal("80"),"OBC","FEMALE","Kerala"),s,"2026-27");
        assertTrue(result.failedCriteria().stream().anyMatch(x->x.startsWith("Social category")));
        assertTrue(result.failedCriteria().stream().anyMatch(x->x.startsWith("State of residence")));
    }
    @Test void courseAndStudyYearMismatchFail() {
        var s=scholarship("AICTE-PRAGATI",new BigDecimal("800000"),new BigDecimal("50"),Set.of("*"),Set.of("FEMALE"),Set.of("*"),true,List.of());
        var p=new StudentEligibilityProfile("2026-27","PG","M.Tech",2,new BigDecimal("75"),new BigDecimal("100000"),"GENERAL","FEMALE","Delhi","APPROVED","REGULAR",true,true,true,null,null);
        var result=service.evaluate(p,s,"2026-27");assertTrue(result.failedCriteria().stream().anyMatch(x->x.startsWith("Course type")));assertTrue(result.failedCriteria().stream().anyMatch(x->x.startsWith("Study year")));
    }
    @Test void scholarshipYearMismatchIsIneligible() {
        var original=scholarship("NSP-CSSS",new BigDecimal("450000"),new BigDecimal("85"),Set.of("*"),Set.of("*"),Set.of("*"),true,List.of());
        var s=new ScholarshipCriteria(original.id(),original.schemeCode(),original.name(),original.administeringAuthority(),"2025-26",original.courseTypes(),original.courseNames(),original.years(),original.categories(),original.genders(),original.states(),original.maximumIncome(),original.minimumPercentage(),original.scholarshipAmount(),original.deadline(),true,true,List.of(),List.of());
        assertEquals("INELIGIBLE",service.evaluate(profile(new BigDecimal("100000"),new BigDecimal("90"),"GENERAL","OTHER","Goa"),s,"2026-27").eligible());
    }
    @Test void csssAndPragatiConditionsRemainProvisional() {
        var csss=scholarship("NSP-CSSS",new BigDecimal("450000"),new BigDecimal("85"),Set.of("*"),Set.of("*"),Set.of("*"),false,List.of(new EligibilityRule("CSSS_BOARD_MERIT","Board-specific percentile requirement",true,false)));
        assertTrue(service.evaluate(profile(new BigDecimal("400000"),new BigDecimal("90"),"GENERAL","MALE","Goa"),csss,"2026-27").verificationRequired());
        var pragati=scholarship("AICTE-PRAGATI",new BigDecimal("800000"),new BigDecimal("50"),Set.of("*"),Set.of("FEMALE"),Set.of("*"),false,List.of(new EligibilityRule("PRAGATI_INSTITUTION_APPROVAL","Institution approval",true,false),new EligibilityRule("PRAGATI_ADMISSION_ROUTE","Admission route",true,false)));
        var female=profile(new BigDecimal("200000"),new BigDecimal("80"),"GENERAL","FEMALE","Goa");
        var result=service.evaluate(female,pragati,"2026-27");assertEquals("INDETERMINATE",result.eligible());assertTrue(result.verificationRequired());
    }
    @Test void registeredRuleEvaluatorWorksWithoutScholarshipNameBranches() {
        var s=scholarship("FUTURE-SCHEME",new BigDecimal("450000"),new BigDecimal("85"),Set.of("*"),Set.of("*"),Set.of("*"),true,List.of(new EligibilityRule("CSSS_BOARD_MERIT","Board merit rule",true,true)));
        var result=service.evaluate(profile(new BigDecimal("100000"),new BigDecimal("90"),"GENERAL","OTHER","Goa"),s,"2026-27");
        assertEquals("ELIGIBLE",result.eligible());
        assertTrue(result.matchedCriteria().stream().anyMatch(x->x.contains("institutionally verified")));
    }
    @Test void unknownVerifiedRulesStayIndeterminateWithoutEvaluator() {
        var s=scholarship("ANOTHER-FUTURE-SCHEME",new BigDecimal("450000"),new BigDecimal("50"),Set.of("*"),Set.of("*"),Set.of("*"),true,List.of(new EligibilityRule("UNREGISTERED_RULE","A rule without a validator",true,true)));
        var result=service.evaluate(profile(new BigDecimal("100000"),new BigDecimal("90"),"GENERAL","OTHER","Goa"),s,"2026-27");
        assertEquals("INDETERMINATE",result.eligible());assertTrue(result.verificationRequired());
    }
    @Test void expiredAndInactiveScholarshipsCloseApplications() {
        var expiredBase=scholarship("SSP-OBC",new BigDecimal("150000"),new BigDecimal("50"),Set.of("OBC"),Set.of("*"),Set.of("Karnataka"),true,List.of());
        var expired=new ScholarshipCriteria(expiredBase.id(),expiredBase.schemeCode(),expiredBase.name(),expiredBase.administeringAuthority(),expiredBase.academicYear(),expiredBase.courseTypes(),expiredBase.courseNames(),expiredBase.years(),expiredBase.categories(),expiredBase.genders(),expiredBase.states(),expiredBase.maximumIncome(),expiredBase.minimumPercentage(),expiredBase.scholarshipAmount(),LocalDate.now().minusDays(1),true,true,List.of(),List.of());
        var result=service.evaluate(profile(new BigDecimal("100000"),new BigDecimal("70"),"OBC","OTHER","Karnataka"),expired,"2026-27");assertFalse(result.applicationOpen());assertTrue(result.failedCriteria().stream().anyMatch(x->x.contains("deadline")));
        var inactive=new ScholarshipCriteria(expiredBase.id(),expiredBase.schemeCode(),expiredBase.name(),expiredBase.administeringAuthority(),expiredBase.academicYear(),expiredBase.courseTypes(),expiredBase.courseNames(),expiredBase.years(),expiredBase.categories(),expiredBase.genders(),expiredBase.states(),expiredBase.maximumIncome(),expiredBase.minimumPercentage(),expiredBase.scholarshipAmount(),null,false,true,List.of(),List.of());
        assertFalse(service.evaluate(profile(new BigDecimal("100000"),new BigDecimal("70"),"OBC","OTHER","Karnataka"),inactive,"2026-27").applicationOpen());
    }
}

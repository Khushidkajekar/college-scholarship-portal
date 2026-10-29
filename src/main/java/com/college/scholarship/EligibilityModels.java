package com.college.scholarship;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

record StudentEligibilityProfile(String academicYear, String courseType, String courseName, Integer currentYear,
        BigDecimal previousAcademicPercentage, BigDecimal annualFamilyIncome, String socialCategory,
        String gender, String stateOfResidence, String institutionApprovalStatus, String admissionRoute,
        boolean institutionApprovalVerified,boolean admissionRouteVerified,boolean qualifyingConditionsVerified,
        String qualifyingBoard, Integer qualifyingMeritPosition) {}

record ScholarshipCriteria(long id, String schemeCode, String name, String administeringAuthority,
        String academicYear, Set<String> courseTypes, Set<String> courseNames, Set<Integer> years,
        Set<String> categories, Set<String> genders, Set<String> states, BigDecimal maximumIncome,
        BigDecimal minimumPercentage, BigDecimal scholarshipAmount, LocalDate deadline, boolean active, boolean verified,
        List<String> requiredDocuments, List<EligibilityRule> additionalRules) {}

record EligibilityRule(String code,String description,boolean required,boolean verified) {}

record RuleEvaluation(String outcome,String explanation) {
    static RuleEvaluation pass(String explanation){return new RuleEvaluation("PASS",explanation);}
    static RuleEvaluation fail(String explanation){return new RuleEvaluation("FAIL",explanation);}
    static RuleEvaluation missing(String explanation){return new RuleEvaluation("MISSING",explanation);}
}

record EligibilityResult(long scholarshipId, String scholarshipName, String eligible,
        List<String> matchedCriteria, List<String> failedCriteria, List<String> missingInformation,
        boolean verificationRequired, boolean applicationOpen, String explanation) {}

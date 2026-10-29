package com.college.scholarship;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ScholarshipEligibilityService {
    private static final Set<String> VALID_CATEGORIES = Set.of("GENERAL", "OBC", "SC", "ST", "MINORITY");
    private static final Set<String> VALID_GENDERS = Set.of("MALE", "FEMALE", "OTHER");
    private final Map<String,SchemeRuleEvaluator> ruleEvaluators;

    public ScholarshipEligibilityService(List<SchemeRuleEvaluator> evaluators){this.ruleEvaluators=evaluators.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(SchemeRuleEvaluator::ruleCode,e->e));}

    public EligibilityResult evaluate(StudentEligibilityProfile p, ScholarshipCriteria s, String academicYear) {
        List<String> matched = new ArrayList<>(), failed = new ArrayList<>(), missing = new ArrayList<>();
        boolean verificationRequired = !s.verified();
        checkValue("Academic year", p == null ? null : p.academicYear(), academicYear, matched, failed, missing);
        if (!s.academicYear().equals(academicYear)) failed.add("Scholarship is for academic year " + s.academicYear() + ", not " + academicYear + ".");
        else matched.add("Scholarship academic year matches " + academicYear + ".");
        if (p == null) {
            missing.add("Student eligibility profile");
        } else {
            membership("Course type", p.courseType(), s.courseTypes(), matched, failed, missing);
            if (!s.courseNames().contains("*")) membership("Course name", p.courseName(), s.courseNames(), matched, failed, missing);
            membership("Study year", p.currentYear() == null ? null : p.currentYear().toString(),
                    s.years().stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet()), matched, failed, missing);
            threshold("Previous academic percentage", p.previousAcademicPercentage(), s.minimumPercentage(), true, matched, failed, missing);
            threshold("Annual family income", p.annualFamilyIncome(), s.maximumIncome(), false, matched, failed, missing);
            if (!s.categories().contains("*")) membership("Social category", p.socialCategory(), s.categories(), matched, failed, missing);
            else if (p.socialCategory() == null) missing.add("Social category"); else if (VALID_CATEGORIES.contains(p.socialCategory())) matched.add("Social category is within the unrestricted supported categories."); else failed.add("Social category is unsupported.");
            if (!s.genders().contains("*")) membership("Gender", p.gender(), s.genders(), matched, failed, missing);
            else if (p.gender() == null) missing.add("Gender"); else if (VALID_GENDERS.contains(p.gender())) matched.add("Gender is unrestricted for this scheme."); else failed.add("Gender value is unsupported.");
            if (!s.states().contains("*")) membership("State of residence", p.stateOfResidence(), s.states(), matched, failed, missing);
            else if (p.stateOfResidence() == null) missing.add("State of residence"); else matched.add("State is unrestricted within the configured dataset.");
        }
        if (!s.additionalRules().isEmpty()) {
            for (EligibilityRule rule : s.additionalRules()) {
                if(!rule.required())continue;
                if(!rule.verified()){
                    verificationRequired=true;
                    missing.add("Scheme-specific rule is not verified: "+rule.description());
                    continue;
                }
                SchemeRuleEvaluator evaluator=ruleEvaluators.get(rule.code());
                if(evaluator==null){
                    verificationRequired=true;
                    missing.add("No evaluator is configured for required scheme rule: "+rule.description());
                    continue;
                }
                RuleEvaluation result=evaluator.evaluate(p);
                switch(result.outcome()){
                    case "PASS" -> matched.add(result.explanation());
                    case "FAIL" -> failed.add(result.explanation());
                    default -> missing.add(result.explanation());
                }
            }
        }
        boolean open = s.active() && (s.deadline() == null || !s.deadline().isBefore(LocalDate.now()));
        if (!s.active()) failed.add("Scholarship is inactive.");
        else if (s.deadline() != null && s.deadline().isBefore(LocalDate.now())) failed.add("Application deadline has passed.");
        else matched.add("Scholarship is active and application deadline is open.");
        String state = !failed.isEmpty() ? "INELIGIBLE" : (!missing.isEmpty() || verificationRequired ? "INDETERMINATE" : "ELIGIBLE");
        String explanation = switch (state) {
            case "INELIGIBLE" -> "One or more configured criteria are not met. Review failed criteria.";
            case "INDETERMINATE" -> "No configured failure was found, but required profile details or current scheme verification are incomplete. This is not confirmed government eligibility.";
            default -> "All configured and verified criteria are met. Confirm the current official scheme rules before applying.";
        };
        return new EligibilityResult(s.id(), s.name(), state, List.copyOf(matched), List.copyOf(failed),
                List.copyOf(missing), verificationRequired, open, explanation);
    }

    private void membership(String label, String value, Set<String> allowed, List<String> matched, List<String> failed, List<String> missing) {
        if (value == null || value.isBlank()) missing.add(label);
        else if (allowed.contains("*") || allowed.contains(value)) matched.add(label + " matches the configured criteria.");
        else failed.add(label + " does not match the permitted values.");
    }
    private void threshold(String label, java.math.BigDecimal value, java.math.BigDecimal limit, boolean minimum, List<String> matched, List<String> failed, List<String> missing) {
        if (limit == null) { if (value == null) missing.add(label); else matched.add(label + " recorded; no threshold configured."); return; }
        if (value == null) missing.add(label);
        else if (minimum ? value.compareTo(limit) >= 0 : value.compareTo(limit) <= 0) matched.add(label + " meets the inclusive " + (minimum ? "minimum" : "maximum") + " of " + limit + ".");
        else failed.add(label + " does not meet the inclusive " + (minimum ? "minimum" : "maximum") + " of " + limit + ".");
    }
    private void checkValue(String label, String actual, String expected, List<String> matched, List<String> failed, List<String> missing) {
        if (actual == null) missing.add(label); else if (actual.equals(expected)) matched.add(label + " matches."); else failed.add(label + " differs from the selected year.");
    }
}

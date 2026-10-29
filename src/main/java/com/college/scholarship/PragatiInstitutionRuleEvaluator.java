package com.college.scholarship;

import org.springframework.stereotype.Component;

@Component
public class PragatiInstitutionRuleEvaluator implements SchemeRuleEvaluator {
    @Override public String ruleCode(){return "PRAGATI_INSTITUTION_APPROVAL";}
    @Override public RuleEvaluation evaluate(StudentEligibilityProfile p){if(p==null||p.institutionApprovalStatus()==null)return RuleEvaluation.missing("Institution approval status is required.");if(!p.institutionApprovalVerified())return RuleEvaluation.missing("Institution approval status has not been verified by an administrator.");if(!p.institutionApprovalStatus().equals("APPROVED"))return RuleEvaluation.fail("Institution is not recorded as approved.");return RuleEvaluation.pass("Institution is recorded as administrator-verified and approved.");}
}

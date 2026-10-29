package com.college.scholarship;

import org.springframework.stereotype.Component;

@Component
public class PragatiAdmissionRouteRuleEvaluator implements SchemeRuleEvaluator {
    @Override public String ruleCode(){return "PRAGATI_ADMISSION_ROUTE";}
    @Override public RuleEvaluation evaluate(StudentEligibilityProfile p){if(p==null||p.admissionRoute()==null)return RuleEvaluation.missing("Admission route is required.");if(!p.admissionRouteVerified())return RuleEvaluation.missing("Admission route has not been verified by an administrator.");if(!p.admissionRoute().equals("REGULAR")&&!p.admissionRoute().equals("LATERAL_ENTRY"))return RuleEvaluation.missing("Admission route needs review against the current scheme rules.");return RuleEvaluation.pass("Admission route is administrator-verified; current entry/lateral-entry rules still need scheme confirmation.");}
}

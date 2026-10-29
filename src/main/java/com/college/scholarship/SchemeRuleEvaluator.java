package com.college.scholarship;

public interface SchemeRuleEvaluator {
    String ruleCode();
    RuleEvaluation evaluate(StudentEligibilityProfile profile);
}

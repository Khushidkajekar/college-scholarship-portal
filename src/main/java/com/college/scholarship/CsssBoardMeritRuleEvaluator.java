package com.college.scholarship;

import org.springframework.stereotype.Component;

@Component
public class CsssBoardMeritRuleEvaluator implements SchemeRuleEvaluator {
    @Override public String ruleCode(){return "CSSS_BOARD_MERIT";}
    @Override public RuleEvaluation evaluate(StudentEligibilityProfile p){if(p==null||p.qualifyingBoard()==null||p.qualifyingMeritPosition()==null)return RuleEvaluation.missing("Qualifying board and board-specific merit position are required.");if(!p.qualifyingConditionsVerified())return RuleEvaluation.missing("Qualifying examination conditions need institutional verification.");return RuleEvaluation.pass("Board-specific qualifying condition is recorded as institutionally verified.");}
}

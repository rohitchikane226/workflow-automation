package com.springrest.services;

import org.springframework.stereotype.Service;

import java.util.Map;
import com.springrest.Entities.ConditionRule;
@Service
public class ConditionService {

    public boolean evaluate(ConditionRule rule, Map<String, Object> data) {

        Object fieldValue = data.get(rule.getField());

        if (fieldValue == null) {
            return false;
        }

        switch (rule.getOperator()) {

            case "equals":
                return fieldValue.toString().equals(rule.getValue().toString());

            case "not_equals":
                return !fieldValue.toString().equals(rule.getValue().toString());

            case "greater_than":
                return Double.parseDouble(fieldValue.toString()) >
                        Double.parseDouble(rule.getValue().toString());

            case "less_than":
                return Double.parseDouble(fieldValue.toString()) <
                        Double.parseDouble(rule.getValue().toString());

            case "contains":
                return fieldValue.toString().contains(rule.getValue().toString());

            default:
                throw new RuntimeException("Unsupported operator: " + rule.getOperator());
        }
    }
}

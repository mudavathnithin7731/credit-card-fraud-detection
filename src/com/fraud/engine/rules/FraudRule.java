package com.fraud.engine.rules;

import com.fraud.model.CardholderProfile;
import com.fraud.model.Transaction;

/**
 * Strategy interface for fraud detection heuristic rules.
 */
public interface FraudRule {
    /**
     * Unique name identifying the rule.
     */
    String getRuleName();

    /**
     * Evaluates the transaction against the cardholder's profile.
     * @param transaction the incoming transaction to evaluate
     * @param profile the cardholder's historical profile
     * @return a risk score penalty (0.0 to 100.0) contributing to the rule score. 0 means no violation.
     */
    double evaluate(Transaction transaction, CardholderProfile profile);

    /**
     * Provides an explanatory violation message if the rule triggered.
     */
    String getViolationReason(Transaction transaction, CardholderProfile profile);
}

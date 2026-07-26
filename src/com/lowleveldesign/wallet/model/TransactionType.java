package com.lowleveldesign.wallet.model;

/**
 * The direction of a wallet transaction from the perspective of the account
 * whose statement it appears on.
 */
public enum TransactionType {

    /** Money was added to the account. */
    CREDIT("credit"),

    /** Money was removed from the account. */
    DEBIT("debit");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    /** @return the lower-case label used when printing statements. */
    public String getLabel() {
        return label;
    }
}

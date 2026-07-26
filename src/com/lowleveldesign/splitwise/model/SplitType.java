package com.lowleveldesign.splitwise.model;

/**
 * The strategy used to split an {@link Expense} among its participants.
 */
public enum SplitType {
    EQUAL,
    EXACT,
    PERCENT
}

package com.lowleveldesign.splitwise.exception;

import com.lowleveldesign.splitwise.model.SplitType;

/**
 * Thrown by {@code SplitStrategyFactory} when it is asked for a strategy for
 * a {@link SplitType} that has no registered implementation.
 */
public class UnsupportedSplitTypeException extends SplitwiseException {

    private static final long serialVersionUID = 1L;
    /**
     * @param type the unsupported split type (may be {@code null})
     */
    public UnsupportedSplitTypeException(SplitType type) {
        super("Unsupported split type: " + type);
    }
}

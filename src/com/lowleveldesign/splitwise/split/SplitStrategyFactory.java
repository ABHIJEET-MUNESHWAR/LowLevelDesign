package com.lowleveldesign.splitwise.split;

import com.lowleveldesign.splitwise.model.SplitType;

/**
 * Factory that maps a {@link SplitType} to its corresponding {@link SplitStrategy}.
 */
public final class SplitStrategyFactory {

    private SplitStrategyFactory() {
    }

    public static SplitStrategy getStrategy(SplitType type) {
        switch (type) {
            case EQUAL:
                return new EqualSplitStrategy();
            case EXACT:
                return new ExactSplitStrategy();
            case PERCENT:
                return new PercentSplitStrategy();
            default:
                throw new IllegalArgumentException("Unsupported split type: " + type);
        }
    }
}

package com.lowleveldesign.splitwise.split;

import com.lowleveldesign.splitwise.exception.UnsupportedSplitTypeException;
import com.lowleveldesign.splitwise.model.SplitType;

/**
 * Factory that maps a {@link SplitType} to its corresponding {@link SplitStrategy}.
 */
public final class SplitStrategyFactory {

    private SplitStrategyFactory() {
    }

    /**
     * Returns the {@link SplitStrategy} that implements the given split type.
     *
     * @param type the split type to resolve
     * @return a strategy instance for {@code type}
     * @throws UnsupportedSplitTypeException if {@code type} is {@code null} or has no implementation
     */
    public static SplitStrategy getStrategy(SplitType type) {
        if (type == null) {
            throw new UnsupportedSplitTypeException(null);
        }
        switch (type) {
            case EQUAL:
                return new EqualSplitStrategy();
            case EXACT:
                return new ExactSplitStrategy();
            case PERCENT:
                return new PercentSplitStrategy();
            default:
                throw new UnsupportedSplitTypeException(type);
        }
    }
}

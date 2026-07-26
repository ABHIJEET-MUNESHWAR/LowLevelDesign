package com.lowleveldesign.splitwise.split;

import com.lowleveldesign.splitwise.exception.InvalidSplitException;
import com.lowleveldesign.splitwise.model.Split;

import java.util.List;

/**
 * Uses the exact amounts supplied for each participant. The amounts must
 * add up to the total expense amount.
 */
public class ExactSplitStrategy implements SplitStrategy {

    private static final double EPSILON = 0.01;

    @Override
    public void validateAndCompute(double totalAmount, List<Split> splits) {
        if (splits == null || splits.isEmpty()) {
            throw new InvalidSplitException("At least one participant is required for an exact split");
        }
        double sum = splits.stream().mapToDouble(Split::getAmount).sum();
        if (Math.abs(sum - totalAmount) > EPSILON) {
            throw new InvalidSplitException(
                    String.format("Sum of exact shares (%.2f) does not match total expense amount (%.2f)", sum, totalAmount));
        }
    }
}

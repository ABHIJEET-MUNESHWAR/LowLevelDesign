package com.lowleveldesign.splitwise.split;

import com.lowleveldesign.splitwise.exception.InvalidSplitException;
import com.lowleveldesign.splitwise.model.Split;

import java.util.List;

/**
 * Splits the total amount based on a percentage assigned to each
 * participant. Percentages must add up to 100%.
 */
public class PercentSplitStrategy implements SplitStrategy {

    private static final double EPSILON = 0.01;

    /**
     * Validates that the participants' percentages sum to 100 (within
     * {@link #EPSILON}) and computes each participant's amount as a
     * percentage of {@code totalAmount}.
     *
     * @param totalAmount the total amount to split
     * @param splits      the participants with pre-set percentages
     * @throws InvalidSplitException if {@code splits} is null/empty, any
     *                               percent is missing, or percentages don't sum to 100
     */
    @Override
    public void validateAndCompute(double totalAmount, List<Split> splits) {
        if (splits == null || splits.isEmpty()) {
            throw new InvalidSplitException("At least one participant is required for a percent split");
        }
        double percentSum = 0.0;
        for (Split split : splits) {
            if (split.getPercent() == null) {
                throw new InvalidSplitException("Percent must be specified for every participant");
            }
            percentSum += split.getPercent();
        }
        if (Math.abs(percentSum - 100.0) > EPSILON) {
            throw new InvalidSplitException(
                    String.format("Sum of percentages (%.2f) must equal 100", percentSum));
        }
        for (Split split : splits) {
            double amount = Math.round(totalAmount * split.getPercent() / 100.0 * 100) / 100.0;
            split.setAmount(amount);
        }
    }
}

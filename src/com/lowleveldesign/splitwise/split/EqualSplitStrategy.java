package com.lowleveldesign.splitwise.split;

import com.lowleveldesign.splitwise.exception.InvalidSplitException;
import com.lowleveldesign.splitwise.model.Split;

import java.util.List;

/**
 * Splits the total amount equally among all participants. Any remainder
 * (due to rounding to 2 decimal places) is added to the first participant's
 * share so that the sum of splits always equals the total amount exactly.
 */
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public void validateAndCompute(double totalAmount, List<Split> splits) {
        if (splits == null || splits.isEmpty()) {
            throw new InvalidSplitException("At least one participant is required for an equal split");
        }
        int n = splits.size();
        double share = Math.floor((totalAmount / n) * 100) / 100.0;
        double distributed = 0.0;
        for (int i = 0; i < n; i++) {
            splits.get(i).setAmount(share);
            distributed += share;
        }
        // Assign any leftover cents (rounding remainder) to the first split.
        double remainder = Math.round((totalAmount - distributed) * 100) / 100.0;
        if (remainder != 0.0) {
            Split first = splits.get(0);
            first.setAmount(Math.round((first.getAmount() + remainder) * 100) / 100.0);
        }
    }
}

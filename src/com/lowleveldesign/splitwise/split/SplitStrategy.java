package com.lowleveldesign.splitwise.split;

import com.lowleveldesign.splitwise.model.Split;

import java.util.List;

/**
 * Strategy pattern: validates a list of {@link Split}s against the total
 * expense amount and fills in the final {@code amount} owed by each user.
 */
public interface SplitStrategy {

    /**
     * Validates the given splits and, where necessary, computes the amount
     * owed by each participant.
     *
     * @param totalAmount total amount of the expense
     * @param splits      the participants' shares (input format depends on split type)
     * @throws com.lowleveldesign.splitwise.exception.InvalidSplitException if the splits are invalid
     */
    void validateAndCompute(double totalAmount, List<Split> splits);
}

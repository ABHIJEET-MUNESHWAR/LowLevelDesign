package com.lowleveldesign.wallet.model;

import com.lowleveldesign.wallet.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility helpers for working with FkRupee (F&#8377;) monetary amounts.
 * <p>
 * The currency's smallest unit is F&#8377; 0.0001, so all amounts are kept to a
 * maximum of four decimal places. {@link BigDecimal} is used throughout to
 * avoid the rounding errors that plague binary floating point arithmetic.
 */
public final class Money {

    /** The smallest transferable amount: F&#8377; 0.0001. */
    public static final BigDecimal SMALLEST_UNIT = new BigDecimal("0.0001");

    /** Number of decimal places the currency supports. */
    public static final int SCALE = 4;

    private Money() {
    }

    /**
     * Parses a textual amount into a normalized {@link BigDecimal}, rejecting
     * anything that is not a valid non-negative amount aligned to the currency's
     * smallest unit.
     *
     * @param text the raw amount token (e.g. {@code "95.7"})
     * @return the parsed amount, scaled to {@link #SCALE} decimals
     * @throws InvalidAmountException if the text is not a number, is negative, or
     *                                has finer precision than {@link #SMALLEST_UNIT}
     */
    public static BigDecimal parse(String text) {
        BigDecimal value;
        try {
            value = new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("'" + text + "' is not a valid amount");
        }
        if (value.scale() > SCALE) {
            throw new InvalidAmountException(
                    "Amount '" + text + "' is finer than the smallest unit F\u20B9 0.0001");
        }
        if (value.signum() < 0) {
            throw new InvalidAmountException("Amount must not be negative: " + text);
        }
        return normalize(value);
    }

    /**
     * Normalizes an amount to the currency's fixed scale.
     *
     * @param value the amount to normalize
     * @return the amount rescaled to {@link #SCALE} decimals
     */
    public static BigDecimal normalize(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Formats an amount for display, trimming insignificant trailing zeros so
     * that, e.g., {@code 100.0000} prints as {@code 100} and {@code 95.7000} as
     * {@code 95.7}.
     *
     * @param value the amount to format
     * @return a compact, human-readable representation
     */
    public static String format(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString();
    }
}

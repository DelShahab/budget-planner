package com.budgetplanner.budget.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for formatting currency amounts in a user-friendly way
 */
public class CurrencyFormatter {
    
    private static final NumberFormat USD_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    
    // Private constructor to prevent instantiation
    private CurrencyFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Format amount as USD currency with $ symbol
     * Example: 1234.56 -> "$1,234.56"
     */
    public static String formatUSD(double amount) {
        return USD_FORMAT.format(amount);
    }
    
    /**
     * Format amount as USD currency with $ symbol
     * Example: 1234 -> "$1,234.00"
     */
    public static String formatUSD(int amount) {
        return USD_FORMAT.format(amount);
    }
    
    /**
     * Format amount with commas and 2 decimal places, no currency symbol
     * Example: 1234.56 -> "1,234.56"
     */
    public static String formatAmount(double amount) {
        return DECIMAL_FORMAT.format(amount);
    }
    
    /**
     * Format amount with commas and 2 decimal places, no currency symbol
     * Example: 1234 -> "1,234.00"
     */
    public static String formatAmount(int amount) {
        return DECIMAL_FORMAT.format(amount);
    }
    
    /**
     * Format amount as compact currency (K, M, B suffixes)
     * Example: 1234.56 -> "$1.2K", 1234567 -> "$1.2M"
     */
    public static String formatCompactUSD(double amount) {
        if (Math.abs(amount) >= 1_000_000_000) {
            return String.format("$%.1fB", amount / 1_000_000_000);
        } else if (Math.abs(amount) >= 1_000_000) {
            return String.format("$%.1fM", amount / 1_000_000);
        } else if (Math.abs(amount) >= 1_000) {
            return String.format("$%.1fK", amount / 1_000);
        } else {
            return formatUSD(amount);
        }
    }
    
    /**
     * Format amount with currency symbol and proper sign for positive/negative
     * Example: -1234.56 -> "-$1,234.56", 1234.56 -> "$1,234.56"
     */
    public static String formatSignedUSD(double amount) {
        if (amount < 0) {
            return "-" + USD_FORMAT.format(Math.abs(amount));
        }
        return USD_FORMAT.format(amount);
    }
    
    /**
     * Format percentage with 1 decimal place
     * Example: 0.1234 -> "12.3%"
     */
    public static String formatPercentage(double percentage) {
        return String.format("%.1f%%", percentage * 100);
    }
}

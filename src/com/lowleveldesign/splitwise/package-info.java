/**
 * Low Level Design of Splitwise - an expense sharing application.
 * <p>
 * Supports:
 * <ul>
 *     <li>Adding users and groups</li>
 *     <li>Adding expenses with EQUAL, EXACT and PERCENT split strategies</li>
 *     <li>Maintaining a pairwise balance sheet between users</li>
 *     <li>Settling up balances between two users</li>
 *     <li>Simplifying debts within a group to minimize the number of transactions</li>
 * </ul>
 * Package layout:
 * <ul>
 *     <li>{@code model} - core domain entities (User, Group, Expense, Split, SplitType)</li>
 *     <li>{@code split} - Strategy pattern implementations for the different split types</li>
 *     <li>{@code service} - BalanceSheet and SplitwiseService (facade) that ties everything together</li>
 *     <li>{@code exception} - domain specific exceptions</li>
 * </ul>
 */
package com.lowleveldesign.splitwise;

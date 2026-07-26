/**
 * Low Level Design of a Digital Wallet / Bank Account application.
 * <p>
 * People own wallets denominated in the system's own currency, FkRupee
 * (F&#8377;), and transfer money between them. No balance may ever fall below
 * zero and the smallest transferable amount is F&#8377; 0.0001.
 * <p>
 * Supported commands:
 * <ul>
 *     <li>{@code CreateWallet <name> <balance>} - open a wallet</li>
 *     <li>{@code TransferMoney <from> <to> <amount>} - move money between wallets</li>
 *     <li>{@code Statement <name>} - print a wallet's transaction history</li>
 *     <li>{@code Overview} - print every wallet's current balance</li>
 *     <li>{@code Offer2} - reward the three most active customers</li>
 *     <li>{@code FixedDeposit <name> <amount>} - park a fixed deposit (bonus)</li>
 * </ul>
 * Offers:
 * <ul>
 *     <li>Offer1 - both parties to a transfer that leaves them with equal
 *     balances earn F&#8377; 10 each.</li>
 *     <li>Offer2 - the top three customers by transaction count earn F&#8377; 10,
 *     F&#8377; 5 and F&#8377; 2, with ties broken by higher balance then earlier
 *     wallet creation.</li>
 * </ul>
 * Package layout:
 * <ul>
 *     <li>{@code model} - core entities (Wallet, Transaction, TransactionType,
 *     FixedDeposit) and the Money helper</li>
 *     <li>{@code service} - WalletService (facade) and CommandProcessor</li>
 *     <li>{@code exception} - domain specific exceptions</li>
 * </ul>
 * All state is held in memory; nothing is persisted to files or databases.
 */
package com.lowleveldesign.wallet;

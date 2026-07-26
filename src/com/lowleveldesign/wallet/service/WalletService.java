package com.lowleveldesign.wallet.service;

import com.lowleveldesign.wallet.exception.InsufficientBalanceException;
import com.lowleveldesign.wallet.exception.InvalidAmountException;
import com.lowleveldesign.wallet.exception.WalletAlreadyExistsException;
import com.lowleveldesign.wallet.exception.WalletNotFoundException;
import com.lowleveldesign.wallet.model.FixedDeposit;
import com.lowleveldesign.wallet.model.Money;
import com.lowleveldesign.wallet.model.Transaction;
import com.lowleveldesign.wallet.model.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory facade for the digital wallet system. This is the main entry point
 * client code should use.
 * <p>
 * It owns every {@link Wallet} and implements the full command set:
 * {@code CreateWallet}, {@code TransferMoney}, {@code Statement},
 * {@code Overview}, {@code Offer2} and the bonus {@code FixedDeposit}, along
 * with the automatic Offer1 reward that fires on qualifying transfers.
 */
public class WalletService {

    /** Reward paid to both parties when Offer1 triggers: F&#8377; 10. */
    public static final BigDecimal OFFER1_REWARD = new BigDecimal("10");

    /** Rewards paid to the top three customers when Offer2 is fired. */
    public static final BigDecimal[] OFFER2_REWARDS = {
            new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("2")
    };

    private static final String OFFER1_SOURCE = "Offer1";
    private static final String OFFER2_SOURCE = "Offer2";
    private static final String FIXED_DEPOSIT_SOURCE = "FixedDeposit";

    private final Map<String, Wallet> wallets = new LinkedHashMap<>();
    private int creationCounter = 0;

    /**
     * Creates a new wallet for an account holder.
     *
     * @param accountHolder  the account holder's name
     * @param openingBalance the opening balance (must be &gt;= 0)
     * @return the newly created wallet
     * @throws InvalidAmountException        if the name is blank or the balance is negative
     * @throws WalletAlreadyExistsException  if the account holder already has a wallet
     */
    public Wallet createWallet(String accountHolder, BigDecimal openingBalance) {
        if (accountHolder == null || accountHolder.trim().isEmpty()) {
            throw new InvalidAmountException("Account holder name must not be empty");
        }
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new InvalidAmountException("Opening balance must not be negative");
        }
        if (wallets.containsKey(accountHolder)) {
            throw new WalletAlreadyExistsException(accountHolder);
        }
        Wallet wallet = new Wallet(accountHolder, openingBalance, creationCounter++);
        wallets.put(accountHolder, wallet);
        return wallet;
    }

    /**
     * Transfers money from one wallet to another, then applies Offer1 and
     * advances any fixed deposits held by the two parties.
     *
     * @param fromHolder the payer's account holder name
     * @param toHolder   the payee's account holder name
     * @param amount     the amount to transfer (must be &gt;= F&#8377; 0.0001)
     * @throws WalletNotFoundException      if either wallet does not exist
     * @throws InvalidAmountException       if the amount is below the smallest unit or the parties are the same
     * @throws InsufficientBalanceException if the payer lacks sufficient funds
     */
    public void transferMoney(String fromHolder, String toHolder, BigDecimal amount) {
        Wallet from = requireWallet(fromHolder);
        Wallet to = requireWallet(toHolder);
        if (from == to) {
            throw new InvalidAmountException("Cannot transfer money to the same wallet: " + fromHolder);
        }
        requireTransferable(amount);

        from.debit(to.getAccountHolder(), amount, true);
        to.credit(from.getAccountHolder(), amount, true);

        applyOffer1(from, to);

        advanceFixedDeposit(from);
        advanceFixedDeposit(to);
    }

    /**
     * Offer1: if both parties end up with exactly the same balance after a
     * transfer, each receives {@link #OFFER1_REWARD}.
     */
    private void applyOffer1(Wallet from, Wallet to) {
        if (from.getBalance().compareTo(to.getBalance()) == 0) {
            from.credit(OFFER1_SOURCE, OFFER1_REWARD, false);
            to.credit(OFFER1_SOURCE, OFFER1_REWARD, false);
        }
    }

    /**
     * Advances the fixed deposit of a wallet that has just taken part in a
     * transfer: dissolves it if the balance has dropped below the parked amount,
     * or pays interest and matures it once it has survived the required number
     * of transactions.
     */
    private void advanceFixedDeposit(Wallet wallet) {
        FixedDeposit fd = wallet.getFixedDeposit();
        if (fd == null) {
            return;
        }
        if (wallet.getBalance().compareTo(fd.getAmount()) < 0) {
            wallet.setFixedDeposit(null);
            return;
        }
        if (fd.countTransaction()) {
            wallet.credit(FIXED_DEPOSIT_SOURCE, FixedDeposit.INTEREST, false);
            wallet.setFixedDeposit(null);
        }
    }

    /**
     * Offer2: awards F&#8377; 10, F&#8377; 5 and F&#8377; 2 to the three customers with the most
     * transactions. Ties are broken first by higher balance, then by earlier
     * wallet creation.
     */
    public void applyOffer2() {
        List<Wallet> ranked = new ArrayList<>(wallets.values());
        ranked.sort(Comparator
                .comparingInt(Wallet::getTransferCount).reversed()
                .thenComparing(Comparator.comparing(Wallet::getBalance).reversed())
                .thenComparingInt(Wallet::getCreationOrder));

        int limit = Math.min(OFFER2_REWARDS.length, ranked.size());
        for (int i = 0; i < limit; i++) {
            ranked.get(i).credit(OFFER2_SOURCE, OFFER2_REWARDS[i], false);
        }
    }

    /**
     * Opens (or replaces) a fixed deposit on a wallet. The parked amount must be
     * positive and no greater than the wallet's current balance.
     *
     * @param accountHolder the account holder's name
     * @param amount        the amount to park
     * @throws WalletNotFoundException      if the wallet does not exist
     * @throws InvalidAmountException       if the amount is below the smallest unit
     * @throws InsufficientBalanceException if the amount exceeds the current balance
     */
    public void openFixedDeposit(String accountHolder, BigDecimal amount) {
        Wallet wallet = requireWallet(accountHolder);
        requireTransferable(amount);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountHolder);
        }
        wallet.setFixedDeposit(new FixedDeposit(amount));
    }

    /**
     * Returns the statement (full transaction history) of a wallet.
     *
     * @param accountHolder the account holder's name
     * @return the chronological list of transactions
     * @throws WalletNotFoundException if the wallet does not exist
     */
    public List<Transaction> getStatement(String accountHolder) {
        return requireWallet(accountHolder).getTransactions();
    }

    /**
     * Returns every wallet in the order the wallets were created, for the
     * Overview command.
     *
     * @return an ordered collection of all wallets
     */
    public Collection<Wallet> getAllWallets() {
        return wallets.values();
    }

    /**
     * Looks up a wallet by account holder name.
     *
     * @param accountHolder the account holder's name
     * @return the wallet
     * @throws WalletNotFoundException if no such wallet exists
     */
    public Wallet requireWallet(String accountHolder) {
        Wallet wallet = wallets.get(accountHolder);
        if (wallet == null) {
            throw new WalletNotFoundException(accountHolder == null ? "null" : accountHolder);
        }
        return wallet;
    }

    private void requireTransferable(BigDecimal amount) {
        if (amount == null || amount.compareTo(Money.SMALLEST_UNIT) < 0) {
            throw new InvalidAmountException(
                    "Amount must be at least the smallest unit F\u20B9 0.0001");
        }
    }
}

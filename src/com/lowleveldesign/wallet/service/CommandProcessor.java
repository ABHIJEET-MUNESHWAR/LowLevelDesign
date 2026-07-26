package com.lowleveldesign.wallet.service;

import com.lowleveldesign.wallet.exception.InvalidCommandException;
import com.lowleveldesign.wallet.exception.WalletException;
import com.lowleveldesign.wallet.model.FixedDeposit;
import com.lowleveldesign.wallet.model.Money;
import com.lowleveldesign.wallet.model.Transaction;
import com.lowleveldesign.wallet.model.Wallet;

import java.io.PrintStream;
import java.util.List;

/**
 * Translates textual commands into calls on a {@link WalletService} and prints
 * their results, making the system demo-able from a plain string, a file or the
 * command line.
 * <p>
 * Supported commands (one per line):
 * <ul>
 *     <li>{@code CreateWallet <name> <balance>}</li>
 *     <li>{@code TransferMoney <from> <to> <amount>}</li>
 *     <li>{@code Statement <name>}</li>
 *     <li>{@code Overview}</li>
 *     <li>{@code Offer2}</li>
 *     <li>{@code FixedDeposit <name> <amount>}</li>
 * </ul>
 * Any {@link WalletException} raised while running a command is caught and
 * reported so that a batch of commands keeps going.
 */
public class CommandProcessor {

    private final WalletService service;
    private final PrintStream out;

    /**
     * Creates a processor writing to {@link System#out}.
     *
     * @param service the wallet service to drive
     */
    public CommandProcessor(WalletService service) {
        this(service, System.out);
    }

    /**
     * Creates a processor writing to a custom stream (useful for testing).
     *
     * @param service the wallet service to drive
     * @param out     the stream to print results to
     */
    public CommandProcessor(WalletService service, PrintStream out) {
        this.service = service;
        this.out = out;
    }

    /**
     * Runs a block of newline-separated commands. Blank lines and lines starting
     * with {@code #} (comments) are ignored.
     *
     * @param script the multi-line command script
     */
    public void runScript(String script) {
        if (script == null) {
            return;
        }
        for (String line : script.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            process(trimmed);
        }
    }

    /**
     * Executes a single command line.
     *
     * @param line the command to run
     */
    public void process(String line) {
        String[] parts = line.trim().split("\\s+");
        String command = parts[0];
        try {
            switch (command) {
                case "CreateWallet":
                    handleCreateWallet(parts);
                    break;
                case "TransferMoney":
                    handleTransferMoney(parts);
                    break;
                case "Statement":
                    handleStatement(parts);
                    break;
                case "Overview":
                    handleOverview(parts);
                    break;
                case "Offer2":
                    handleOffer2(parts);
                    break;
                case "FixedDeposit":
                    handleFixedDeposit(parts);
                    break;
                default:
                    throw new InvalidCommandException("Unknown command: " + command);
            }
        } catch (WalletException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void handleCreateWallet(String[] parts) {
        requireArgs(parts, 3, "CreateWallet <name> <balance>");
        service.createWallet(parts[1], Money.parse(parts[2]));
    }

    private void handleTransferMoney(String[] parts) {
        requireArgs(parts, 4, "TransferMoney <from> <to> <amount>");
        service.transferMoney(parts[1], parts[2], Money.parse(parts[3]));
    }

    private void handleStatement(String[] parts) {
        requireArgs(parts, 2, "Statement <name>");
        Wallet wallet = service.requireWallet(parts[1]);
        List<Transaction> statement = service.getStatement(parts[1]);
        for (Transaction transaction : statement) {
            out.println(transaction);
        }
        printFixedDeposit(wallet);
    }

    private void handleOverview(String[] parts) {
        requireArgs(parts, 1, "Overview");
        for (Wallet wallet : service.getAllWallets()) {
            StringBuilder sb = new StringBuilder();
            sb.append(wallet.getAccountHolder()).append(' ').append(Money.format(wallet.getBalance()));
            FixedDeposit fd = wallet.getFixedDeposit();
            if (fd != null) {
                sb.append(" | FD ").append(Money.format(fd.getAmount()))
                        .append(", ").append(fd.getRemainingTransactions()).append(" txns left");
            }
            out.println(sb.toString());
        }
    }

    private void handleOffer2(String[] parts) {
        requireArgs(parts, 1, "Offer2");
        service.applyOffer2();
    }

    private void handleFixedDeposit(String[] parts) {
        requireArgs(parts, 3, "FixedDeposit <name> <amount>");
        service.openFixedDeposit(parts[1], Money.parse(parts[2]));
    }

    private void printFixedDeposit(Wallet wallet) {
        FixedDeposit fd = wallet.getFixedDeposit();
        if (fd != null) {
            out.println("FixedDeposit " + Money.format(fd.getAmount())
                    + " (" + fd.getRemainingTransactions() + " txns remaining)");
        }
    }

    private void requireArgs(String[] parts, int expected, String usage) {
        if (parts.length != expected) {
            throw new InvalidCommandException("Usage: " + usage);
        }
    }
}

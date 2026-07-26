package com.lowleveldesign.wallet;

import com.lowleveldesign.wallet.service.CommandProcessor;
import com.lowleveldesign.wallet.service.WalletService;

/**
 * Demonstrates the digital wallet system by running the sample script from the
 * problem statement plus a short fixed-deposit (bonus) scenario.
 * <p>
 * The entire input is hard-coded as a single string and fed to the
 * {@link CommandProcessor}, keeping the demo self-contained and free of any
 * file or database access.
 */
public final class Main {

    private Main() {
    }

    /**
     * Runs the demonstration.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        CommandProcessor processor = new CommandProcessor(new WalletService());

        System.out.println("==== Create wallets, then Overview ====");
        processor.runScript(String.join("\n",
                "CreateWallet Harry 100",
                "CreateWallet Ron 95.7",
                "CreateWallet Hermione 104",
                "CreateWallet Albus 200",
                "CreateWallet Draco 500",
                "Overview"));

        System.out.println("\n==== Three transfers (Offer1 fires on Hermione->Harry), then Overview ====");
        processor.runScript(String.join("\n",
                "TransferMoney Albus Draco 30",
                "TransferMoney Hermione Harry 2",
                "TransferMoney Albus Ron 5",
                "Overview"));

        System.out.println("\n==== Statement Harry ====");
        processor.process("Statement Harry");

        System.out.println("\n==== Statement Albus ====");
        processor.process("Statement Albus");

        System.out.println("\n==== Offer2, then Overview ====");
        processor.runScript(String.join("\n",
                "Offer2",
                "Overview"));

        System.out.println("\n==== Bonus: FixedDeposit lifecycle ====");
        CommandProcessor bonus = new CommandProcessor(new WalletService());
        bonus.runScript(String.join("\n",
                "CreateWallet Alice 100",
                "CreateWallet Bob 100",
                "CreateWallet Carol 100",
                "FixedDeposit Alice 90",
                "# Alice keeps her balance at or above 90 across five transfers -> earns interest",
                "TransferMoney Bob Alice 5",
                "TransferMoney Alice Carol 5",
                "TransferMoney Carol Alice 5",
                "TransferMoney Bob Alice 5",
                "TransferMoney Alice Carol 5",
                "Overview",
                "Statement Alice"));

        System.out.println("\n==== Bonus: FixedDeposit dissolved when balance drops below the parked amount ====");
        CommandProcessor dissolve = new CommandProcessor(new WalletService());
        dissolve.runScript(String.join("\n",
                "CreateWallet Dan 100",
                "CreateWallet Eve 100",
                "FixedDeposit Dan 90",
                "TransferMoney Dan Eve 20",
                "Overview"));
    }
}

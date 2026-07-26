package com.lowleveldesign.wallet.test;

import com.lowleveldesign.wallet.exception.InsufficientBalanceException;
import com.lowleveldesign.wallet.exception.InvalidAmountException;
import com.lowleveldesign.wallet.exception.WalletAlreadyExistsException;
import com.lowleveldesign.wallet.exception.WalletNotFoundException;
import com.lowleveldesign.wallet.model.Money;
import com.lowleveldesign.wallet.model.Wallet;
import com.lowleveldesign.wallet.service.CommandProcessor;
import com.lowleveldesign.wallet.service.WalletService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

/**
 * Dependency-free correctness test suite for the digital wallet package (no
 * JUnit required, targets Java 8). Run via
 * {@code java com.lowleveldesign.wallet.test.TestRunner}; exits non-zero on
 * failure.
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    private TestRunner() {
    }

    /**
     * Runs every test and prints a pass/fail line per test plus a final tally.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        run("sample scenario reproduces the expected output", TestRunner::testSampleScenario);
        run("overview lists wallets in creation order", TestRunner::testOverviewOrder);
        run("balances format without trailing zeros", TestRunner::testBalanceFormatting);
        run("Offer1 fires only when post-transfer balances are equal", TestRunner::testOffer1);
        run("Offer2 rewards by count then balance then creation order", TestRunner::testOffer2TieBreaks);
        run("Offer2 with fewer than three wallets rewards all present", TestRunner::testOffer2FewWallets);
        run("transfer below the smallest unit (zero) is rejected", TestRunner::testSmallestUnit);
        run("transfer exceeding balance throws InsufficientBalanceException", TestRunner::testInsufficientBalance);
        run("transfer to self is rejected", TestRunner::testSelfTransfer);
        run("duplicate wallet creation throws", TestRunner::testDuplicateWallet);
        run("operating on an unknown wallet throws", TestRunner::testUnknownWallet);
        run("negative opening balance is rejected", TestRunner::testNegativeOpeningBalance);
        run("amount finer than the smallest unit is rejected", TestRunner::testTooFinePrecision);
        run("smallest unit transfer of 0.0001 is allowed and exact", TestRunner::testSmallestUnitExact);
        run("fixed deposit pays interest after five surviving transactions", TestRunner::testFixedDepositMatures);
        run("fixed deposit dissolves when balance drops below parked amount", TestRunner::testFixedDepositDissolves);
        run("fixed deposit appears in overview and statement", TestRunner::testFixedDepositDisplayed);
        run("opening a fixed deposit above balance throws", TestRunner::testFixedDepositTooLarge);

        System.out.printf("%n%d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testSampleScenario() {
        WalletService service = new WalletService();
        String create = capture(service, String.join("\n",
                "CreateWallet Harry 100",
                "CreateWallet Ron 95.7",
                "CreateWallet Hermione 104",
                "CreateWallet Albus 200",
                "CreateWallet Draco 500",
                "Overview"));
        assertEquals("Harry 100\nRon 95.7\nHermione 104\nAlbus 200\nDraco 500", create, "initial overview");

        String afterTransfers = capture(service, String.join("\n",
                "TransferMoney Albus Draco 30",
                "TransferMoney Hermione Harry 2",
                "TransferMoney Albus Ron 5",
                "Overview"));
        assertEquals("Harry 112\nRon 100.7\nHermione 112\nAlbus 165\nDraco 530", afterTransfers, "overview after transfers");

        assertEquals("Hermione credit 2\nOffer1 credit 10", capture(service, "Statement Harry"), "Harry statement");
        assertEquals("Draco debit 30\nRon debit 5", capture(service, "Statement Albus"), "Albus statement");

        String afterOffer2 = capture(service, String.join("\n", "Offer2", "Overview"));
        assertEquals("Harry 114\nRon 100.7\nHermione 112\nAlbus 175\nDraco 535", afterOffer2, "overview after Offer2");
    }

    private static void testOverviewOrder() {
        WalletService service = new WalletService();
        service.createWallet("Zed", bd("1"));
        service.createWallet("Ann", bd("2"));
        service.createWallet("Mia", bd("3"));
        String overview = capture(service, "Overview");
        assertEquals("Zed 1\nAnn 2\nMia 3", overview, "creation-order overview");
    }

    private static void testBalanceFormatting() {
        assertEquals("100", Money.format(bd("100.0000")), "trailing zeros trimmed");
        assertEquals("95.7", Money.format(bd("95.7000")), "single decimal kept");
        assertEquals("0.0001", Money.format(bd("0.0001")), "smallest unit");
        assertEquals("0", Money.format(bd("0")), "zero");
    }

    private static void testOffer1() {
        WalletService service = new WalletService();
        service.createWallet("A", bd("100"));
        service.createWallet("B", bd("104"));
        service.transferMoney("A", "B", bd("1")); // 99 vs 105 -> no offer
        assertEquals(bd("99"), service.requireWallet("A").getBalance(), "no offer A");
        service.createWallet("C", bd("100"));
        service.createWallet("D", bd("104"));
        service.transferMoney("D", "C", bd("2")); // both 102 -> offer1 -> both 112
        assertEquals(bd("112"), service.requireWallet("C").getBalance(), "offer1 C");
        assertEquals(bd("112"), service.requireWallet("D").getBalance(), "offer1 D");
    }

    private static void testOffer2TieBreaks() {
        WalletService service = new WalletService();
        service.createWallet("First", bd("100"));
        service.createWallet("Second", bd("100"));
        service.createWallet("Rich", bd("1000"));
        // Give everyone exactly one transaction so counts tie at 1.
        service.transferMoney("Rich", "First", bd("10"));   // Rich & First: 1 txn each
        service.transferMoney("First", "Second", bd("10")); // First now 2, Second 1
        // Counts: First=2, Rich=1, Second=1. Balances: Rich 990, Second 110, First 100.
        service.applyOffer2();
        // First (most txns) +10, then tie Rich/Second broken by balance -> Rich +5, Second +2.
        assertEquals(bd("110"), service.requireWallet("First").getBalance(), "First +10");
        assertEquals(bd("995"), service.requireWallet("Rich").getBalance(), "Rich +5");
        assertEquals(bd("112"), service.requireWallet("Second").getBalance(), "Second +2");
    }

    private static void testOffer2FewWallets() {
        WalletService service = new WalletService();
        service.createWallet("Solo", bd("100"));
        service.createWallet("Duo", bd("50"));
        service.applyOffer2(); // both tie at 0 txns; Solo higher balance -> +10, Duo -> +5
        assertEquals(bd("110"), service.requireWallet("Solo").getBalance(), "Solo +10");
        assertEquals(bd("55"), service.requireWallet("Duo").getBalance(), "Duo +5");
    }

    private static void testSmallestUnit() {
        WalletService service = twoWallets();
        expect(InvalidAmountException.class, () -> service.transferMoney("A", "B", bd("0")));
    }

    private static void testInsufficientBalance() {
        WalletService service = twoWallets();
        expect(InsufficientBalanceException.class, () -> service.transferMoney("A", "B", bd("1000")));
    }

    private static void testSelfTransfer() {
        WalletService service = twoWallets();
        expect(InvalidAmountException.class, () -> service.transferMoney("A", "A", bd("1")));
    }

    private static void testDuplicateWallet() {
        WalletService service = new WalletService();
        service.createWallet("A", bd("1"));
        expect(WalletAlreadyExistsException.class, () -> service.createWallet("A", bd("2")));
    }

    private static void testUnknownWallet() {
        WalletService service = new WalletService();
        expect(WalletNotFoundException.class, () -> service.requireWallet("Ghost"));
    }

    private static void testNegativeOpeningBalance() {
        WalletService service = new WalletService();
        expect(InvalidAmountException.class, () -> service.createWallet("A", bd("-1")));
    }

    private static void testTooFinePrecision() {
        expect(InvalidAmountException.class, () -> Money.parse("1.00001"));
    }

    private static void testSmallestUnitExact() {
        WalletService service = twoWallets();
        service.transferMoney("A", "B", bd("0.0001"));
        assertEquals(bd("99.9999"), service.requireWallet("A").getBalance(), "A after smallest transfer");
        assertEquals(bd("100.0001"), service.requireWallet("B").getBalance(), "B after smallest transfer");
    }

    private static void testFixedDepositMatures() {
        WalletService service = new WalletService();
        service.createWallet("Alice", bd("100"));
        service.createWallet("Bob", bd("100"));
        service.openFixedDeposit("Alice", bd("90"));
        for (int i = 0; i < 5; i++) {
            service.transferMoney("Bob", "Alice", bd("1")); // Alice climbs, stays >= 90
        }
        Wallet alice = service.requireWallet("Alice");
        assertEquals(bd("115"), alice.getBalance(), "100 + 5 transfers + 10 interest");
        assertTrue(alice.getFixedDeposit() == null, "FD cleared after maturity");
    }

    private static void testFixedDepositDissolves() {
        WalletService service = new WalletService();
        service.createWallet("Dan", bd("100"));
        service.createWallet("Eve", bd("100"));
        service.openFixedDeposit("Dan", bd("90"));
        service.transferMoney("Dan", "Eve", bd("20")); // Dan -> 80 < 90 -> dissolved
        Wallet dan = service.requireWallet("Dan");
        assertEquals(bd("80"), dan.getBalance(), "no interest after dissolution");
        assertTrue(dan.getFixedDeposit() == null, "FD dissolved");
    }

    private static void testFixedDepositDisplayed() {
        WalletService service = new WalletService();
        service.createWallet("Alice", bd("100"));
        service.createWallet("Bob", bd("100"));
        service.openFixedDeposit("Alice", bd("90"));
        service.transferMoney("Bob", "Alice", bd("1")); // one transfer used, 4 remain
        assertEquals("Alice 101 | FD 90, 4 txns left\nBob 99", capture(service, "Overview"), "overview shows FD");
        assertEquals("Bob credit 1\nFixedDeposit 90 (4 txns remaining)",
                capture(service, "Statement Alice"), "statement shows FD");
    }

    private static void testFixedDepositTooLarge() {
        WalletService service = new WalletService();
        service.createWallet("A", bd("100"));
        expect(InsufficientBalanceException.class, () -> service.openFixedDeposit("A", bd("150")));
    }

    // ---- helpers ----

    private static WalletService twoWallets() {
        WalletService service = new WalletService();
        service.createWallet("A", bd("100"));
        service.createWallet("B", bd("100"));
        return service;
    }

    private static BigDecimal bd(String value) {
        return Money.normalize(new BigDecimal(value));
    }

    private static String capture(WalletService service, String script) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            PrintStream stream = new PrintStream(buffer, true, "UTF-8");
            new CommandProcessor(service, stream).runScript(script);
            return buffer.toString("UTF-8").replace("\r\n", "\n").trim();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (AssertionError | RuntimeException e) {
            failed++;
            System.out.println("[FAIL] " + name + " -> " + e.getMessage());
        }
    }

    private static void assertEquals(Object expected, Object actual, String context) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(context + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String context) {
        if (!condition) {
            throw new AssertionError(context + ": expected true");
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) {
                return;
            }
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }
}

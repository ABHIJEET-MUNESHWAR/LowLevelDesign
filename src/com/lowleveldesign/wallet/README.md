# Digital Wallet – Low Level Design

An in-memory digital wallet / bank-account application. People own wallets
denominated in the system's own currency, **FkRupee (F₹)**, and transfer money
between them. No balance may ever fall below zero and the smallest transferable
amount is **F₹ 0.0001**.

The system is driven entirely by text commands, so it can be demoed from a
hard-coded string, a file or the command line. All state is held in memory —
nothing is persisted to files or databases.

## Table of Contents

1. [Commands](#commands)
2. [Offers & Bonus](#offers--bonus)
3. [Folder Structure](#folder-structure)
4. [Design](#design)
5. [Running the Demo](#running-the-demo)
6. [Running the Tests](#running-the-tests)
7. [Corner Cases Handled](#corner-cases-handled)

## Commands

| Command | Description |
| --- | --- |
| `CreateWallet <name> <balance>` | Opens a wallet with an opening balance (≥ 0). |
| `TransferMoney <from> <to> <amount>` | Debits `from` and credits `to` by the same amount. |
| `Statement <name>` | Prints every transaction recorded on the account. |
| `Overview` | Prints the current balance of all accounts (in creation order). |
| `Offer2` | Rewards the three most active customers. |
| `FixedDeposit <name> <amount>` | *(Bonus)* Parks a fixed deposit against the account. |

## Offers & Bonus

- **Offer1** – When a transfer leaves *both* the sender and receiver with exactly
  the same balance, each earns **F₹ 10**.
- **Offer2** – When fired, the three customers with the **highest number of
  transactions** earn **F₹ 10, F₹ 5 and F₹ 2**. Ties are broken by (1) higher
  account balance, then (2) earlier wallet creation.
  Only `TransferMoney` operations count as "transactions"; offer/interest
  rewards do not.
- **FixedDeposit (bonus)** – Parks `<amount>`. If the balance stays at or above
  that amount for the next **5** transactions, the holder earns **F₹ 10**
  interest and the deposit matures. If the balance ever drops below the parked
  amount the deposit is dissolved and must be reopened. The parked amount and
  the remaining transaction count are shown in both `Overview` and `Statement`.

## Folder Structure

```
wallet
├── Main.java              # Demo: runs the sample script + FD scenarios
├── package-info.java
├── model
│   ├── Money.java         # Parsing, validation and F₹ formatting (BigDecimal)
│   ├── Wallet.java        # Balance, statement, transfer count, creation order, FD
│   ├── Transaction.java   # One statement line (source, credit/debit, amount)
│   ├── TransactionType.java
│   └── FixedDeposit.java  # Parked amount + remaining transactions
├── service
│   ├── WalletService.java   # Facade: all operations, offers and FD lifecycle
│   └── CommandProcessor.java# Parses command text and prints results
├── exception
│   ├── WalletException.java              # Base (unchecked)
│   ├── WalletAlreadyExistsException.java
│   ├── WalletNotFoundException.java
│   ├── InsufficientBalanceException.java
│   ├── InvalidAmountException.java
│   └── InvalidCommandException.java
└── test
    └── TestRunner.java    # Dependency-free test suite (no JUnit)
```

## Design

- **`Money`** centralises currency concerns. Amounts are held as `BigDecimal`
  at a fixed scale of 4 to avoid binary floating-point rounding errors, and are
  formatted for display by stripping insignificant trailing zeros (`100.0000` →
  `100`, `95.7000` → `95.7`).
- **`Wallet`** owns its balance, an ordered statement, a running count of the
  transfers it took part in, its creation order (final Offer2 tie-breaker) and
  an optional `FixedDeposit`. It exposes only `credit`/`debit`, keeping the
  invariant that a balance never goes negative.
- **`WalletService`** is the facade holding all wallets in a `LinkedHashMap`
  (preserving creation order for `Overview`). It implements transfers, wires in
  Offer1 automatically after each transfer, ranks customers for Offer2 using a
  single `Comparator` chain, and advances/dissolves fixed deposits.
- **`CommandProcessor`** is the thin text layer. It parses each command,
  delegates to the service and prints output, catching `WalletException` so a
  batch of commands keeps running.

This separation (model ↔ service ↔ command/IO) keeps the core logic testable
without any I/O and makes it trivial to swap the text front-end for a REPL, a
file reader or an HTTP layer.

## Running the Demo

From the `src` directory:

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java com\lowleveldesign\wallet | % FullName)
java -cp out com.lowleveldesign.wallet.Main
```

The demo reproduces the sample from the problem statement exactly and then
exercises the fixed-deposit maturity and dissolution scenarios.

## Running the Tests

```powershell
java -cp out com.lowleveldesign.wallet.test.TestRunner
```

The runner needs no JUnit, prints a `[PASS]/[FAIL]` line per test and exits
non-zero on any failure.

## Corner Cases Handled

- Balance can never go below zero (`InsufficientBalanceException`).
- Transfer amount must be at least F₹ 0.0001; amounts finer than the smallest
  unit or otherwise malformed are rejected (`InvalidAmountException`).
- No transferring money to yourself.
- Creating a duplicate wallet, or referencing an unknown wallet, is rejected.
- Negative opening balances are rejected.
- Offer2 works with fewer than three wallets (rewards as many as exist).
- Offer1/Offer2/interest rewards are recorded on statements but never counted as
  transactions for Offer2 or fixed-deposit maturity.
- Opening a fixed deposit larger than the current balance is rejected.

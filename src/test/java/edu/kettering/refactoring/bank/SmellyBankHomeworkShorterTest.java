//NOTE: Not modified due to learning in class that tests should not be modified when simply refactoring code, as it may ultimately change the results.

package edu.kettering.refactoring.bank;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class BankAccountDepositWithdrawTests {

    // ---------- Helper to build standard accounts ----------
    private List<BankAccount> baseAccounts() {
        List<BankAccount> accounts = new ArrayList<>();
        accounts.add(new BankAccount.CheckingAccount("C-100", "A. Chen", 250, 100));
        accounts.add(new BankAccount.SavingsAccount("S-200", "B. Patel", 1200, 0.02));
        accounts.add(new BankAccount.CheckingAccount("C-300", "C. Rivera", 40, 50));
        accounts.add(new BankAccount.SavingsAccount("S-400", "D. Smith", 9000, 0.03));
        return accounts;
    }

    // ---------- Deposit tests ----------

    @Test
    void deposit_shouldIncreaseBalance() {
        var accounts = baseAccounts();
        var acct = (BankAccount) accounts.get(0); // C-100

        List<Transaction.Txn> txns = List.of(
                new Transaction.Txn("C-100", "DEPOSIT", 50, "cash")
        );

        RefactoredBankHomeworkShorter.processDailyBatch(
                accounts, txns,
                false,
                1000.0,
                5000.0,
                false,
                "USD",
                2,
                true
        );

        assertEquals(300.0, acct.balance(), 1e-9);
    }

    // ---------- Withdrawal tests ----------

    @Test
    void withdrawal_fromCheckingWithinOverdraft_shouldSucceed() {
        var accounts = baseAccounts();
        var acct = (BankAccount) accounts.get(0); // C-100

        List<Transaction.Txn> txns = List.of(
                new Transaction.Txn("C-100", "WITHDRAW", 300, "rent")
        );

        RefactoredBankHomeworkShorter.processDailyBatch(
                accounts, txns,
                false,
                1000.0,
                5000.0,
                false,
                "USD",
                2,
                true
        );

        // 250 - 300 = -50, within overdraft limit of 100
        assertEquals(-50.0, acct.balance(), 1e-9);
    }

    @Test
    void withdrawal_fromCheckingBeyondOverdraft_shouldBeDeclined() {
        var accounts = baseAccounts();
        var acct = (BankAccount) accounts.get(2); // C-300

        List<Transaction.Txn> txns = List.of(
                new Transaction.Txn("C-300", "WITHDRAW", 120, "billpay")
        );

        RefactoredBankHomeworkShorter.processDailyBatch(
                accounts, txns,
                false,
                1000.0,
                5000.0,
                false,
                "USD",
                2,
                true
        );

        // Should remain unchanged
        assertEquals(40.0, acct.balance(), 1e-9);
    }

    @Test
    void withdrawal_fromSavingsThatWouldGoNegative_shouldBeDeclined() {
        var accounts = baseAccounts();
        var acct = (BankAccount) accounts.get(1); // S-200

        List<Transaction.Txn> txns = List.of(
                new Transaction.Txn("S-200", "WITHDRAW", 1300, "transfer")
        );

        RefactoredBankHomeworkShorter.processDailyBatch(
                accounts, txns,
                false,
                1000.0,
                5000.0,
                false,
                "USD",
                2,
                true
        );

        assertEquals(1200.0, acct.balance(), 1e-9);
    }

    // ---------- Flagging & VIP tests ----------

    @Test
    void largeTransaction_shouldFlagAccount() {
        var accounts = baseAccounts();
        var acct = (BankAccount) accounts.get(3); // S-400

        List<Transaction.Txn> txns = List.of(
                new Transaction.Txn("S-400", "DEPOSIT", 1500, "bonus")
        );

        RefactoredBankHomeworkShorter.processDailyBatch(
                accounts, txns,
                false,
                1000.0,  // flag threshold
                5000.0,
                false,
                "USD",
                2,
                true
        );

        assertTrue(acct.flagged());
    }

    @Test
    void highBalance_shouldTriggerVipNoteInReport() {
        var accounts = baseAccounts();

        List<Transaction.Txn> txns = List.of(
                new Transaction.Txn("S-400", "DEPOSIT", 1500, "bonus")
        );

        String report = RefactoredBankHomeworkShorter.processDailyBatch(
                accounts, txns,
                false,
                1000.0,
                5000.0,  // VIP threshold
                false,
                "USD",
                2,
                true
        );

        assertTrue(report.contains("VIP NOTE"));
    }

    // ---------- Report sanity tests ----------

    @Test
    void report_shouldContainSummarySections() {
        var accounts = baseAccounts();

        List<Transaction.Txn> txns = List.of(
                new Transaction.Txn("C-100", "DEPOSIT", 10, "test")
        );

        String report = RefactoredBankHomeworkShorter.processDailyBatch(
                accounts, txns,
                false,
                1000.0,
                5000.0,
                false,
                "USD",
                2,
                true
        );

        assertAll(
                () -> assertTrue(report.contains("-- POST-CHECKS --")),
                () -> assertTrue(report.contains("-- SUMMARY A --")),
                () -> assertTrue(report.contains("-- TOTALS --")),
                () -> assertTrue(report.contains("-- SUMMARY B --"))
        );
    }
}

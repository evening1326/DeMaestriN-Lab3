package edu.kettering.refactoring.bank;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.kettering.refactoring.bank.BankAccount.CheckingAccount;
import edu.kettering.refactoring.bank.BankAccount.SavingsAccount;
import edu.kettering.refactoring.bank.Transaction.Txn;

public class RefactoredBankHomeworkShorter
{

    public static void main(String[] args) 
    {
        List<BankAccount> accounts = new ArrayList<>();
        accounts.add(new CheckingAccount("C-100", "A. Chen", 250, 100));
        accounts.add(new SavingsAccount("S-200", "B. Patel", 1200, 0.02));
        accounts.add(new CheckingAccount("C-300", "C. Rivera", 40, 50));
        accounts.add(new SavingsAccount("S-400", "D. Smith", 9000, 0.03));

        // Five transactions chosen to exercise all behavior paths
        List<Txn> transactions = List.of
        (
        // 1) Normal withdrawal from checking (allowed)
        new Txn("C-100", "WITHDRAW", 75, "ATM withdrawal"),

        // 2) Withdrawal from checking that exceeds overdraft (DECLINED)
        new Txn("C-300", "WITHDRAW", 120, "Billpay overdraft test"),

        // 3) Withdrawal from savings that would go negative (DECLINED)
        new Txn("S-200", "WITHDRAW", 1300, "Savings overdraft test"),

        // 4) Large deposit that triggers FLAG + VIP NOTE
        new Txn("S-400", "DEPOSIT", 1500, "Bonus deposit"),

        // 5) Small deposit to verify normal deposit path
        new Txn("C-100", "DEPOSIT", 25, "Cash deposit")
        );

        System.out.println(processDailyBatch(
                accounts, transactions,
                false,   // includeZeroAmounttransactions
                1000.0,  // flagLargeTxnThreshold
                5000.0,  // vipBalanceThreshold
                true,    // debug
                "USD",   // currency
                2,       // digits
                true     // rounding
        ));
    }

    // Long function + long-ish parameter list + mixed responsibilities (intentionally)
    //NOTE: I was ultimately unsure how to change this without breaking things, especially being able to do so in a way that did not break the StringBuilder output.
    public static String processDailyBatch(
            List<BankAccount> inputAccounts,
            List<Txn> inputTransactions,
            boolean includeZeroAmountTransactions,
            double flagLargeTxnThreshold,
            double vipBalanceThreshold,
            boolean debug,
            String currency,
            int digits,
            boolean rounding) 
        {
        StringBuilder out = new StringBuilder();
        out.append("=== BANK BATCH REPORT ===\n");

        //NOTE: I've actually never used ".stream()" before, so this was an interesting learning experience outside of just refactoring.

        // store accounts by their id as the key
        Map<String, BankAccount> byId = inputAccounts.stream().collect(Collectors.toMap(BankAccount::id, Function.identity()));

        //include / exclude zero-amount transactions based on "includeZeroAmountTransactions"
        List<Txn> transactions = inputTransactions;

        if(!debug)
        {
        transactions = inputTransactions.stream()
        .filter(transaction -> includeZeroAmountTransactions || transaction.amount() != 0.0)
        .toList();
        }

        int numAppliedTransactions = 0;
        int numSkippedTransactions = 0;      
        double appliedTransactionsAbsoluteVal = 0.0;

        out.append("\n-- APPLY --\n");
        
        
        for (Txn transaction : transactions) 
        {
            BankAccount account = byId.get(transaction.acctId());

            // if can't find bank account
            if (account == null)
            {
                numSkippedTransactions++;

                if (debug) out.append("[dbg] unknown ").append(transaction.acctId()).append("\n");
                continue;
            }

            out.append(transaction.typeOfTransaction()).append(" acct=").append(account.id())
                    .append(" owner=").append(account.owner())
                    .append(" amount=").append(format(transaction.amount(), digits, rounding)).append(" ").append(currency)
                    .append(" memo=").append(transaction.memo()).append("\n");
            
            if (transaction.typeOfTransaction().equals("DEPOSIT")) 
            {
                // update the bank account balance with a deposit
                account.bal += transaction.amount(); numAppliedTransactions++; appliedTransactionsAbsoluteVal += Math.abs(transaction.amount());
                out.append("  newBal=").append(format(account.bal, digits, rounding)).append("\n");

            } 
            
            else if (transaction.typeOfTransaction().equals("WITHDRAW")) 
            {
                boolean ok;

                // update the bank account balance with a withdrawal
                if (account instanceof CheckingAccount checkAcc) ok = (account.bal - transaction.amount()) >= -checkAcc.overdraft();
                else ok = (account.bal - transaction.amount()) >= 0;

                if (!ok) 
                {numSkippedTransactions++; out.append("  DECLINED\n");}

                else 
                {
                    account.bal -= transaction.amount(); numAppliedTransactions++; appliedTransactionsAbsoluteVal += Math.abs(transaction.amount());
                    out.append("  newBal=").append(format(account.bal, digits, rounding)).append("\n");
                }

            } 
            
            else
            {
                numSkippedTransactions++;
                out.append("  SKIP unknown kind\n");
            }

            if (Math.abs(transaction.amount()) >= flagLargeTxnThreshold) 
            {
                account.setFlagged(true);
                out.append("  ** FLAG large txn **\n");
            }

            if (account.balance() >= vipBalanceThreshold) out.append("  VIP NOTE\n");
            out.append("\n");
        }

        out.append("-- POST-CHECKS --\n");

        for (BankAccount account : inputAccounts) 
        {
            if (account instanceof CheckingAccount checkingAccount) 
            {
                if (account.balance() < -checkingAccount.overdraft()) { account.setFlagged(true); out.append("Flag ").append(account.id()).append(" beyond overdraft\n"); }
            } 
            
            else 
            {
                if (account.balance() < 0) { account.setFlagged(true); out.append("Flag ").append(account.id()).append(" negative savings\n"); }
            }
        }

        out.append("\n-- SUMMARY A --\n");
        for (BankAccount account : inputAccounts)
            out.append(account.id()).append(" ").append(account.type()).append(" ").append(account.owner())
                    .append(" bal=").append(format(account.balance(), digits, rounding))
                    .append(account.flagged() ? " [FLAG]" : "").append("\n");

        out.append("\n-- TOTALS --\n");
        out.append("applied=").append(numAppliedTransactions).append(" skipped=").append(numSkippedTransactions)
                .append(" absTotal=").append(format(appliedTransactionsAbsoluteVal, digits, rounding)).append(" ").append(currency).append("\n");

        out.append("\n-- SUMMARY B --\n");
        for (int i = inputAccounts.size() - 1; i >= 0; i--) {
            BankAccount account = inputAccounts.get(i);
            out.append("[").append(account.type()).append("] ").append(account.owner())
                    .append(" id=").append(account.id())
                    .append(" bal=").append(format(account.balance(), digits, rounding))
                    .append(account.flagged() ? " *" : "").append("\n");
        }

        return out.toString();
    }

    static String format(double value, int digits, boolean rounding) 
    {
        if (!rounding) 
        {return Double.toString(value);}

        double f = Math.pow(10, digits);
        double r = Math.round(value * f) / f;

        return String.format(Locale.US, "%." + digits + "f", r);
    }
}

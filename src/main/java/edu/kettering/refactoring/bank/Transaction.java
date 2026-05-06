package edu.kettering.refactoring.bank;

public class Transaction 
{
    static class Txn 
    {
        private final String acctId, typeOfTransaction, memo;
        private final double amount;

        Txn(String acctId, String typeOfTransaction, double amount, String memo) 
        {this.acctId = acctId; this.typeOfTransaction = typeOfTransaction; this.amount = amount; this.memo = memo;}

        public String acctId() {return acctId;}
        public String typeOfTransaction() {return typeOfTransaction;}
        public String memo() {return memo;}
        public double amount() {return amount;}
    }    
}

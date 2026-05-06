package edu.kettering.refactoring.bank;

public abstract class BankAccount 
{
    private final String id, owner, type;
    protected double bal;
    private boolean flagged;

    protected BankAccount(String id, String owner, double bal, String type) 
    {this.id = id; this.owner = owner; this.bal = bal; this.type = type;}
    
    public String id() {return id;}
    public String owner() {return owner;}
    public double balance() {return bal;}
    public boolean flagged() {return flagged;}
    public void setFlagged(boolean boolValue) {flagged = boolValue;}
    public String type() {return type;}

    public static class CheckingAccount extends BankAccount 
    {
        private final double overdraft;
        
        public CheckingAccount(String id, String owner, double bal, double overdraft) 
        {super(id, owner, bal, "CHECKING"); this.overdraft = overdraft;}

        public double overdraft() {return overdraft;}
    }

    public static class SavingsAccount extends BankAccount 
    {
        public SavingsAccount(String id, String owner, double bal, double rate) 
        {super(id, owner, bal, "SAVINGS");}
    }
}

package common;


import java.util.ArrayList;
import java.util.List;


/**
* Represents a bank account with all its properties.
*/
public class BankAccount {
   private int accountNumber;
   private String holderName;
   private String password;          // Fixed-length (max 16 chars)
   private CurrencyType currency;
   private float balance;
   private List<Transaction> transactions;  // For bank statement feature
  
   // Password max length for marshalling
   public static final int PASSWORD_MAX_LENGTH = 16;
  
   public BankAccount(int accountNumber, String holderName, String password,
                      CurrencyType currency, float balance) {
       this.accountNumber = accountNumber;
       this.holderName = holderName;
       this.password = password;
       this.currency = currency;
       this.balance = balance;
       this.transactions = new ArrayList<>();
      
       // Record initial deposit
       if (balance > 0) {
           addTransaction(TransactionType.DEPOSIT, balance, "Initial deposit");
       }
   }
  
   // ==================== GETTERS AND SETTERS ====================
  
   public int getAccountNumber() { return accountNumber; }
  
   public String getHolderName() { return holderName; }
  
   public String getPassword() { return password; }
  
   public CurrencyType getCurrency() { return currency; }
  
   public float getBalance() { return balance; }
   public void setBalance(float balance) { this.balance = balance; }
  
   public List<Transaction> getTransactions() { return transactions; }
  
   // ==================== ACCOUNT OPERATIONS ====================
  
   /**
    * Deposit money into the account.
    * @param amount the amount to deposit
    * @return true if successful
    */
   public boolean deposit(float amount) {
       if (amount <= 0) {
           return false;
       }
       this.balance += amount;
       addTransaction(TransactionType.DEPOSIT, amount, "Deposit");
       return true;
   }
  
   /**
    * Withdraw money from the account.
    * @param amount the amount to withdraw
    * @return true if successful, false if insufficient balance
    */
   public boolean withdraw(float amount) {
       if (amount <= 0 || amount > this.balance) {
           return false;
       }
       this.balance -= amount;
       addTransaction(TransactionType.WITHDRAW, amount, "Withdraw");
       return true;
   }
  
   /**
    * Transfer money to another account.
    * @param amount the amount to transfer
    * @param toAccountNumber the destination account number
    * @return true if withdrawal part is successful
    */
   public boolean transferOut(float amount, int toAccountNumber) {
       if (amount <= 0 || amount > this.balance) {
           return false;
       }
       this.balance -= amount;
       addTransaction(TransactionType.TRANSFER_OUT, amount,
                     "Transfer to account " + toAccountNumber);
       return true;
   }
  
   /**
    * Receive transfer from another account.
    * @param amount the amount received
    * @param fromAccountNumber the source account number
    */
   public void transferIn(float amount, int fromAccountNumber) {
       this.balance += amount;
       addTransaction(TransactionType.TRANSFER_IN, amount,
                     "Transfer from account " + fromAccountNumber);
   }
  
   /**
    * Add a transaction to the history.
    */
   private void addTransaction(TransactionType type, float amount, String description) {
       transactions.add(new Transaction(type, amount, balance, description,
                                        System.currentTimeMillis()));
   }
  
   /**
    * Verify the password.
    * @param inputPassword the password to verify
    * @return true if password matches
    */
   public boolean verifyPassword(String inputPassword) {
       return this.password.equals(inputPassword);
   }
  
   @Override
   public String toString() {
       return String.format("Account[#%d, %s, %s, %.2f %s]",
               accountNumber, holderName, currency, balance, currency.name());
   }
  
   // ==================== TRANSACTION TYPES ====================
  
   public enum TransactionType {
       DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT
   }
  
   // ==================== TRANSACTION RECORD ====================
  
   public static class Transaction {
       public final TransactionType type;
       public final float amount;
       public final float balanceAfter;
       public final String description;
       public final long timestamp;
      
       public Transaction(TransactionType type, float amount, float balanceAfter,
                         String description, long timestamp) {
           this.type = type;
           this.amount = amount;
           this.balanceAfter = balanceAfter;
           this.description = description;
           this.timestamp = timestamp;
       }
   }
}








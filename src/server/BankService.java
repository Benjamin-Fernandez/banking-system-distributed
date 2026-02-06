package server;


import common.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
* Core banking service that manages all bank accounts and operations.
* This class handles the business logic for all banking operations.
*/
public class BankService {
  
   // In-memory storage for bank accounts
   private final Map<Integer, BankAccount> accounts;
  
   // Counter for generating unique account numbers
   private final AtomicInteger accountNumberGenerator;
  
   // Callback for notifying monitors about updates
   private UpdateCallback updateCallback;
  
   public BankService() {
       this.accounts = new HashMap<>();
       this.accountNumberGenerator = new AtomicInteger(1000); // Start from account #1000
   }
  
   public void setUpdateCallback(UpdateCallback callback) {
       this.updateCallback = callback;
   }
  
   // ==================== OPERATION 1: OPEN ACCOUNT ====================
  
   /**
    * Open a new bank account.
    * @param holderName the name of the account holder
    * @param password the account password
    * @param currency the currency type
    * @param initialBalance the initial balance
    * @return ServiceResult containing the new account number or error
    */
   public ServiceResult openAccount(String holderName, String password,
                                    CurrencyType currency, float initialBalance) {
       // Validate inputs
       if (holderName == null || holderName.trim().isEmpty()) {
           return new ServiceResult(StatusCode.ERROR_INVALID_REQUEST,
                                   "Holder name cannot be empty");
       }
       if (password == null || password.isEmpty() || password.length() > BankAccount.PASSWORD_MAX_LENGTH) {
           return new ServiceResult(StatusCode.ERROR_INVALID_REQUEST,
                                   "Password must be 1-16 characters");
       }
       if (initialBalance < 0) {
           return new ServiceResult(StatusCode.ERROR_INVALID_AMOUNT,
                                   "Initial balance cannot be negative");
       }
      
       // Create new account
       int accountNumber = accountNumberGenerator.getAndIncrement();
       BankAccount account = new BankAccount(accountNumber, holderName, password,
                                             currency, initialBalance);
       accounts.put(accountNumber, account);
      
       System.out.println("[BankService] Opened account: " + account);
      
       // Notify monitors
       notifyUpdate("ACCOUNT_OPENED", account);
      
       return new ServiceResult(StatusCode.SUCCESS, accountNumber);
   }
  
   // ==================== OPERATION 2: CLOSE ACCOUNT ====================
  
   /**
    * Close an existing bank account.
    * @param holderName the name of the account holder
    * @param accountNumber the account number
    * @param password the account password
    * @return ServiceResult indicating success or error
    */
   public ServiceResult closeAccount(String holderName, int accountNumber, String password) {
       // Validate account exists
       BankAccount account = accounts.get(accountNumber);
       if (account == null) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NOT_FOUND,
                                   "Account not found");
       }
      
       // Validate name matches
       if (!account.getHolderName().equals(holderName)) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NAME_MISMATCH,
                                   "Account does not belong to this user");
       }
      
       // Validate password
       if (!account.verifyPassword(password)) {
           return new ServiceResult(StatusCode.ERROR_WRONG_PASSWORD,
                                   "Incorrect password");
       }
      
       // Close account
       float finalBalance = account.getBalance();
       accounts.remove(accountNumber);
      
       System.out.println("[BankService] Closed account #" + accountNumber +
                         " (final balance: " + finalBalance + ")");
      
       // Notify monitors
       notifyUpdate("ACCOUNT_CLOSED", account);
      
       return new ServiceResult(StatusCode.SUCCESS,
                               "Account #" + accountNumber + " closed. Final balance: " + finalBalance);
   }
  
   // ==================== OPERATION 3: DEPOSIT ====================
  
   /**
    * Deposit money into an account.
    * @param holderName the name of the account holder
    * @param accountNumber the account number
    * @param password the account password
    * @param currency the currency type (must match account currency)
    * @param amount the amount to deposit
    * @return ServiceResult with updated balance or error
    */
   public ServiceResult deposit(String holderName, int accountNumber, String password,
                               CurrencyType currency, float amount) {
       // Validate account exists
       BankAccount account = accounts.get(accountNumber);
       if (account == null) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NOT_FOUND,
                                   "Account not found");
       }
      
       // Validate name matches
       if (!account.getHolderName().equals(holderName)) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NAME_MISMATCH,
                                   "Account does not belong to this user");
       }
      
       // Validate password
       if (!account.verifyPassword(password)) {
           return new ServiceResult(StatusCode.ERROR_WRONG_PASSWORD,
                                   "Incorrect password");
       }
      
       // Validate currency matches
       if (account.getCurrency() != currency) {
           return new ServiceResult(StatusCode.ERROR_INVALID_CURRENCY,
                                   "Currency mismatch. Account uses " + account.getCurrency());
       }
      
       // Validate amount
       if (amount <= 0) {
           return new ServiceResult(StatusCode.ERROR_INVALID_AMOUNT,
                                   "Amount must be positive");
       }
      
       // Perform deposit
       account.deposit(amount);


       System.out.println("[BankService] Deposited " + amount + " " + currency +
                         " to account #" + accountNumber + ". New balance: " + account.getBalance());


       // Notify monitors
       notifyUpdate("DEPOSIT", account);


       return new ServiceResult(StatusCode.SUCCESS, account.getBalance());
   }


   // ==================== OPERATION 4: WITHDRAW ====================


   /**
    * Withdraw money from an account.
    */
   public ServiceResult withdraw(String holderName, int accountNumber, String password,
                                CurrencyType currency, float amount) {
       BankAccount account = accounts.get(accountNumber);
       if (account == null) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NOT_FOUND, "Account not found");
       }
       if (!account.getHolderName().equals(holderName)) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NAME_MISMATCH,
                                   "Account does not belong to this user");
       }
       if (!account.verifyPassword(password)) {
           return new ServiceResult(StatusCode.ERROR_WRONG_PASSWORD, "Incorrect password");
       }
       if (account.getCurrency() != currency) {
           return new ServiceResult(StatusCode.ERROR_INVALID_CURRENCY,
                                   "Currency mismatch. Account uses " + account.getCurrency());
       }
       if (amount <= 0) {
           return new ServiceResult(StatusCode.ERROR_INVALID_AMOUNT, "Amount must be positive");
       }
       if (amount > account.getBalance()) {
           return new ServiceResult(StatusCode.ERROR_INSUFFICIENT_BALANCE,
                                   "Insufficient balance. Available: " + account.getBalance());
       }


       account.withdraw(amount);
       System.out.println("[BankService] Withdrew " + amount + " " + currency +
                         " from account #" + accountNumber + ". New balance: " + account.getBalance());
       notifyUpdate("WITHDRAW", account);


       return new ServiceResult(StatusCode.SUCCESS, account.getBalance());
   }


   // ==================== OPERATION 5: BANK STATEMENT (IDEMPOTENT) ====================


   /**
    * Generate a bank statement for an account.
    * This is idempotent as it only reads data, no state changes.
    */
   public ServiceResult getBankStatement(String holderName, int accountNumber, String password) {
       BankAccount account = accounts.get(accountNumber);
       if (account == null) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NOT_FOUND, "Account not found");
       }
       if (!account.getHolderName().equals(holderName)) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NAME_MISMATCH,
                                   "Account does not belong to this user");
       }
       if (!account.verifyPassword(password)) {
           return new ServiceResult(StatusCode.ERROR_WRONG_PASSWORD, "Incorrect password");
       }


       // Build statement
       StringBuilder statement = new StringBuilder();
       statement.append("=== Bank Statement for Account #").append(accountNumber).append(" ===\n");
       statement.append("Holder: ").append(account.getHolderName()).append("\n");
       statement.append("Currency: ").append(account.getCurrency().getDisplayName()).append("\n");
       statement.append("Current Balance: ").append(String.format("%.2f", account.getBalance())).append("\n");
       statement.append("\n--- Transaction History ---\n");


       for (BankAccount.Transaction tx : account.getTransactions()) {
           statement.append(String.format("[%s] %s: %.2f -> Balance: %.2f\n",
                   new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(tx.timestamp)),
                   tx.description, tx.amount, tx.balanceAfter));
       }


       System.out.println("[BankService] Generated statement for account #" + accountNumber);


       return new ServiceResult(StatusCode.SUCCESS, statement.toString());
   }


   // ==================== OPERATION 6: TRANSFER (NON-IDEMPOTENT) ====================


   /**
    * Transfer money between two accounts.
    * This is non-idempotent as it modifies state of both accounts.
    */
   public ServiceResult transfer(String holderName, int fromAccountNumber, String password,
                                 int toAccountNumber, float amount) {
       if (fromAccountNumber == toAccountNumber) {
           return new ServiceResult(StatusCode.ERROR_SAME_ACCOUNT_TRANSFER,
                                   "Cannot transfer to the same account");
       }


       BankAccount fromAccount = accounts.get(fromAccountNumber);
       BankAccount toAccount = accounts.get(toAccountNumber);


       if (fromAccount == null) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NOT_FOUND,
                                   "Source account not found");
       }
       if (toAccount == null) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NOT_FOUND,
                                   "Destination account not found");
       }
       if (!fromAccount.getHolderName().equals(holderName)) {
           return new ServiceResult(StatusCode.ERROR_ACCOUNT_NAME_MISMATCH,
                                   "Source account does not belong to this user");
       }
       if (!fromAccount.verifyPassword(password)) {
           return new ServiceResult(StatusCode.ERROR_WRONG_PASSWORD, "Incorrect password");
       }
       if (amount <= 0) {
           return new ServiceResult(StatusCode.ERROR_INVALID_AMOUNT, "Amount must be positive");
       }
       if (amount > fromAccount.getBalance()) {
           return new ServiceResult(StatusCode.ERROR_INSUFFICIENT_BALANCE,
                                   "Insufficient balance. Available: " + fromAccount.getBalance());
       }


       // Perform transfer (currency conversion not implemented for simplicity)
       fromAccount.transferOut(amount, toAccountNumber);
       toAccount.transferIn(amount, fromAccountNumber);


       System.out.println("[BankService] Transferred " + amount + " from #" + fromAccountNumber +
                         " to #" + toAccountNumber);
       notifyUpdate("TRANSFER_OUT", fromAccount);
       notifyUpdate("TRANSFER_IN", toAccount);


       return new ServiceResult(StatusCode.SUCCESS, fromAccount.getBalance());
   }


   // ==================== HELPER METHODS ====================


   private void notifyUpdate(String updateType, BankAccount account) {
       if (updateCallback != null) {
           updateCallback.onUpdate(updateType, account);
       }
   }


   public BankAccount getAccount(int accountNumber) {
       return accounts.get(accountNumber);
   }


   public int getAccountCount() {
       return accounts.size();
   }
}








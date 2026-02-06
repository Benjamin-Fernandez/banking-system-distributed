package server;


import common.BankAccount;


/**
* Callback interface for monitoring updates to bank accounts.
* Used by the server to notify registered clients about account changes.
*/
public interface UpdateCallback {
   /**
    * Called when an account update occurs.
    * @param updateType the type of update (e.g., "DEPOSIT", "WITHDRAW", "ACCOUNT_OPENED")
    * @param account the affected bank account
    */
   void onUpdate(String updateType, BankAccount account);
}








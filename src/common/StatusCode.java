package common;


/**
* Status codes for server responses.
*/
public class StatusCode {
   public static final int SUCCESS = 0;
   public static final int ERROR_ACCOUNT_NOT_FOUND = 1;
   public static final int ERROR_WRONG_PASSWORD = 2;
   public static final int ERROR_INSUFFICIENT_BALANCE = 3;
   public static final int ERROR_ACCOUNT_NAME_MISMATCH = 4;
   public static final int ERROR_INVALID_AMOUNT = 5;
   public static final int ERROR_INVALID_CURRENCY = 6;
   public static final int ERROR_DUPLICATE_ACCOUNT = 7;
   public static final int ERROR_INTERNAL_SERVER = 8;
   public static final int ERROR_INVALID_REQUEST = 9;
   public static final int ERROR_SAME_ACCOUNT_TRANSFER = 10;


   /**
    * Get a human-readable message for a status code.
    * @param code the status code
    * @return the corresponding message
    */
   public static String getMessage(int code) {
       switch (code) {
           case SUCCESS:
               return "Success";
           case ERROR_ACCOUNT_NOT_FOUND:
               return "Account not found";
           case ERROR_WRONG_PASSWORD:
               return "Incorrect password";
           case ERROR_INSUFFICIENT_BALANCE:
               return "Insufficient balance";
           case ERROR_ACCOUNT_NAME_MISMATCH:
               return "Account does not belong to this user";
           case ERROR_INVALID_AMOUNT:
               return "Invalid amount specified";
           case ERROR_INVALID_CURRENCY:
               return "Invalid currency type";
           case ERROR_DUPLICATE_ACCOUNT:
               return "Account already exists";
           case ERROR_INTERNAL_SERVER:
               return "Internal server error";
           case ERROR_INVALID_REQUEST:
               return "Invalid request format";
           case ERROR_SAME_ACCOUNT_TRANSFER:
               return "Cannot transfer to the same account";
           default:
               return "Unknown error (code: " + code + ")";
       }
   }
}








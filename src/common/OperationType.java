package common;


/**
* Enumeration representing the types of operations supported by the banking system.
*/
public enum OperationType {
   OPEN_ACCOUNT(0, "Open Account", false),
   CLOSE_ACCOUNT(1, "Close Account", true),
   DEPOSIT(2, "Deposit", false),
   WITHDRAW(3, "Withdraw", false),
   MONITOR(4, "Monitor Updates", true),
   BANK_STATEMENT(5, "Bank Statement", true),    // Additional idempotent operation
   TRANSFER(6, "Transfer Money", false);          // Additional non-idempotent operation


   private final int code;
   private final String displayName;
   private final boolean idempotent;


   OperationType(int code, String displayName, boolean idempotent) {
       this.code = code;
       this.displayName = displayName;
       this.idempotent = idempotent;
   }


   public int getCode() {
       return code;
   }


   public String getDisplayName() {
       return displayName;
   }


   public boolean isIdempotent() {
       return idempotent;
   }


   /**
    * Get OperationType from its code.
    * @param code the operation code
    * @return the corresponding OperationType
    * @throws IllegalArgumentException if code is invalid
    */
   public static OperationType fromCode(int code) {
       for (OperationType type : values()) {
           if (type.code == code) {
               return type;
           }
       }
       throw new IllegalArgumentException("Invalid operation code: " + code);
   }
}








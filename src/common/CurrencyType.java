package common;


/**
* Enumeration representing supported currency types.
* Each currency has an ordinal value used for marshalling.
*/
public enum CurrencyType {
   USD(0, "US Dollar"),
   EUR(1, "Euro"),
   GBP(2, "British Pound"),
   SGD(3, "Singapore Dollar"),
   JPY(4, "Japanese Yen");


   private final int code;
   private final String displayName;


   CurrencyType(int code, String displayName) {
       this.code = code;
       this.displayName = displayName;
   }


   public int getCode() {
       return code;
   }


   public String getDisplayName() {
       return displayName;
   }


   /**
    * Get CurrencyType from its ordinal code.
    * @param code the currency code (0-4)
    * @return the corresponding CurrencyType
    * @throws IllegalArgumentException if code is invalid
    */
   public static CurrencyType fromCode(int code) {
       for (CurrencyType type : values()) {
           if (type.code == code) {
               return type;
           }
       }
       throw new IllegalArgumentException("Invalid currency code: " + code);
   }
}








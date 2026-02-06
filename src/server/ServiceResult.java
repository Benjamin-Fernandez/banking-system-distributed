package server;


/**
* Represents the result of a banking service operation.
* Contains a status code and either a success result or error message.
*/
public class ServiceResult {
   private final int statusCode;
   private final Object result;  // Can be String, Integer, Float depending on operation
  
   public ServiceResult(int statusCode, Object result) {
       this.statusCode = statusCode;
       this.result = result;
   }
  
   public int getStatusCode() {
       return statusCode;
   }
  
   public Object getResult() {
       return result;
   }
  
   public boolean isSuccess() {
       return statusCode == 0;  // StatusCode.SUCCESS
   }
  
   public String getResultAsString() {
       return result != null ? result.toString() : "";
   }
  
   public int getResultAsInt() {
       if (result instanceof Integer) {
           return (Integer) result;
       }
       return 0;
   }
  
   public float getResultAsFloat() {
       if (result instanceof Float) {
           return (Float) result;
       }
       if (result instanceof Double) {
           return ((Double) result).floatValue();
       }
       return 0.0f;
   }
}








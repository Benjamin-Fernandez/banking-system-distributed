package server;


import common.*;


/**
* Handles incoming requests by parsing payloads and invoking appropriate BankService methods.
*/
public class RequestHandler {
  
   private final BankService bankService;
  
   public RequestHandler(BankService bankService) {
       this.bankService = bankService;
   }
  
   /**
    * Process a request message and return a reply message.
    * @param request the incoming request
    * @return the reply message
    */
   public Message handleRequest(Message request) {
       OperationType operation;
       try {
           operation = OperationType.fromCode(request.getOperationCode());
       } catch (IllegalArgumentException e) {
           return Message.createErrorReply(request.getRequestId(), StatusCode.ERROR_INVALID_REQUEST);
       }
      
       System.out.println("[RequestHandler] Processing " + operation.getDisplayName() +
                         " (reqId=" + request.getRequestId() + ", clientId=" + request.getClientId() + ")");
      
       switch (operation) {
           case OPEN_ACCOUNT:
               return handleOpenAccount(request);
           case CLOSE_ACCOUNT:
               return handleCloseAccount(request);
           case DEPOSIT:
               return handleDeposit(request);
           case WITHDRAW:
               return handleWithdraw(request);
           case MONITOR:
               // Monitor is handled separately in the server
               return Message.createSuccessReply(request.getRequestId(), new byte[0]);
           case BANK_STATEMENT:
               return handleBankStatement(request);
           case TRANSFER:
               return handleTransfer(request);
           default:
               return Message.createErrorReply(request.getRequestId(), StatusCode.ERROR_INVALID_REQUEST);
       }
   }
  
   // ==================== OPEN ACCOUNT ====================
   // Payload: name (string) + password (string) + initialBalance (4 bytes float)


   private Message handleOpenAccount(Message request) {
       byte[] payload = request.getPayload();
       int offset = 0;


       try {
           Marshaller.StringResult nameResult = Marshaller.unmarshalString(payload, offset);
           String name = nameResult.value;
           offset += nameResult.bytesConsumed;


           Marshaller.StringResult passResult = Marshaller.unmarshalString(payload, offset);
           String password = passResult.value;
           offset += passResult.bytesConsumed;


           float initialBalance = Marshaller.unmarshalFloat(payload, offset);


           // Default currency to USD
           ServiceResult result = bankService.openAccount(name, password, CurrencyType.USD, initialBalance);
          
           if (result.isSuccess()) {
               byte[] replyPayload = new byte[4];
               Marshaller.marshalInt(result.getResultAsInt(), replyPayload, 0);
               return Message.createSuccessReply(request.getRequestId(), replyPayload);
           } else {
               return createErrorReplyWithMessage(request.getRequestId(),
                       result.getStatusCode(), result.getResultAsString());
           }
       } catch (Exception e) {
           System.err.println("[RequestHandler] Error handling OPEN_ACCOUNT: " + e.getMessage());
           return Message.createErrorReply(request.getRequestId(), StatusCode.ERROR_INVALID_REQUEST);
       }
   }
  
   // ==================== CLOSE ACCOUNT ====================
   // Payload: name (string) + accountNumber (4 bytes) + password (string)
  
   private Message handleCloseAccount(Message request) {
       byte[] payload = request.getPayload();
       int offset = 0;
      
       try {
           Marshaller.StringResult nameResult = Marshaller.unmarshalString(payload, offset);
           String name = nameResult.value;
           offset += nameResult.bytesConsumed;
          
           int accountNumber = Marshaller.unmarshalInt(payload, offset);
           offset += 4;
          
           Marshaller.StringResult passResult = Marshaller.unmarshalString(payload, offset);
           String password = passResult.value;
          
           ServiceResult result = bankService.closeAccount(name, accountNumber, password);
          
           if (result.isSuccess()) {
               return createSuccessReplyWithMessage(request.getRequestId(), result.getResultAsString());
           } else {
               return createErrorReplyWithMessage(request.getRequestId(),
                       result.getStatusCode(), result.getResultAsString());
           }
       } catch (Exception e) {
           System.err.println("[RequestHandler] Error handling CLOSE_ACCOUNT: " + e.getMessage());
           return Message.createErrorReply(request.getRequestId(), StatusCode.ERROR_INVALID_REQUEST);
       }
   }
  
   // ==================== DEPOSIT ====================
   // Payload: name + accountNumber + password + amount


   private Message handleDeposit(Message request) {
       byte[] payload = request.getPayload();
       int offset = 0;


       try {
           Marshaller.StringResult nameResult = Marshaller.unmarshalString(payload, offset);
           String name = nameResult.value;
           offset += nameResult.bytesConsumed;


           int accountNumber = Marshaller.unmarshalInt(payload, offset);
           offset += 4;


           Marshaller.StringResult passResult = Marshaller.unmarshalString(payload, offset);
           String password = passResult.value;
           offset += passResult.bytesConsumed;


           float amount = Marshaller.unmarshalFloat(payload, offset);


           ServiceResult result = bankService.deposit(name, accountNumber, password, amount);


           if (result.isSuccess()) {
               byte[] replyPayload = new byte[4];
               Marshaller.marshalFloat(result.getResultAsFloat(), replyPayload, 0);
               return Message.createSuccessReply(request.getRequestId(), replyPayload);
           } else {
               return createErrorReplyWithMessage(request.getRequestId(),
                       result.getStatusCode(), result.getResultAsString());
           }
       } catch (Exception e) {
           System.err.println("[RequestHandler] Error handling DEPOSIT: " + e.getMessage());
           return Message.createErrorReply(request.getRequestId(), StatusCode.ERROR_INVALID_REQUEST);
       }
   }


   // ==================== WITHDRAW ====================
   // Payload: name + accountNumber + password + amount


   private Message handleWithdraw(Message request) {
       byte[] payload = request.getPayload();
       int offset = 0;


       try {
           Marshaller.StringResult nameResult = Marshaller.unmarshalString(payload, offset);
           String name = nameResult.value;
           offset += nameResult.bytesConsumed;


           int accountNumber = Marshaller.unmarshalInt(payload, offset);
           offset += 4;


           Marshaller.StringResult passResult = Marshaller.unmarshalString(payload, offset);
           String password = passResult.value;
           offset += passResult.bytesConsumed;


           float amount = Marshaller.unmarshalFloat(payload, offset);


           ServiceResult result = bankService.withdraw(name, accountNumber, password, amount);


           if (result.isSuccess()) {
               byte[] replyPayload = new byte[4];
               Marshaller.marshalFloat(result.getResultAsFloat(), replyPayload, 0);
               return Message.createSuccessReply(request.getRequestId(), replyPayload);
           } else {
               return createErrorReplyWithMessage(request.getRequestId(),
                       result.getStatusCode(), result.getResultAsString());
           }
       } catch (Exception e) {
           System.err.println("[RequestHandler] Error handling WITHDRAW: " + e.getMessage());
           return Message.createErrorReply(request.getRequestId(), StatusCode.ERROR_INVALID_REQUEST);
       }
   }


   // ==================== BANK STATEMENT ====================


   private Message handleBankStatement(Message request) {
       byte[] payload = request.getPayload();
       int offset = 0;


       try {
           Marshaller.StringResult nameResult = Marshaller.unmarshalString(payload, offset);
           String name = nameResult.value;
           offset += nameResult.bytesConsumed;


           int accountNumber = Marshaller.unmarshalInt(payload, offset);
           offset += 4;


           Marshaller.StringResult passResult = Marshaller.unmarshalString(payload, offset);
           String password = passResult.value;


           ServiceResult result = bankService.getBankStatement(name, accountNumber, password);


           if (result.isSuccess()) {
               return createSuccessReplyWithMessage(request.getRequestId(), result.getResultAsString());
           } else {
               return createErrorReplyWithMessage(request.getRequestId(),
                       result.getStatusCode(), result.getResultAsString());
           }
       } catch (Exception e) {
           System.err.println("[RequestHandler] Error handling BANK_STATEMENT: " + e.getMessage());
           return Message.createErrorReply(request.getRequestId(), StatusCode.ERROR_INVALID_REQUEST);
       }
   }


   // ==================== TRANSFER ====================


   private Message handleTransfer(Message request) {
       byte[] payload = request.getPayload();
       int offset = 0;


       try {
           Marshaller.StringResult nameResult = Marshaller.unmarshalString(payload, offset);
           String name = nameResult.value;
           offset += nameResult.bytesConsumed;


           int fromAccountNumber = Marshaller.unmarshalInt(payload, offset);
           offset += 4;


           Marshaller.StringResult passResult = Marshaller.unmarshalString(payload, offset);
           String password = passResult.value;
           offset += passResult.bytesConsumed;


           int toAccountNumber = Marshaller.unmarshalInt(payload, offset);
           offset += 4;


           float amount = Marshaller.unmarshalFloat(payload, offset);


           ServiceResult result = bankService.transfer(name, fromAccountNumber, password,
                                                       toAccountNumber, amount);


           if (result.isSuccess()) {
               byte[] replyPayload = new byte[4];
               Marshaller.marshalFloat(result.getResultAsFloat(), replyPayload, 0);
               return Message.createSuccessReply(request.getRequestId(), replyPayload);
           } else {
               return createErrorReplyWithMessage(request.getRequestId(),
                       result.getStatusCode(), result.getResultAsString());
           }
       } catch (Exception e) {
           System.err.println("[RequestHandler] Error handling TRANSFER: " + e.getMessage());
           return Message.createErrorReply(request.getRequestId(), StatusCode.ERROR_INVALID_REQUEST);
       }
   }


   // ==================== HELPER METHODS ====================


   private Message createSuccessReplyWithMessage(int requestId, String message) {
       byte[] payload = new byte[Marshaller.getStringMarshalledSize(message)];
       Marshaller.marshalString(message, payload, 0);
       return Message.createSuccessReply(requestId, payload);
   }


   private Message createErrorReplyWithMessage(int requestId, int statusCode, String message) {
       byte[] payload = new byte[Marshaller.getStringMarshalledSize(message)];
       Marshaller.marshalString(message, payload, 0);
       Message reply = new Message();
       reply.setRequestId(requestId);
       reply.setStatusCode((byte) statusCode);
       reply.setPayload(payload);
       return reply;
   }
}








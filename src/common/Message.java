package common;


/**
* Represents a message transmitted between client and server.
*
* Request Message Format (total header: 12 bytes):
* - Request ID (4 bytes): Unique identifier for duplicate detection
* - Operation Code (1 byte): Type of operation (0-6)
* - Client ID (4 bytes): Unique client identifier
* - Payload Length (2 bytes): Length of payload in bytes
* - Semantics Type (1 byte): 0=at-least-once, 1=at-most-once
*
* Reply Message Format (total header: 8 bytes):
* - Request ID (4 bytes): Echo of original request ID
* - Status Code (1 byte): 0=success, 1-255=error codes
* - Payload Length (2 bytes): Length of payload in bytes
* - Reserved (1 byte): For future use
*/
public class Message {
  
   public static final int REQUEST_HEADER_SIZE = 12;
   public static final int REPLY_HEADER_SIZE = 8;
   public static final int MAX_MESSAGE_SIZE = 4096;
  
   // Semantics types
   public static final byte SEMANTICS_AT_LEAST_ONCE = 0;
   public static final byte SEMANTICS_AT_MOST_ONCE = 1;
  
   // Message type
   public static final byte TYPE_REQUEST = 0;
   public static final byte TYPE_REPLY = 1;
   public static final byte TYPE_CALLBACK = 2;  // For monitoring updates
  
   private int requestId;
   private byte operationCode;
   private int clientId;
   private short payloadLength;
   private byte semanticsType;
   private byte statusCode;
   private byte[] payload;
   private byte messageType;
  
   // Default constructor
   public Message() {
       this.payload = new byte[0];
   }
  
   // ==================== GETTERS AND SETTERS ====================
  
   public int getRequestId() { return requestId; }
   public void setRequestId(int requestId) { this.requestId = requestId; }
  
   public byte getOperationCode() { return operationCode; }
   public void setOperationCode(byte operationCode) { this.operationCode = operationCode; }
  
   public int getClientId() { return clientId; }
   public void setClientId(int clientId) { this.clientId = clientId; }
  
   public short getPayloadLength() { return payloadLength; }
   public void setPayloadLength(short payloadLength) { this.payloadLength = payloadLength; }
  
   public byte getSemanticsType() { return semanticsType; }
   public void setSemanticsType(byte semanticsType) { this.semanticsType = semanticsType; }
  
   public byte getStatusCode() { return statusCode; }
   public void setStatusCode(byte statusCode) { this.statusCode = statusCode; }
  
   public byte[] getPayload() { return payload; }
   public void setPayload(byte[] payload) {
       this.payload = payload;
       this.payloadLength = (short) payload.length;
   }
  
   public byte getMessageType() { return messageType; }
   public void setMessageType(byte messageType) { this.messageType = messageType; }
  
   // ==================== MARSHALLING ====================
  
   /**
    * Marshal a request message to a byte array.
    * @return the marshalled byte array
    */
   public byte[] marshalRequest() {
       byte[] buffer = new byte[REQUEST_HEADER_SIZE + payload.length];
       int offset = 0;
      
       offset = Marshaller.marshalInt(requestId, buffer, offset);
       offset = Marshaller.marshalByte(operationCode, buffer, offset);
       offset = Marshaller.marshalInt(clientId, buffer, offset);
       offset = Marshaller.marshalShort((short) payload.length, buffer, offset);
       offset = Marshaller.marshalByte(semanticsType, buffer, offset);
      
       if (payload.length > 0) {
           Marshaller.copyBytes(payload, 0, buffer, offset, payload.length);
       }
      
       return buffer;
   }
  
   /**
    * Unmarshal a request message from a byte array.
    * @param buffer the byte array containing the marshalled request
    * @param length the length of valid data in the buffer
    * @return the unmarshalled Message object
    */
   public static Message unmarshalRequest(byte[] buffer, int length) {
       Message msg = new Message();
       msg.messageType = TYPE_REQUEST;
       int offset = 0;
      
       msg.requestId = Marshaller.unmarshalInt(buffer, offset);
       offset += 4;
      
       msg.operationCode = Marshaller.unmarshalByte(buffer, offset);
       offset += 1;
      
       msg.clientId = Marshaller.unmarshalInt(buffer, offset);
       offset += 4;
      
       msg.payloadLength = Marshaller.unmarshalShort(buffer, offset);
       offset += 2;
      
       msg.semanticsType = Marshaller.unmarshalByte(buffer, offset);
       offset += 1;
      
       if (msg.payloadLength > 0 && length > REQUEST_HEADER_SIZE) {
           msg.payload = Marshaller.extractBytes(buffer, offset, msg.payloadLength);
       } else {
           msg.payload = new byte[0];
       }
      
       return msg;
   }
  
   /**
    * Marshal a reply message to a byte array.
    * @return the marshalled byte array
    */
   public byte[] marshalReply() {
       byte[] buffer = new byte[REPLY_HEADER_SIZE + payload.length];
       int offset = 0;


       offset = Marshaller.marshalInt(requestId, buffer, offset);
       offset = Marshaller.marshalByte(statusCode, buffer, offset);
       offset = Marshaller.marshalShort((short) payload.length, buffer, offset);
       offset = Marshaller.marshalByte((byte) 0, buffer, offset);  // Reserved


       if (payload.length > 0) {
           Marshaller.copyBytes(payload, 0, buffer, offset, payload.length);
       }


       return buffer;
   }


   /**
    * Unmarshal a reply message from a byte array.
    * @param buffer the byte array containing the marshalled reply
    * @param length the length of valid data in the buffer
    * @return the unmarshalled Message object
    */
   public static Message unmarshalReply(byte[] buffer, int length) {
       Message msg = new Message();
       msg.messageType = TYPE_REPLY;
       int offset = 0;


       msg.requestId = Marshaller.unmarshalInt(buffer, offset);
       offset += 4;


       msg.statusCode = Marshaller.unmarshalByte(buffer, offset);
       offset += 1;


       msg.payloadLength = Marshaller.unmarshalShort(buffer, offset);
       offset += 2;


       // Skip reserved byte
       offset += 1;


       if (msg.payloadLength > 0 && length > REPLY_HEADER_SIZE) {
           msg.payload = Marshaller.extractBytes(buffer, offset, msg.payloadLength);
       } else {
           msg.payload = new byte[0];
       }


       return msg;
   }


   /**
    * Create a success reply for a given request.
    * @param requestId the original request ID
    * @param payload the response payload
    * @return a new reply Message
    */
   public static Message createSuccessReply(int requestId, byte[] payload) {
       Message reply = new Message();
       reply.messageType = TYPE_REPLY;
       reply.requestId = requestId;
       reply.statusCode = (byte) StatusCode.SUCCESS;
       reply.payload = payload != null ? payload : new byte[0];
       reply.payloadLength = (short) reply.payload.length;
       return reply;
   }


   /**
    * Create an error reply for a given request.
    * @param requestId the original request ID
    * @param errorCode the error status code
    * @return a new reply Message
    */
   public static Message createErrorReply(int requestId, int errorCode) {
       Message reply = new Message();
       reply.messageType = TYPE_REPLY;
       reply.requestId = requestId;
       reply.statusCode = (byte) errorCode;
       reply.payload = new byte[0];
       reply.payloadLength = 0;
       return reply;
   }


   @Override
   public String toString() {
       return String.format("Message[reqId=%d, op=%d, clientId=%d, status=%d, payloadLen=%d]",
               requestId, operationCode, clientId, statusCode, payloadLength);
   }
}








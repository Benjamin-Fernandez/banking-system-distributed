package common;


/**
* Custom marshalling and unmarshalling utilities for converting data types to/from byte arrays.
* Uses big-endian byte order for all multi-byte values.
*
* This class provides static methods for:
* - Integer (32-bit): 4 bytes, big-endian
* - Short (16-bit): 2 bytes, big-endian
* - Float: 4 bytes, IEEE 754 format via Float.floatToIntBits()
* - String: 2-byte length prefix + UTF-8 encoded bytes
* - Byte: 1 byte
* - Boolean: 1 byte (0 for false, 1 for true)
*/
public class Marshaller {


   // ==================== INTEGER (32-bit) ====================


   /**
    * Marshal a 32-bit integer to a byte array at the specified offset.
    * @param value the integer value to marshal
    * @param buffer the destination byte array
    * @param offset the starting position in the buffer
    * @return the new offset after writing (offset + 4)
    */
   public static int marshalInt(int value, byte[] buffer, int offset) {
       buffer[offset]     = (byte) ((value >> 24) & 0xFF);
       buffer[offset + 1] = (byte) ((value >> 16) & 0xFF);
       buffer[offset + 2] = (byte) ((value >> 8) & 0xFF);
       buffer[offset + 3] = (byte) (value & 0xFF);
       return offset + 4;
   }


   /**
    * Unmarshal a 32-bit integer from a byte array at the specified offset.
    * @param buffer the source byte array
    * @param offset the starting position in the buffer
    * @return the unmarshalled integer value
    */
   public static int unmarshalInt(byte[] buffer, int offset) {
       return ((buffer[offset] & 0xFF) << 24) |
              ((buffer[offset + 1] & 0xFF) << 16) |
              ((buffer[offset + 2] & 0xFF) << 8) |
              (buffer[offset + 3] & 0xFF);
   }


   // ==================== SHORT (16-bit) ====================


   /**
    * Marshal a 16-bit short to a byte array at the specified offset.
    * @param value the short value to marshal
    * @param buffer the destination byte array
    * @param offset the starting position in the buffer
    * @return the new offset after writing (offset + 2)
    */
   public static int marshalShort(short value, byte[] buffer, int offset) {
       buffer[offset]     = (byte) ((value >> 8) & 0xFF);
       buffer[offset + 1] = (byte) (value & 0xFF);
       return offset + 2;
   }


   /**
    * Unmarshal a 16-bit short from a byte array at the specified offset.
    * @param buffer the source byte array
    * @param offset the starting position in the buffer
    * @return the unmarshalled short value
    */
   public static short unmarshalShort(byte[] buffer, int offset) {
       return (short) (((buffer[offset] & 0xFF) << 8) |
                       (buffer[offset + 1] & 0xFF));
   }


   // ==================== FLOAT ====================


   /**
    * Marshal a float to a byte array at the specified offset.
    * Uses IEEE 754 representation via Float.floatToIntBits().
    * @param value the float value to marshal
    * @param buffer the destination byte array
    * @param offset the starting position in the buffer
    * @return the new offset after writing (offset + 4)
    */
   public static int marshalFloat(float value, byte[] buffer, int offset) {
       int intBits = Float.floatToIntBits(value);
       return marshalInt(intBits, buffer, offset);
   }


   /**
    * Unmarshal a float from a byte array at the specified offset.
    * @param buffer the source byte array
    * @param offset the starting position in the buffer
    * @return the unmarshalled float value
    */
   public static float unmarshalFloat(byte[] buffer, int offset) {
       int intBits = unmarshalInt(buffer, offset);
       return Float.intBitsToFloat(intBits);
   }


   // ==================== BYTE ====================


   /**
    * Marshal a single byte to a byte array at the specified offset.
    * @param value the byte value to marshal
    * @param buffer the destination byte array
    * @param offset the starting position in the buffer
    * @return the new offset after writing (offset + 1)
    */
   public static int marshalByte(byte value, byte[] buffer, int offset) {
       buffer[offset] = value;
       return offset + 1;
   }


   /**
    * Unmarshal a single byte from a byte array at the specified offset.
    * @param buffer the source byte array
    * @param offset the starting position in the buffer
    * @return the unmarshalled byte value
    */
   public static byte unmarshalByte(byte[] buffer, int offset) {
       return buffer[offset];
   }


   // ==================== BOOLEAN ====================


   /**
    * Marshal a boolean to a byte array at the specified offset.
    * @param value the boolean value to marshal (true=1, false=0)
    * @param buffer the destination byte array
    * @param offset the starting position in the buffer
    * @return the new offset after writing (offset + 1)
    */
   public static int marshalBoolean(boolean value, byte[] buffer, int offset) {
       buffer[offset] = (byte) (value ? 1 : 0);
       return offset + 1;
   }


   /**
    * Unmarshal a boolean from a byte array at the specified offset.
    * @param buffer the source byte array
    * @param offset the starting position in the buffer
    * @return the unmarshalled boolean value
    */
   public static boolean unmarshalBoolean(byte[] buffer, int offset) {
       return buffer[offset] != 0;
   }


   // ==================== STRING ====================


   /**
    * Marshal a string to a byte array at the specified offset.
    * Format: 2-byte length prefix (short) + UTF-8 encoded bytes.
    * @param value the string to marshal (can be null, marshalled as length 0)
    * @param buffer the destination byte array
    * @param offset the starting position in the buffer
    * @return the new offset after writing
    */
   public static int marshalString(String value, byte[] buffer, int offset) {
       if (value == null || value.isEmpty()) {
           return marshalShort((short) 0, buffer, offset);
       }
       byte[] stringBytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
       short length = (short) stringBytes.length;
       offset = marshalShort(length, buffer, offset);
       System.arraycopy(stringBytes, 0, buffer, offset, stringBytes.length);
       return offset + stringBytes.length;
   }


   /**
    * Unmarshal a string from a byte array at the specified offset.
    * @param buffer the source byte array
    * @param offset the starting position in the buffer
    * @return a StringResult containing the string and bytes consumed
    */
   public static StringResult unmarshalString(byte[] buffer, int offset) {
       short length = unmarshalShort(buffer, offset);
       if (length == 0) {
           return new StringResult("", 2);
       }
       String value = new String(buffer, offset + 2, length, java.nio.charset.StandardCharsets.UTF_8);
       return new StringResult(value, 2 + length);
   }


   /**
    * Calculate the marshalled size of a string (2-byte length + UTF-8 bytes).
    * @param value the string to measure
    * @return the number of bytes needed to marshal this string
    */
   public static int getStringMarshalledSize(String value) {
       if (value == null || value.isEmpty()) {
           return 2;
       }
       return 2 + value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
   }


   /**
    * Helper class to return both the unmarshalled string and bytes consumed.
    */
   public static class StringResult {
       public final String value;
       public final int bytesConsumed;


       public StringResult(String value, int bytesConsumed) {
           this.value = value;
           this.bytesConsumed = bytesConsumed;
       }
   }


   // ==================== BYTE ARRAY UTILITIES ====================


   /**
    * Copy bytes from source to destination.
    * @param src source byte array
    * @param srcOffset starting position in source
    * @param dest destination byte array
    * @param destOffset starting position in destination
    * @param length number of bytes to copy
    */
   public static void copyBytes(byte[] src, int srcOffset, byte[] dest, int destOffset, int length) {
       System.arraycopy(src, srcOffset, dest, destOffset, length);
   }


   /**
    * Create a new byte array containing a portion of the source.
    * @param src source byte array
    * @param offset starting position
    * @param length number of bytes to extract
    * @return new byte array with the extracted bytes
    */
   public static byte[] extractBytes(byte[] src, int offset, int length) {
       byte[] result = new byte[length];
       System.arraycopy(src, offset, result, 0, length);
       return result;
   }
}








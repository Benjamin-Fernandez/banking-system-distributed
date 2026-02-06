package client;


import common.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Scanner;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
* UDP-based banking client that provides a text-based interface for users.
* Supports both at-least-once and at-most-once invocation semantics.
*/
public class BankClient {
  
   private final String serverHost;
   private final int serverPort;
   private final boolean atMostOnce;
   private final int timeout;      // Timeout in milliseconds
   private final int maxRetries;
   private final double lossRate;  // Simulated packet loss for testing
  
   private DatagramSocket socket;
   private InetAddress serverAddress;
   private Scanner scanner;
   private Random random;
  
   // Client identification
   private final int clientId;
   private final AtomicInteger requestIdGenerator;
  
   public BankClient(String serverHost, int serverPort, boolean atMostOnce,
                     int timeout, int maxRetries, double lossRate) {
       this.serverHost = serverHost;
       this.serverPort = serverPort;
       this.atMostOnce = atMostOnce;
       this.timeout = timeout;
       this.maxRetries = maxRetries;
       this.lossRate = lossRate;
       this.random = new Random();
       this.clientId = random.nextInt(Integer.MAX_VALUE);
       this.requestIdGenerator = new AtomicInteger(1);
   }
  
   public void start() {
       try {
           socket = new DatagramSocket();
           socket.setSoTimeout(timeout);
           serverAddress = InetAddress.getByName(serverHost);
           scanner = new Scanner(System.in);
          
           printWelcome();
          
           boolean running = true;
           while (running) {
               printMenu();
               System.out.print("Enter choice: ");
               String input = scanner.nextLine().trim();
              
               switch (input) {
                   case "1": handleOpenAccount(); break;
                   case "2": handleCloseAccount(); break;
                   case "3": handleDeposit(); break;
                   case "4": handleWithdraw(); break;
                   case "5": handleMonitor(); break;
                   case "6": handleBankStatement(); break;
                   case "7": handleTransfer(); break;
                   case "0":
                   case "q":
                   case "quit":
                   case "exit":
                       running = false;
                       System.out.println("Goodbye!");
                       break;
                   default:
                       System.out.println("Invalid choice. Please try again.");
               }
               System.out.println();
           }
       } catch (Exception e) {
           System.err.println("Client error: " + e.getMessage());
           e.printStackTrace();
       } finally {
           if (socket != null && !socket.isClosed()) {
               socket.close();
           }
           if (scanner != null) {
               scanner.close();
           }
       }
   }
  
   private void printWelcome() {
       System.out.println("========================================");
       System.out.println("    Distributed Banking Client");
       System.out.println("========================================");
       System.out.println("Server: " + serverHost + ":" + serverPort);
       System.out.println("Client ID: " + clientId);
       System.out.println("Semantics: " + (atMostOnce ? "AT-MOST-ONCE" : "AT-LEAST-ONCE"));
       System.out.println("Timeout: " + timeout + "ms, Max Retries: " + maxRetries);
       if (lossRate > 0) {
           System.out.println("Simulated Loss Rate: " + (lossRate * 100) + "%");
       }
       System.out.println("========================================\n");
   }
  
   private void printMenu() {
       System.out.println("--- Banking Operations ---");
       System.out.println("1. Open Account");
       System.out.println("2. Close Account");
       System.out.println("3. Deposit");
       System.out.println("4. Withdraw");
       System.out.println("5. Monitor Updates");
       System.out.println("6. Bank Statement");
       System.out.println("7. Transfer");
       System.out.println("0. Exit");
   }
  
   // ==================== SEND REQUEST WITH RETRIES ====================
  
   private Message sendRequest(Message request) {
       byte[] requestBytes = request.marshalRequest();
       byte[] buffer = new byte[Message.MAX_MESSAGE_SIZE];
      
       for (int attempt = 1; attempt <= maxRetries; attempt++) {
           try {
               // Simulate packet loss on send
               if (shouldSimulateLoss()) {
                   System.out.println("[Client] Simulating REQUEST loss (attempt " + attempt + ")");
               } else {
                   DatagramPacket sendPacket = new DatagramPacket(
                       requestBytes, requestBytes.length, serverAddress, serverPort);
                   socket.send(sendPacket);
                   System.out.println("[Client] Sent request (attempt " + attempt +
                                     ", reqId=" + request.getRequestId() + ")");
               }
              
               // Wait for reply
               DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
               socket.receive(receivePacket);
              
               // Simulate packet loss on receive
               if (shouldSimulateLoss()) {
                   System.out.println("[Client] Simulating REPLY loss");
                   continue;
               }
              
               Message reply = Message.unmarshalReply(buffer, receivePacket.getLength());
               return reply;


           } catch (SocketTimeoutException e) {
               System.out.println("[Client] Timeout waiting for reply (attempt " + attempt + ")");
               if (attempt < maxRetries) {
                   System.out.println("[Client] Retrying...");
               }
           } catch (Exception e) {
               System.err.println("[Client] Error: " + e.getMessage());
               break;
           }
       }


       System.out.println("[Client] Failed after " + maxRetries + " attempts");
       return null;
   }


   private boolean shouldSimulateLoss() {
       return lossRate > 0 && random.nextDouble() < lossRate;
   }


   private int getNextRequestId() {
       return requestIdGenerator.getAndIncrement();
   }


   // ==================== OPERATION 1: OPEN ACCOUNT ====================


   private void handleOpenAccount() {
       System.out.println("\n--- Open New Account ---");


       System.out.print("Enter your name: ");
       String name = scanner.nextLine().trim();


       System.out.print("Enter password (max 16 chars): ");
       String password = scanner.nextLine().trim();


       System.out.print("Enter initial balance: ");
       float balance = Float.parseFloat(scanner.nextLine().trim());


       // Build payload (no currency - defaults to USD)
       int payloadSize = Marshaller.getStringMarshalledSize(name) +
                         Marshaller.getStringMarshalledSize(password) + 4;
       byte[] payload = new byte[payloadSize];
       int offset = 0;
       offset = Marshaller.marshalString(name, payload, offset);
       offset = Marshaller.marshalString(password, payload, offset);
       Marshaller.marshalFloat(balance, payload, offset);


       // Create and send request
       Message request = createRequest(OperationType.OPEN_ACCOUNT, payload);
       Message reply = sendRequest(request);


       if (reply != null) {
           if (reply.getStatusCode() == StatusCode.SUCCESS) {
               int accountNumber = Marshaller.unmarshalInt(reply.getPayload(), 0);
               System.out.println("\n*** SUCCESS! Account created ***");
               System.out.println("Your account number: " + accountNumber);
           } else {
               printError(reply);
           }
       }
   }


   // ==================== OPERATION 2: CLOSE ACCOUNT ====================


   private void handleCloseAccount() {
       System.out.println("\n--- Close Account ---");


       System.out.print("Enter your name: ");
       String name = scanner.nextLine().trim();


       System.out.print("Enter account number: ");
       int accountNumber = Integer.parseInt(scanner.nextLine().trim());


       System.out.print("Enter password: ");
       String password = scanner.nextLine().trim();


       // Build payload
       int payloadSize = Marshaller.getStringMarshalledSize(name) + 4 +
                         Marshaller.getStringMarshalledSize(password);
       byte[] payload = new byte[payloadSize];
       int offset = 0;
       offset = Marshaller.marshalString(name, payload, offset);
       offset = Marshaller.marshalInt(accountNumber, payload, offset);
       Marshaller.marshalString(password, payload, offset);


       Message request = createRequest(OperationType.CLOSE_ACCOUNT, payload);
       Message reply = sendRequest(request);


       if (reply != null) {
           if (reply.getStatusCode() == StatusCode.SUCCESS) {
               Marshaller.StringResult result = Marshaller.unmarshalString(reply.getPayload(), 0);
               System.out.println("\n*** SUCCESS! ***");
               System.out.println(result.value);
           } else {
               printError(reply);
           }
       }
   }


   // ==================== OPERATION 3: DEPOSIT ====================


   private void handleDeposit() {
       System.out.println("\n--- Deposit Money ---");


       System.out.print("Enter your name: ");
       String name = scanner.nextLine().trim();


       System.out.print("Enter account number: ");
       int accountNumber = Integer.parseInt(scanner.nextLine().trim());


       System.out.print("Enter password: ");
       String password = scanner.nextLine().trim();


       System.out.print("Enter amount to deposit: ");
       float amount = Float.parseFloat(scanner.nextLine().trim());


       // Build payload (no currency - uses account's currency)
       int payloadSize = Marshaller.getStringMarshalledSize(name) + 4 +
                         Marshaller.getStringMarshalledSize(password) + 4;
       byte[] payload = new byte[payloadSize];
       int offset = 0;
       offset = Marshaller.marshalString(name, payload, offset);
       offset = Marshaller.marshalInt(accountNumber, payload, offset);
       offset = Marshaller.marshalString(password, payload, offset);
       Marshaller.marshalFloat(amount, payload, offset);


       Message request = createRequest(OperationType.DEPOSIT, payload);
       Message reply = sendRequest(request);


       if (reply != null) {
           if (reply.getStatusCode() == StatusCode.SUCCESS) {
               float newBalance = Marshaller.unmarshalFloat(reply.getPayload(), 0);
               System.out.println("\n*** SUCCESS! Deposit completed ***");
               System.out.println("New balance: " + newBalance);
           } else {
               printError(reply);
           }
       }
   }


   // ==================== OPERATION 4: WITHDRAW ====================


   private void handleWithdraw() {
       System.out.println("\n--- Withdraw Money ---");


       System.out.print("Enter your name: ");
       String name = scanner.nextLine().trim();


       System.out.print("Enter account number: ");
       int accountNumber = Integer.parseInt(scanner.nextLine().trim());


       System.out.print("Enter password: ");
       String password = scanner.nextLine().trim();


       System.out.print("Enter amount to withdraw: ");
       float amount = Float.parseFloat(scanner.nextLine().trim());


       // Build payload (no currency - uses account's currency)
       int payloadSize = Marshaller.getStringMarshalledSize(name) + 4 +
                         Marshaller.getStringMarshalledSize(password) + 4;
       byte[] payload = new byte[payloadSize];
       int offset = 0;
       offset = Marshaller.marshalString(name, payload, offset);
       offset = Marshaller.marshalInt(accountNumber, payload, offset);
       offset = Marshaller.marshalString(password, payload, offset);
       Marshaller.marshalFloat(amount, payload, offset);


       Message request = createRequest(OperationType.WITHDRAW, payload);
       Message reply = sendRequest(request);


       if (reply != null) {
           if (reply.getStatusCode() == StatusCode.SUCCESS) {
               float newBalance = Marshaller.unmarshalFloat(reply.getPayload(), 0);
               System.out.println("\n*** SUCCESS! Withdrawal completed ***");
               System.out.println("New balance: " + newBalance);
           } else {
               printError(reply);
           }
       }
   }


   // ==================== OPERATION 5: MONITOR ====================


   private void handleMonitor() {
       System.out.println("\n--- Monitor Account Updates ---");
       System.out.print("Enter monitoring duration (seconds): ");
       int seconds = Integer.parseInt(scanner.nextLine().trim());


       // Build payload with monitor interval
       byte[] payload = new byte[4];
       Marshaller.marshalInt(seconds, payload, 0);


       Message request = createRequest(OperationType.MONITOR, payload);
       Message reply = sendRequest(request);


       if (reply != null && reply.getStatusCode() == StatusCode.SUCCESS) {
           System.out.println("\n*** Monitoring started for " + seconds + " seconds ***");
           System.out.println("Waiting for account updates...\n");


           long endTime = System.currentTimeMillis() + (seconds * 1000L);
           byte[] buffer = new byte[Message.MAX_MESSAGE_SIZE];


           // Temporarily increase timeout for monitoring
           try {
               socket.setSoTimeout(1000);  // Check every second


               while (System.currentTimeMillis() < endTime) {
                   try {
                       DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                       socket.receive(packet);


                       Message callback = Message.unmarshalReply(buffer, packet.getLength());
                       displayCallback(callback);


                   } catch (SocketTimeoutException e) {
                       // Continue waiting
                   }
               }


               socket.setSoTimeout(timeout);  // Restore original timeout
               System.out.println("\n*** Monitoring period ended ***");


           } catch (Exception e) {
               System.err.println("Error during monitoring: " + e.getMessage());
           }
       } else {
           System.out.println("Failed to register for monitoring");
       }
   }


   private void displayCallback(Message callback) {
       byte[] payload = callback.getPayload();
       int offset = 0;


       Marshaller.StringResult updateTypeResult = Marshaller.unmarshalString(payload, offset);
       offset += updateTypeResult.bytesConsumed;


       int accountNumber = Marshaller.unmarshalInt(payload, offset);
       offset += 4;


       Marshaller.StringResult holderResult = Marshaller.unmarshalString(payload, offset);
       offset += holderResult.bytesConsumed;


       int currencyCode = Marshaller.unmarshalByte(payload, offset);
       offset += 1;


       float balance = Marshaller.unmarshalFloat(payload, offset);


       CurrencyType currency = CurrencyType.fromCode(currencyCode);


       System.out.println(">>> UPDATE: " + updateTypeResult.value);
       System.out.println("    Account #" + accountNumber + " (" + holderResult.value + ")");
       System.out.println("    Balance: " + balance + " " + currency.name());
       System.out.println();
   }


   // ==================== OPERATION 6: BANK STATEMENT ====================


   private void handleBankStatement() {
       System.out.println("\n--- Bank Statement ---");


       System.out.print("Enter your name: ");
       String name = scanner.nextLine().trim();


       System.out.print("Enter account number: ");
       int accountNumber = Integer.parseInt(scanner.nextLine().trim());


       System.out.print("Enter password: ");
       String password = scanner.nextLine().trim();


       int payloadSize = Marshaller.getStringMarshalledSize(name) + 4 +
                         Marshaller.getStringMarshalledSize(password);
       byte[] payload = new byte[payloadSize];
       int offset = 0;
       offset = Marshaller.marshalString(name, payload, offset);
       offset = Marshaller.marshalInt(accountNumber, payload, offset);
       Marshaller.marshalString(password, payload, offset);


       Message request = createRequest(OperationType.BANK_STATEMENT, payload);
       Message reply = sendRequest(request);


       if (reply != null) {
           if (reply.getStatusCode() == StatusCode.SUCCESS) {
               Marshaller.StringResult result = Marshaller.unmarshalString(reply.getPayload(), 0);
               System.out.println("\n" + result.value);
           } else {
               printError(reply);
           }
       }
   }


   // ==================== OPERATION 7: TRANSFER ====================


   private void handleTransfer() {
       System.out.println("\n--- Transfer Money ---");


       System.out.print("Enter your name: ");
       String name = scanner.nextLine().trim();


       System.out.print("Enter your account number (source): ");
       int fromAccount = Integer.parseInt(scanner.nextLine().trim());


       System.out.print("Enter password: ");
       String password = scanner.nextLine().trim();


       System.out.print("Enter destination account number: ");
       int toAccount = Integer.parseInt(scanner.nextLine().trim());


       System.out.print("Enter amount to transfer: ");
       float amount = Float.parseFloat(scanner.nextLine().trim());


       int payloadSize = Marshaller.getStringMarshalledSize(name) + 4 +
                         Marshaller.getStringMarshalledSize(password) + 4 + 4;
       byte[] payload = new byte[payloadSize];
       int offset = 0;
       offset = Marshaller.marshalString(name, payload, offset);
       offset = Marshaller.marshalInt(fromAccount, payload, offset);
       offset = Marshaller.marshalString(password, payload, offset);
       offset = Marshaller.marshalInt(toAccount, payload, offset);
       Marshaller.marshalFloat(amount, payload, offset);


       Message request = createRequest(OperationType.TRANSFER, payload);
       Message reply = sendRequest(request);


       if (reply != null) {
           if (reply.getStatusCode() == StatusCode.SUCCESS) {
               float newBalance = Marshaller.unmarshalFloat(reply.getPayload(), 0);
               System.out.println("\n*** SUCCESS! Transfer completed ***");
               System.out.println("Your new balance: " + newBalance);
           } else {
               printError(reply);
           }
       }
   }


   // ==================== HELPER METHODS ====================


   private Message createRequest(OperationType operation, byte[] payload) {
       Message request = new Message();
       request.setRequestId(getNextRequestId());
       request.setOperationCode((byte) operation.getCode());
       request.setClientId(clientId);
       request.setSemanticsType(atMostOnce ? Message.SEMANTICS_AT_MOST_ONCE : Message.SEMANTICS_AT_LEAST_ONCE);
       request.setPayload(payload);
       return request;
   }


   private void printError(Message reply) {
       System.out.println("\n*** ERROR ***");
       System.out.println("Status: " + StatusCode.getMessage(reply.getStatusCode()));
       if (reply.getPayload().length > 0) {
           Marshaller.StringResult msg = Marshaller.unmarshalString(reply.getPayload(), 0);
           System.out.println("Details: " + msg.value);
       }
   }


   // ==================== MAIN ====================


   public static void main(String[] args) {
       String host = "localhost";
       int port = 8888;
       boolean atMostOnce = false;
       int timeout = 3000;
       int maxRetries = 3;
       double lossRate = 0.0;


       for (int i = 0; i < args.length; i++) {
           switch (args[i]) {
               case "-h":
               case "--host":
                   if (i + 1 < args.length) host = args[++i];
                   break;
               case "-p":
               case "--port":
                   if (i + 1 < args.length) port = Integer.parseInt(args[++i]);
                   break;
               case "-s":
               case "--semantics":
                   if (i + 1 < args.length) {
                       String sem = args[++i].toLowerCase();
                       atMostOnce = sem.equals("atmost") || sem.equals("at-most-once");
                   }
                   break;
               case "-t":
               case "--timeout":
                   if (i + 1 < args.length) timeout = Integer.parseInt(args[++i]);
                   break;
               case "-r":
               case "--retries":
                   if (i + 1 < args.length) maxRetries = Integer.parseInt(args[++i]);
                   break;
               case "-l":
               case "--loss":
                   if (i + 1 < args.length) lossRate = Double.parseDouble(args[++i]);
                   break;
               case "--help":
                   printUsage();
                   return;
           }
       }


       BankClient client = new BankClient(host, port, atMostOnce, timeout, maxRetries, lossRate);
       client.start();
   }


   private static void printUsage() {
       System.out.println("Usage: java client.BankClient [options]");
       System.out.println("Options:");
       System.out.println("  -h, --host <host>       Server hostname (default: localhost)");
       System.out.println("  -p, --port <port>       Server port (default: 8888)");
       System.out.println("  -s, --semantics <type>  Invocation semantics:");
       System.out.println("                          atleast (default) - at-least-once");
       System.out.println("                          atmost - at-most-once");
       System.out.println("  -t, --timeout <ms>      Request timeout in milliseconds (default: 3000)");
       System.out.println("  -r, --retries <count>   Maximum retry attempts (default: 3)");
       System.out.println("  -l, --loss <rate>       Simulated packet loss rate 0.0-1.0 (default: 0.0)");
       System.out.println("  --help                  Show this help message");
   }
}








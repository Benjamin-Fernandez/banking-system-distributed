package server;


import common.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;


/**
* UDP-based banking server that handles client requests.
* Supports both at-least-once and at-most-once invocation semantics.
*/
public class BankServer {
  
   private final int port;
   private final boolean atMostOnce;
   private final double lossRate;  // Simulated packet loss rate (0.0 to 1.0)
  
   private DatagramSocket socket;
   private BankService bankService;
   private RequestHandler requestHandler;
   private Random random;
  
   // For at-most-once: history of processed requests (clientId -> (requestId -> reply))
   private Map<Integer, Map<Integer, byte[]>> replyHistory;
  
   // For monitoring: registered clients
   private List<MonitorClient> monitorClients;
  
   // Inner class for tracking monitoring clients
   private static class MonitorClient {
       InetAddress address;
       int port;
       long expirationTime;
      
       MonitorClient(InetAddress address, int port, long expirationTime) {
           this.address = address;
           this.port = port;
           this.expirationTime = expirationTime;
       }
   }
  
   public BankServer(int port, boolean atMostOnce, double lossRate) {
       this.port = port;
       this.atMostOnce = atMostOnce;
       this.lossRate = lossRate;
       this.random = new Random();
       this.replyHistory = new ConcurrentHashMap<>();
       this.monitorClients = new ArrayList<>();
   }
  
   public void start() {
       try {
           socket = new DatagramSocket(port);
           bankService = new BankService();
           requestHandler = new RequestHandler(bankService);
          
           // Set up monitoring callback
           bankService.setUpdateCallback(this::notifyMonitors);
          
           System.out.println("========================================");
           System.out.println("    Distributed Banking Server");
           System.out.println("========================================");
           System.out.println("Port: " + port);
           System.out.println("Semantics: " + (atMostOnce ? "AT-MOST-ONCE" : "AT-LEAST-ONCE"));
           System.out.println("Simulated Loss Rate: " + (lossRate * 100) + "%");
           System.out.println("========================================");
           System.out.println("Server is running. Waiting for requests...\n");
          
           byte[] buffer = new byte[Message.MAX_MESSAGE_SIZE];
          
           while (true) {
               DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
               socket.receive(packet);
              
               // Simulate packet loss for incoming requests
               if (shouldSimulateLoss()) {
                   System.out.println("[Server] Simulating REQUEST loss (packet dropped)");
                   continue;
               }
              
               processPacket(packet);
           }
       } catch (Exception e) {
           System.err.println("Server error: " + e.getMessage());
           e.printStackTrace();
       } finally {
           if (socket != null && !socket.isClosed()) {
               socket.close();
           }
       }
   }
  
   private void processPacket(DatagramPacket packet) {
       try {
           Message request = Message.unmarshalRequest(packet.getData(), packet.getLength());
           InetAddress clientAddress = packet.getAddress();
           int clientPort = packet.getPort();
          
           System.out.println("[Server] Received: " + request + " from " +
                             clientAddress.getHostAddress() + ":" + clientPort);
          
           // Check if this is a MONITOR request
           if (request.getOperationCode() == OperationType.MONITOR.getCode()) {
               handleMonitorRequest(request, clientAddress, clientPort);
               return;
           }
          
           byte[] replyBytes;
          
           // At-most-once: check for duplicate requests
           if (atMostOnce) {
               replyBytes = checkDuplicateRequest(request.getClientId(), request.getRequestId());
               if (replyBytes != null) {
                   System.out.println("[Server] Duplicate request detected. Returning cached reply.");
                   sendReply(replyBytes, clientAddress, clientPort);
                   return;
               }
           }
          
           // Process the request
           Message reply = requestHandler.handleRequest(request);
           replyBytes = reply.marshalReply();
          
           // At-most-once: store reply in history
           if (atMostOnce) {
               storeReply(request.getClientId(), request.getRequestId(), replyBytes);
           }
          
           // Simulate packet loss for outgoing replies
           if (shouldSimulateLoss()) {
               System.out.println("[Server] Simulating REPLY loss (packet dropped)");
               return;
           }
          
           sendReply(replyBytes, clientAddress, clientPort);
           System.out.println("[Server] Sent reply: status=" + reply.getStatusCode());
          
       } catch (Exception e) {
           System.err.println("[Server] Error processing packet: " + e.getMessage());
           e.printStackTrace();
       }
   }
  
   private void sendReply(byte[] data, InetAddress address, int port) throws Exception {
       DatagramPacket replyPacket = new DatagramPacket(data, data.length, address, port);
       socket.send(replyPacket);
   }


   private boolean shouldSimulateLoss() {
       return lossRate > 0 && random.nextDouble() < lossRate;
   }


   // ==================== AT-MOST-ONCE DUPLICATE HANDLING ====================


   private byte[] checkDuplicateRequest(int clientId, int requestId) {
       Map<Integer, byte[]> clientHistory = replyHistory.get(clientId);
       if (clientHistory != null) {
           return clientHistory.get(requestId);
       }
       return null;
   }


   private void storeReply(int clientId, int requestId, byte[] reply) {
       replyHistory.computeIfAbsent(clientId, k -> new HashMap<>()).put(requestId, reply);
   }


   // ==================== MONITORING ====================


   private void handleMonitorRequest(Message request, InetAddress clientAddress, int clientPort) {
       try {
           // Parse monitor interval from payload (in seconds)
           byte[] payload = request.getPayload();
           int intervalSeconds = Marshaller.unmarshalInt(payload, 0);


           // Calculate expiration time
           long expirationTime = System.currentTimeMillis() + (intervalSeconds * 1000L);


           // Register the client for monitoring
           synchronized (monitorClients) {
               // Remove any existing registration for this client
               monitorClients.removeIf(c -> c.address.equals(clientAddress) && c.port == clientPort);
               monitorClients.add(new MonitorClient(clientAddress, clientPort, expirationTime));
           }


           System.out.println("[Server] Client " + clientAddress + ":" + clientPort +
                             " registered for monitoring for " + intervalSeconds + " seconds");


           // Send acknowledgment
           Message reply = Message.createSuccessReply(request.getRequestId(), new byte[0]);
           sendReply(reply.marshalReply(), clientAddress, clientPort);


       } catch (Exception e) {
           System.err.println("[Server] Error handling monitor request: " + e.getMessage());
       }
   }


   private void notifyMonitors(String updateType, BankAccount account) {
       // Clean up expired monitors and send updates to active ones
       long currentTime = System.currentTimeMillis();


       // Build update message
       byte[] updateData = buildAccountUpdatePayload(updateType, account);


       synchronized (monitorClients) {
           Iterator<MonitorClient> iterator = monitorClients.iterator();
           while (iterator.hasNext()) {
               MonitorClient client = iterator.next();


               if (currentTime > client.expirationTime) {
                   System.out.println("[Server] Monitor client expired: " +
                                     client.address + ":" + client.port);
                   iterator.remove();
                   continue;
               }


               try {
                   // Create callback message
                   Message callback = new Message();
                   callback.setMessageType(Message.TYPE_CALLBACK);
                   callback.setRequestId(0);  // Callbacks don't have request IDs
                   callback.setStatusCode((byte) StatusCode.SUCCESS);
                   callback.setPayload(updateData);


                   byte[] callbackBytes = callback.marshalReply();


                   if (!shouldSimulateLoss()) {
                       sendReply(callbackBytes, client.address, client.port);
                       System.out.println("[Server] Sent callback to " +
                                         client.address + ":" + client.port);
                   } else {
                       System.out.println("[Server] Simulating callback loss to " +
                                         client.address + ":" + client.port);
                   }
               } catch (Exception e) {
                   System.err.println("[Server] Error sending callback: " + e.getMessage());
               }
           }
       }
   }


   private byte[] buildAccountUpdatePayload(String updateType, BankAccount account) {
       // Format: updateType (string) + accountNumber (4) + holderName (string) +
       //         currency (1) + balance (4)
       int size = Marshaller.getStringMarshalledSize(updateType) + 4 +
                  Marshaller.getStringMarshalledSize(account.getHolderName()) + 1 + 4;
       byte[] payload = new byte[size];
       int offset = 0;


       offset = Marshaller.marshalString(updateType, payload, offset);
       offset = Marshaller.marshalInt(account.getAccountNumber(), payload, offset);
       offset = Marshaller.marshalString(account.getHolderName(), payload, offset);
       offset = Marshaller.marshalByte((byte) account.getCurrency().getCode(), payload, offset);
       Marshaller.marshalFloat(account.getBalance(), payload, offset);


       return payload;
   }


   // ==================== MAIN ====================


   public static void main(String[] args) {
       int port = 8888;
       boolean atMostOnce = false;
       double lossRate = 0.0;


       // Parse command line arguments
       for (int i = 0; i < args.length; i++) {
           switch (args[i]) {
               case "-p":
               case "--port":
                   if (i + 1 < args.length) {
                       port = Integer.parseInt(args[++i]);
                   }
                   break;
               case "-s":
               case "--semantics":
                   if (i + 1 < args.length) {
                       String sem = args[++i].toLowerCase();
                       atMostOnce = sem.equals("atmost") || sem.equals("at-most-once");
                   }
                   break;
               case "-l":
               case "--loss":
                   if (i + 1 < args.length) {
                       lossRate = Double.parseDouble(args[++i]);
                   }
                   break;
               case "-h":
               case "--help":
                   printUsage();
                   return;
           }
       }


       BankServer server = new BankServer(port, atMostOnce, lossRate);
       server.start();
   }


   private static void printUsage() {
       System.out.println("Usage: java server.BankServer [options]");
       System.out.println("Options:");
       System.out.println("  -p, --port <port>       Server port (default: 8888)");
       System.out.println("  -s, --semantics <type>  Invocation semantics:");
       System.out.println("                          atleast (default) - at-least-once");
       System.out.println("                          atmost - at-most-once");
       System.out.println("  -l, --loss <rate>       Simulated packet loss rate 0.0-1.0 (default: 0.0)");
       System.out.println("  -h, --help              Show this help message");
   }
}








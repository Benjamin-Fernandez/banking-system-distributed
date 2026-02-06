# Development Documentation


## Table of Contents
1. [Quick Start Guide](#quick-start-guide)
2. [System Architecture](#system-architecture)
3. [How It Works](#how-it-works)
4. [Request/Reply Message Structure](#requestreply-message-structure)
5. [Marshalling/Unmarshalling](#marshallingunmarshalling)
6. [Operation Payloads](#operation-payloads)
7. [Invocation Semantics](#invocation-semantics)
8. [Additional Operations Design](#additional-operations-design)
9. [Important Functions](#important-functions)
10. [Experiment Design](#experiment-design)
11. [Status Codes](#status-codes)
12. [Monitoring/Callback Flow](#monitoringcallback-flow)


---


## Quick Start Guide


### Prerequisites
- Java 17 or higher installed
- Terminal/Command Prompt access


### Step 1: Compile the Project


```bash
# Navigate to project directory
cd /path/to/dist


# Create output directory and compile all Java files
mkdir -p bin
javac -d bin src/common/*.java src/server/*.java src/client/*.java
```


### Step 2: Start the Server


**Basic server (at-least-once semantics, no packet loss):**
```bash
java -cp bin server.BankServer
```


**Server with at-most-once semantics:**
```bash
java -cp bin server.BankServer --semantics atmost
```


**Server with simulated 30% packet loss (for testing):**
```bash
java -cp bin server.BankServer --semantics atmost --loss 0.3
```


### Step 3: Start the Client (in a new terminal)


**Basic client:**
```bash
java -cp bin client.BankClient
```


**Client with at-most-once semantics:**
```bash
java -cp bin client.BankClient --semantics atmost
```


**Client with packet loss simulation:**
```bash
java -cp bin client.BankClient --semantics atmost --loss 0.3 --timeout 2000 --retries 5
```


### Step 4: Test Operations


Once the client starts, you'll see a menu:
```
--- Banking Operations ---
1. Open Account
2. Close Account
3. Deposit
4. Withdraw
5. Monitor Updates
6. Bank Statement
7. Transfer
0. Exit
```


### Testing Scenarios


**Scenario A: Normal Operation (No Packet Loss)**
```bash
# Terminal 1 - Server
java -cp bin server.BankServer --semantics atmost


# Terminal 2 - Client
java -cp bin client.BankClient --semantics atmost
```


**Scenario B: At-Most-Once with Packet Loss (Correct Behavior)**
```bash
# Terminal 1 - Server with 30% loss
java -cp bin server.BankServer --semantics atmost --loss 0.3


# Terminal 2 - Client with 30% loss
java -cp bin client.BankClient --semantics atmost --loss 0.3 --retries 10
```


**Scenario C: At-Least-Once with Packet Loss (Demonstrates Problem)**
```bash
# Terminal 1 - Server with 30% loss
java -cp bin server.BankServer --semantics atleast --loss 0.3


# Terminal 2 - Client with 30% loss
java -cp bin client.BankClient --semantics atleast --loss 0.3 --retries 10
```


**Scenario D: Monitoring with Multiple Clients**
```bash
# Terminal 1 - Server
java -cp bin server.BankServer --semantics atmost


# Terminal 2 - Monitoring Client
java -cp bin client.BankClient --semantics atmost
# Choose option 5, enter 60 seconds


# Terminal 3 - Active Client
java -cp bin client.BankClient --semantics atmost
# Perform deposits, withdrawals - monitor client will see updates
```


---


## System Architecture


```
┌─────────────────────────────────────────────────────────────────────┐
│                         DISTRIBUTED BANKING SYSTEM                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐         UDP Messages          ┌──────────────────┐│
│  │   Client 1   │ ◄────────────────────────────►│                  ││
│  └──────────────┘                                │                  ││
│                                                  │   Bank Server    ││
│  ┌──────────────┐         UDP Messages          │                  ││
│  │   Client 2   │ ◄────────────────────────────►│  ┌────────────┐  ││
│  └──────────────┘                                │  │BankService │  ││
│       ...              Callbacks (Monitoring)    │  └────────────┘  ││
│  ┌──────────────┐ ◄─────────────────────────────│                  ││
│  │   Client N   │                                │  ┌────────────┐  ││
│  └──────────────┘                                │  │ Accounts   │  ││
│                                                  │  │ (In-Memory)│  ││
│                                                  │  └────────────┘  ││
│                                                  └──────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```


---


## How It Works


This section explains the complete request-response flow from client to server and back.


### Component Interaction Overview


```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        REQUEST FLOW (Client → Server)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  User Input    BankClient     Message      Marshaller    UDP Socket         │
│      │              │            │              │             │              │
│      │─(1)─────────►│            │              │             │              │
│      │  "Deposit    │─(2)───────►│              │             │              │
│      │   $100"      │  Create    │─(3)─────────►│             │              │
│      │              │  request   │  Marshal     │─(4)────────►│              │
│      │              │  message   │  to bytes    │  Send       │──────────────┼──►
│      │              │            │              │  packet     │   Network    │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ──►UDP Socket   BankServer  RequestHandler  BankService  BankAccount       │
│        │              │            │              │             │            │
│   ─────┼─(5)─────────►│            │              │             │            │
│        │  Receive     │─(6)───────►│              │             │            │
│        │  packet      │  Parse     │─(7)─────────►│             │            │
│        │              │  request   │  Execute     │─(8)────────►│            │
│        │              │            │  operation   │  Update     │            │
│        │              │            │              │  balance    │            │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                        REPLY FLOW (Server → Client)                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  BankAccount   BankService  RequestHandler  BankServer   UDP Socket         │
│      │              │            │              │             │              │
│      │─(9)─────────►│            │              │             │              │
│      │  Return      │─(10)──────►│              │             │              │
│      │  new balance │  Return    │─(11)────────►│             │              │
│      │              │  result    │  Marshal     │─(12)───────►│              │
│      │              │            │  reply       │  Send       │──────────────┼──►
│      │              │            │              │  packet     │   Network    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```


### Step-by-Step Request Processing


#### Phase 1: Client-Side Request Creation


1. **User Input**: User selects an operation from the menu (e.g., "3. Deposit")
2. **Input Collection**: `BankClient.handleDeposit()` prompts for account details
3. **Payload Creation**: Client marshals operation data into byte array:
  ```java
  // Example: Deposit $100 to account 1001
  byte[] payload = new byte[256];
  int offset = 0;
  offset = Marshaller.marshalString("John", payload, offset);     // holder name
  offset = Marshaller.marshalInt(1001, payload, offset);          // account number
  offset = Marshaller.marshalString("secret123", payload, offset); // password
  offset = Marshaller.marshalByte(CurrencyType.USD.ordinal(), payload, offset); // currency
  offset = Marshaller.marshalFloat(100.0f, payload, offset);      // amount
  ```


4. **Message Creation**: `BankClient.createRequest()` wraps payload in Message:
  ```java
  Message request = new Message();
  request.setRequestId(nextRequestId++);
  request.setOperationType(OperationType.DEPOSIT);
  request.setClientId(clientId);
  request.setSemanticsType(semanticsType);
  request.setPayload(payload);
  ```


5. **Request Marshalling**: `Message.marshalRequest()` creates final byte array:
  ```
  [RequestID:4][OpCode:1][ClientID:4][PayloadLen:2][Semantics:1][Payload:n]
  ```


#### Phase 2: Network Transmission


6. **UDP Send**: Client sends datagram packet:
  ```java
  DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);
  socket.send(packet);
  System.out.println("[Client] Sent request (attempt " + attempt + "/" + maxRetries + ")");
  ```


7. **Packet Loss Simulation** (if enabled):
  ```java
  if (random.nextFloat() < lossRate) {
     System.out.println("[Client] Simulating REQUEST loss - not sending");
     return;  // Packet dropped
  }
  ```


#### Phase 3: Server-Side Processing


8. **UDP Receive**: `BankServer.run()` receives packet:
  ```java
  DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
  socket.receive(packet);
  System.out.println("[Server] Received request from " + packet.getAddress());
  ```


9. **Duplicate Detection** (At-Most-Once only):
  ```java
  byte[] cached = checkDuplicateRequest(clientId, requestId);
  if (cached != null) {
     System.out.println("[Server] Duplicate request detected - returning cached reply");
     socket.send(new DatagramPacket(cached, cached.length, clientAddress, clientPort));
     return;
  }
  ```


10. **Request Unmarshalling**: `Message.unmarshalRequest()` parses bytes to Message object


11. **Request Handling**: `RequestHandler.handleRequest()` dispatches to appropriate method:
   ```java
   switch (request.getOperationType()) {
      case DEPOSIT -> handleDeposit(request);
      case WITHDRAW -> handleWithdraw(request);
      // ... other operations
   }
   ```


12. **Business Logic**: `BankService.deposit()` executes operation:
   ```java
   public ServiceResult deposit(int accountNumber, String name, String password,
                                CurrencyType currency, float amount) {
      BankAccount account = accounts.get(accountNumber);
      if (account == null) return error(ERROR_ACCOUNT_NOT_FOUND);
      if (!account.verifyPassword(password)) return error(ERROR_WRONG_PASSWORD);
      account.deposit(amount);
      updateCallback.onUpdate("DEPOSIT", account);  // Notify monitors
      return success(account.getBalance());
   }
   ```


#### Phase 4: Reply Transmission


13. **Reply Creation**: `RequestHandler` creates reply message:
   ```java
   byte[] replyPayload = new byte[4];
   Marshaller.marshalFloat(newBalance, replyPayload, 0);
   return Message.createSuccessReply(requestId, replyPayload);
   ```


14. **Reply Caching** (At-Most-Once only):
   ```java
   byte[] replyBytes = reply.marshalReply();
   storeReply(clientId, requestId, replyBytes);
   System.out.println("[Server] Stored reply in history for duplicate detection");
   ```


15. **UDP Send**: Server sends reply packet:
   ```java
   socket.send(new DatagramPacket(replyBytes, replyBytes.length, clientAddress, port));
   System.out.println("[Server] Sent reply for request " + requestId);
   ```


#### Phase 5: Client-Side Reply Processing


16. **UDP Receive**: Client receives reply (or times out):
   ```java
   socket.setSoTimeout(timeout);
   try {
      socket.receive(replyPacket);
      System.out.println("[Client] Received reply");
   } catch (SocketTimeoutException e) {
      System.out.println("[Client] Timeout - will retry");
      continue;  // Retry loop
   }
   ```


17. **Reply Unmarshalling**: `Message.unmarshalReply()` parses reply


18. **Result Display**: Client shows result to user:
   ```
   Deposit successful! New balance: $600.00
   ```


### Operation-Specific Flows


#### Open Account Flow
```
User → Enter name, password, currency, initial balance
    → BankClient.handleOpenAccount()
    → Marshal: [name][password][currency][balance]
    → Server: BankService.openAccount()
    → Create new BankAccount, assign number
    → Reply: [accountNumber]
    → Display: "Account created! Number: 1001"
```


#### Deposit Flow
```
User → Enter account#, name, password, currency, amount
    → BankClient.handleDeposit()
    → Marshal: [name][account#][password][currency][amount]
    → Server: BankService.deposit()
    → Verify credentials, add to balance
    → Notify monitors if registered
    → Reply: [newBalance]
    → Display: "Deposit successful! Balance: $600.00"
```


#### Transfer Flow
```
User → Enter from-account, to-account, password, amount
    → BankClient.handleTransfer()
    → Marshal: [name][fromAccount][password][toAccount][amount]
    → Server: BankService.transfer()
    → Verify source account credentials
    → Withdraw from source, deposit to destination
    → Record transactions in both accounts
    → Notify monitors for both accounts
    → Reply: [newSourceBalance]
    → Display: "Transfer successful! New balance: $400.00"
```


#### Monitor Flow
```
User → Enter monitoring duration (seconds)
    → BankClient.handleMonitor()
    → Marshal: [intervalSeconds]
    → Server: Register client for callbacks
    → Reply: ACK
    → Client enters receive loop
    → When other clients perform operations:
      → Server: notifyMonitors() sends callback
      → Monitor client receives and displays update
    → After interval expires, monitoring ends
```


---


## Request/Reply Message Structure


### Request Message Format (12-byte header + payload)


```
┌───────────────┬──────────────┬────────────────────────────────────────┐
│ Field         │ Size (bytes) │ Description                            │
├───────────────┼──────────────┼────────────────────────────────────────┤
│ Request ID    │ 4            │ Unique identifier for duplicate detect │
│ Operation Code│ 1            │ Type of operation (0-6)                │
│ Client ID     │ 4            │ Unique client identifier               │
│ Payload Length│ 2            │ Length of payload in bytes             │
│ Semantics Type│ 1            │ 0=at-least-once, 1=at-most-once        │
├───────────────┼──────────────┼────────────────────────────────────────┤
│ Payload       │ Variable     │ Operation-specific marshalled data     │
└───────────────┴──────────────┴────────────────────────────────────────┘
```


### Reply Message Format (8-byte header + payload)


```
┌───────────────┬──────────────┬────────────────────────────────────────┐
│ Field         │ Size (bytes) │ Description                            │
├───────────────┼──────────────┼────────────────────────────────────────┤
│ Request ID    │ 4            │ Echo of original request ID            │
│ Status Code   │ 1            │ 0=success, 1-255=error codes           │
│ Payload Length│ 2            │ Length of payload in bytes             │
│ Reserved      │ 1            │ Reserved for future use                │
├───────────────┼──────────────┼────────────────────────────────────────┤
│ Payload       │ Variable     │ Operation-specific result data         │
└───────────────┴──────────────┴────────────────────────────────────────┘
```


## Marshalling/Unmarshalling


All data is marshalled into byte arrays using big-endian byte order.


### Data Type Marshalling


| Data Type | Size | Method |
|-----------|------|--------|
| Integer (32-bit) | 4 bytes | Big-endian, MSB first |
| Short (16-bit) | 2 bytes | Big-endian, MSB first |
| Float | 4 bytes | IEEE 754 via `Float.floatToIntBits()` |
| String | 2 + n bytes | 2-byte length prefix + UTF-8 bytes |
| Byte | 1 byte | Direct copy |
| Boolean | 1 byte | 0=false, 1=true |
| Enum | 1 byte | Ordinal value |


### String Marshalling Example


```java
// Marshalling "Hello" (5 chars)
// [0x00, 0x05, 'H', 'e', 'l', 'l', 'o'] = 7 bytes total
public static int marshalString(String value, byte[] buffer, int offset) {
   byte[] stringBytes = value.getBytes(StandardCharsets.UTF_8);
   short length = (short) stringBytes.length;
   offset = marshalShort(length, buffer, offset);
   System.arraycopy(stringBytes, 0, buffer, offset, stringBytes.length);
   return offset + stringBytes.length;
}
```


## Operation Payloads


### 1. Open Account
**Request Payload:** `name(string) + password(string) + currency(1) + balance(4)`
**Reply Payload:** `accountNumber(4)`


### 2. Close Account
**Request Payload:** `name(string) + accountNumber(4) + password(string)`
**Reply Payload:** `message(string)`


### 3. Deposit / Withdraw
**Request Payload:** `name(string) + accountNumber(4) + password(string) + currency(1) + amount(4)`
**Reply Payload:** `newBalance(4)`


### 4. Monitor
**Request Payload:** `intervalSeconds(4)`
**Reply Payload:** Empty (acknowledgment)
**Callback Payload:** `updateType(string) + accountNumber(4) + holderName(string) + currency(1) + balance(4)`


### 5. Bank Statement (Idempotent)
**Request Payload:** `name(string) + accountNumber(4) + password(string)`
**Reply Payload:** `statementText(string)`


### 6. Transfer (Non-Idempotent)
**Request Payload:** `name(string) + fromAccount(4) + password(string) + toAccount(4) + amount(4)`
**Reply Payload:** `newBalance(4)`


## Invocation Semantics


### At-Least-Once Semantics


```
┌────────────┐                           ┌────────────┐
│   Client   │                           │   Server   │
├────────────┤                           ├────────────┤
│ 1. Send    │──── Request ────────────►│            │
│            │         ✗ (lost)          │            │
│ 2. Timeout │                           │            │
│ 3. Retry   │──── Request ────────────►│ Process    │
│            │                           │ (executes) │
│            │◄─── Reply ────────────────│            │
│            │         ✗ (lost)          │            │
│ 4. Timeout │                           │            │
│ 5. Retry   │──── Request ────────────►│ Process    │
│            │                           │ (EXECUTES  │
│            │                           │  AGAIN!)   │
│            │◄─── Reply ────────────────│            │
│ 6. Done    │                           │            │
└────────────┘                           └────────────┘


RISK: Non-idempotent operations execute multiple times!
```


### At-Most-Once Semantics


```
┌────────────┐                           ┌────────────┐
│   Client   │                           │   Server   │
├────────────┤                           ├────────────┤
│ 1. Send    │──── Request (ID=42) ────►│            │
│            │         ✗ (lost)          │            │
│ 2. Timeout │                           │            │
│ 3. Retry   │──── Request (ID=42) ────►│ Process    │
│            │                           │ Store reply│
│            │◄─── Reply ────────────────│ in history │
│            │         ✗ (lost)          │            │
│ 4. Timeout │                           │            │
│ 5. Retry   │──── Request (ID=42) ────►│ Duplicate! │
│            │                           │ Return     │
│            │◄─── Cached Reply ─────────│ cached     │
│ 6. Done    │                           │            │
└────────────┘                           └────────────┘


GUARANTEE: Operation executes AT MOST once!
```


### Implementation Details


**Server-side (At-Most-Once):**
```java
// History: Map<ClientID, Map<RequestID, CachedReply>>
private Map<Integer, Map<Integer, byte[]>> replyHistory;


// Check for duplicate
byte[] cached = replyHistory.get(clientId).get(requestId);
if (cached != null) {
   return cached;  // Return cached reply without re-executing
}


// Process request
Message reply = processRequest(request);


// Store in history for future duplicate detection
replyHistory.get(clientId).put(requestId, reply.marshal());
```


**Client-side (Both semantics):**
```java
// Same request ID used for all retries
int requestId = getNextRequestId();
for (int attempt = 0; attempt < maxRetries; attempt++) {
   send(request);  // Same requestId each time
   try {
       return receive(timeout);
   } catch (TimeoutException e) {
       continue;  // Retry with same requestId
   }
}
```


## Additional Operations Design


### Bank Statement (Idempotent)


**Rationale:** Reading account history and generating a statement does not modify any state. Calling it multiple times returns the same result (assuming no intervening transactions).


**Implementation:**
- Reads transaction list from BankAccount
- Formats into human-readable string
- Returns balance and transaction history
- No state modification occurs


**Why it's idempotent:** `f(x) = f(f(x))` - multiple calls produce same result without side effects.


### Transfer (Non-Idempotent)


**Rationale:** Transferring money modifies the balance of two accounts. Executing it twice would transfer money twice, resulting in incorrect balances.


**Implementation:**
- Debits source account
- Credits destination account
- Records transactions in both accounts


**Why it's non-idempotent:** `transfer(A, B, 100)` executed twice results in $200 transferred, not $100.


## Important Functions


### Marshaller.java
| Function | Purpose |
|----------|---------|
| `marshalInt(int, byte[], int)` | Convert 32-bit integer to 4 bytes |
| `unmarshalInt(byte[], int)` | Extract 32-bit integer from bytes |
| `marshalFloat(float, byte[], int)` | Convert float using IEEE 754 |
| `marshalString(String, byte[], int)` | Length-prefixed UTF-8 encoding |
| `unmarshalString(byte[], int)` | Returns StringResult with value and bytes consumed |


### Message.java
| Function | Purpose |
|----------|---------|
| `marshalRequest()` | Convert request to byte array for transmission |
| `unmarshalRequest(byte[], int)` | Parse received bytes into Message object |
| `createSuccessReply(int, byte[])` | Factory method for success responses |
| `createErrorReply(int, int)` | Factory method for error responses |


### BankServer.java
| Function | Purpose |
|----------|---------|
| `processPacket(DatagramPacket)` | Main request processing loop |
| `checkDuplicateRequest(int, int)` | At-most-once duplicate detection |
| `notifyMonitors(String, BankAccount)` | Send callbacks to monitoring clients |


### BankClient.java
| Function | Purpose |
|----------|---------|
| `sendRequest(Message)` | Send with retry logic |
| `createRequest(OperationType, byte[])` | Build request message |
| `handleMonitor()` | Long-poll for callbacks |


## Experiment Design


### Objective
Demonstrate that at-least-once semantics can cause incorrect results for non-idempotent operations, while at-most-once semantics work correctly.


### Setup
1. Start server with simulated packet loss: `--loss 0.3`
2. Start client with matching settings: `--loss 0.3`


### Experiment 1: Deposit (Non-Idempotent) with At-Least-Once


**Steps:**
1. Create account with initial balance $100
2. Deposit $50 with at-least-once semantics and high loss rate
3. Observe retries and final balance


**Expected Result:**
If request is processed but reply is lost, client retries.
Server processes deposit again → Balance becomes $200 instead of $150.


### Experiment 2: Same Scenario with At-Most-Once


**Steps:**
1. Create account with initial balance $100
2. Deposit $50 with at-most-once semantics and high loss rate
3. Observe retries and final balance


**Expected Result:**
Duplicate requests return cached reply.
Balance correctly shows $150.


### Experiment 3: Bank Statement (Idempotent) with At-Least-Once


**Steps:**
1. Request bank statement with high loss rate
2. Observe multiple retries


**Expected Result:**
Operation is safe with at-least-once - multiple executions return same result.


### Expected Results Summary


| Operation | Semantics | Loss Rate | Expected Outcome |
|-----------|-----------|-----------|------------------|
| Deposit | At-Least-Once | 30% | Potential double-deposit (WRONG) |
| Deposit | At-Most-Once | 30% | Correct single deposit |
| Statement | At-Least-Once | 30% | Correct (idempotent) |
| Statement | At-Most-Once | 30% | Correct (cached) |
| Transfer | At-Least-Once | 30% | Potential double-transfer (WRONG) |
| Transfer | At-Most-Once | 30% | Correct single transfer |


## Status Codes


| Code | Constant | Description |
|------|----------|-------------|
| 0 | SUCCESS | Operation completed successfully |
| 1 | ERROR_ACCOUNT_NOT_FOUND | Account does not exist |
| 2 | ERROR_WRONG_PASSWORD | Password verification failed |
| 3 | ERROR_INSUFFICIENT_BALANCE | Not enough funds |
| 4 | ERROR_ACCOUNT_NAME_MISMATCH | Account belongs to different user |
| 5 | ERROR_INVALID_AMOUNT | Amount is zero or negative |
| 6 | ERROR_INVALID_CURRENCY | Currency type mismatch |
| 7 | ERROR_DUPLICATE_ACCOUNT | Account already exists |
| 8 | ERROR_INTERNAL_SERVER | Unexpected server error |
| 9 | ERROR_INVALID_REQUEST | Malformed request |
| 10 | ERROR_SAME_ACCOUNT_TRANSFER | Cannot transfer to same account |


## Monitoring/Callback Flow


```
┌──────────┐         ┌──────────────────┐         ┌──────────┐
│ Client A │         │     Server       │         │ Client B │
│(Monitor) │         │                  │         │(Active)  │
└────┬─────┘         └────────┬─────────┘         └────┬─────┘
    │                        │                        │
    │ MONITOR(60s)           │                        │
    │───────────────────────►│                        │
    │                        │ Register Client A      │
    │◄───────────────────────│ for 60 seconds         │
    │ ACK                    │                        │
    │                        │                        │
    │                        │◄───────────────────────│
    │                        │ DEPOSIT $100           │
    │                        │                        │
    │◄───────────────────────│ Process deposit        │
    │ CALLBACK: DEPOSIT      │───────────────────────►│
    │ Account #1000          │ Reply: new balance     │
    │ Balance: $600          │                        │
    │                        │                        │
    │ ... (more callbacks)   │                        │
    │                        │                        │
    │ (After 60 seconds)     │                        │
    │ Registration expires   │                        │
    │                        │                        │
└────┴────────────────────────┴────────────────────────┘
```






---


## Testing Coverage


This section documents the testing performed on the distributed banking system.


### Operation Test Results


| Operation | Status | Normal Case | Error Cases | Notes |
|-----------|--------|-------------|-------------|-------|
| Open Account | TESTED | Creates account with sequential number (1000, 1001, ...) | N/A | Initial balance recorded as first transaction |
| Close Account | TESTED | Returns final balance, removes account | Wrong password, Account not found | Account data deleted after close |
| Deposit | TESTED | Updates balance correctly, records transaction | Wrong password, Account not found, Invalid amount | Notifies monitoring clients |
| Withdraw | TESTED | Updates balance correctly, records transaction | Wrong password, Insufficient balance, Account not found | Shows available balance on error |
| Monitor | TESTED | Receives callbacks for account updates | N/A | Tested with 60-second duration |
| Bank Statement | TESTED | Shows complete transaction history with timestamps | Wrong password, Account not found | Idempotent - safe with at-least-once |
| Transfer | TESTED | Debits source, credits destination | Wrong password, Insufficient balance, Same account, Account not found | Non-idempotent - requires at-most-once |


### Error Handling Test Coverage


| Error Condition | Status | Error Message Displayed | Tested In |
|-----------------|--------|-------------------------|-----------|
| Wrong password | TESTED | "Incorrect password" | Deposit, Withdraw, Transfer, Bank Statement, Close Account |
| Insufficient balance | TESTED | "Insufficient balance. Available: X.X" | Withdraw, Transfer |
| Account not found | TESTED | "Account not found" | All operations except Open Account |
| Invalid amount | TESTED | "Invalid amount" | Deposit, Withdraw (zero or negative) |
| Same account transfer | TESTED | "Cannot transfer to same account" | Transfer |
| Account name mismatch | TESTED | "Account belongs to different user" | Close Account |


### Invocation Semantics Testing


#### At-Least-Once Semantics


| Test Case | Status | Observed Behavior |
|-----------|--------|-------------------|
| Normal operation (0% loss) | TESTED | All operations complete successfully |
| With 30% packet loss | TESTED | Retries occur, operations may execute multiple times |
| Duplicate deposit | TESTED | Balance incorrectly increased (double-deposit observed) |
| Duplicate transfer | TESTED | Money transferred twice (incorrect balances) |
| Idempotent operation (Bank Statement) | TESTED | Safe - multiple executions return same result |


#### At-Most-Once Semantics


| Test Case | Status | Observed Behavior |
|-----------|--------|-------------------|
| Normal operation (0% loss) | TESTED | All operations complete successfully |
| With 30% packet loss | TESTED | Retries occur, duplicate detection prevents re-execution |
| Duplicate request detection | TESTED | Server logs "Duplicate request detected. Returning cached reply." |
| Cached reply returned | TESTED | Client receives correct result despite retries |
| Reply history storage | TESTED | Server maintains Map<ClientID, Map<RequestID, CachedReply>> |


### Packet Loss Simulation Testing


| Loss Rate | Server Behavior | Client Behavior | Overall Result |
|-----------|-----------------|-----------------|----------------|
| 0% | No packet drops | No packet drops | All operations succeed on first attempt |
| 10% | Occasional drops | Occasional drops | Most operations succeed, some retries |
| 30% | Frequent drops | Frequent drops | Multiple retries common, all operations eventually succeed |
| 50% | Heavy drops | Heavy drops | Many retries needed, may exhaust retry limit |


#### Console Output Verification


| Log Message | Component | Verified |
|-------------|-----------|----------|
| `[Client] Sent request (attempt X, reqId=Y)` | Client | Yes |
| `[Client] Simulating REQUEST loss (attempt X)` | Client | Yes |
| `[Client] Timeout waiting for reply (attempt X)` | Client | Yes |
| `[Client] Retrying...` | Client | Yes |
| `[Client] Simulating REPLY loss` | Client | Yes |
| `[Client] Failed after X attempts` | Client | Yes |
| `[Server] Received: <message> from <address>` | Server | Yes |
| `[Server] Simulating REQUEST loss (packet dropped)` | Server | Yes |
| `[Server] Simulating REPLY loss (packet dropped)` | Server | Yes |
| `[Server] Duplicate request detected. Returning cached reply.` | Server | Yes |
| `[Server] Sent reply: status=X` | Server | Yes |
| `[Server] Sent callback to <address>` | Server | Yes |


### Edge Cases and Boundary Conditions


| Test Case | Status | Result |
|-----------|--------|--------|
| Empty account name | NOT TESTED | Should be validated |
| Very long account name | NOT TESTED | May cause buffer overflow |
| Password exactly 16 characters | TESTED | Works correctly |
| Password longer than 16 characters | NOT TESTED | Should be truncated or rejected |
| Zero initial balance | TESTED | Account created with 0 balance |
| Negative initial balance | NOT TESTED | Should be rejected |
| Very large balance (overflow) | NOT TESTED | May cause float precision issues |
| Maximum concurrent clients | NOT TESTED | Server handles sequentially |
| Monitor expiration | TESTED | Client stops receiving callbacks after interval |
| Multiple monitors on same account | NOT TESTED | Should all receive callbacks |


### Network Conditions Testing


| Condition | Status | Notes |
|-----------|--------|-------|
| Localhost communication | TESTED | Primary testing environment |
| Remote server connection | NOT TESTED | Should work with correct host/port |
| High latency | NOT TESTED | May require increased timeout |
| Network disconnection | NOT TESTED | Client should timeout and retry |


### Known Issues


1. **Float Precision**: Currency amounts use `float` type, which may cause precision issues for very large amounts or many decimal places. Consider using `BigDecimal` for production.


2. **Reply History Growth**: The at-most-once reply history grows indefinitely. In production, implement a cleanup mechanism based on time or size limits.


3. **No Persistence**: All account data is stored in memory and lost when the server restarts. Consider adding file-based or database persistence.


4. **Single-Threaded Server**: The server processes requests sequentially. For high concurrency, consider multi-threading.


### Test Environment


- **Java Version**: Java 17
- **Operating System**: macOS (Darwin)
- **Network**: Localhost (127.0.0.1)
- **Test Date**: 2026-02-06


### Recommended Additional Testing


1. **Stress Testing**: Run multiple clients simultaneously with high request rates
2. **Long-Running Test**: Run server for extended period to check for memory leaks
3. **Cross-Platform Testing**: Verify on Windows and Linux
4. **Network Testing**: Test with actual network latency between machines
5. **Boundary Testing**: Test all input validation edge cases






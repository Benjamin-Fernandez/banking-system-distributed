# Distributed Banking System


A distributed banking system implemented in Java using UDP socket programming. This project demonstrates client-server architecture with custom marshalling/unmarshalling and two invocation semantics: at-least-once and at-most-once.


## Table of Contents


1. [Features](#features)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Compilation](#compilation)
5. [Command Reference](#command-reference)
6. [Usage Examples](#usage-examples)
7. [Operations](#operations)
8. [Currency](#currency)
9. [Technical Documentation](#technical-documentation)


---


## Features


- **Account Management**: Open and close bank accounts with password protection
- **Transactions**: Deposit, withdraw, and transfer money between accounts
- **Bank Statements**: Generate transaction history (idempotent operation)
- **Monitoring**: Real-time callback notifications for account updates
- **Fault Tolerance**: Configurable timeouts, retries, and simulated packet loss
- **Invocation Semantics**: Both at-least-once and at-most-once semantics
- **Custom Protocol**: UDP-based communication with custom marshalling (no Java serialization)


## Prerequisites


- Java 17 or higher
- No external dependencies required


## Project Structure


```
dist/
├── README.md              # This file
├── DEVELOPMENT.md         # Technical documentation
├── SRS.txt                # Software Requirements Specification
├── src/
│   ├── common/            # Shared classes
│   │   ├── BankAccount.java
│   │   ├── CurrencyType.java
│   │   ├── Marshaller.java
│   │   ├── Message.java
│   │   ├── OperationType.java
│   │   └── StatusCode.java
│   ├── server/            # Server-side code
│   │   ├── BankServer.java
│   │   ├── BankService.java
│   │   ├── RequestHandler.java
│   │   ├── ServiceResult.java
│   │   └── UpdateCallback.java
│   └── client/            # Client-side code
│       └── BankClient.java
└── bin/                   # Compiled classes (after compilation)
```


## Compilation


From the project root directory:


```bash
# Create output directory and compile all Java files
mkdir -p bin
javac -d bin src/common/*.java src/server/*.java src/client/*.java
```


To verify compilation succeeded:


```bash
ls bin/common/ bin/server/ bin/client/
```


Expected output shows `.class` files for all components.


---


## Command Reference


### Server Command-Line Options


```
Usage: java -cp bin server.BankServer [options]


Options:
 -p, --port <port>       Server port number (default: 8888)
 -s, --semantics <type>  Invocation semantics (default: atleast)
                         atleast - at-least-once semantics
                         atmost  - at-most-once semantics
 -l, --loss <rate>       Simulated packet loss rate 0.0-1.0 (default: 0.0)
 -h, --help              Show help message
```


### Client Command-Line Options


```
Usage: java -cp bin client.BankClient [options]


Options:
 -h, --host <host>       Server hostname (default: localhost)
 -p, --port <port>       Server port number (default: 8888)
 -s, --semantics <type>  Invocation semantics (default: atleast)
                         atleast - at-least-once semantics
                         atmost  - at-most-once semantics
 -t, --timeout <ms>      Request timeout in milliseconds (default: 3000)
 -r, --retries <count>   Maximum retry attempts (default: 3)
 -l, --loss <rate>       Simulated packet loss rate 0.0-1.0 (default: 0.0)
 --help                  Show help message
```


---


## Usage Examples


### Basic Server Startup


```bash
# Start server with default settings (port 8888, at-least-once)
java -cp bin server.BankServer


# Start server with at-most-once semantics (recommended for production)
java -cp bin server.BankServer --semantics atmost


# Start server on a custom port
java -cp bin server.BankServer --port 9999


# Start server with all options specified
java -cp bin server.BankServer --port 8888 --semantics atmost --loss 0.0
```


**Expected Server Output:**
```
========================================
   Distributed Banking Server
========================================
Port: 8888
Semantics: AT-MOST-ONCE
Simulated Loss Rate: 0.0%
========================================
Server is running. Waiting for requests...
```


### Basic Client Startup


```bash
# Connect to local server with default settings
java -cp bin client.BankClient


# Connect with at-most-once semantics (must match server)
java -cp bin client.BankClient --semantics atmost


# Connect to a remote server
java -cp bin client.BankClient --host 192.168.1.100 --port 8888


# Connect with custom timeout and retry settings
java -cp bin client.BankClient --timeout 5000 --retries 5


# Connect with all options specified
java -cp bin client.BankClient --host localhost --port 8888 --semantics atmost --timeout 3000 --retries 3
```


**Expected Client Output:**
```
========================================
   Distributed Banking Client
========================================
Server: localhost:8888
Client ID: 1234567890
Semantics: AT-MOST-ONCE
Timeout: 3000ms, Max Retries: 3
========================================


--- Banking Operations ---
1. Open Account
2. Close Account
3. Deposit
4. Withdraw
5. Monitor Updates
6. Bank Statement
7. Transfer
0. Exit
Enter choice:
```


---


### End-to-End Workflow Examples


#### Scenario 1: Normal Operation (No Packet Loss)


**Terminal 1 - Start Server:**
```bash
java -cp bin server.BankServer --semantics atmost
```


**Terminal 2 - Start Client:**
```bash
java -cp bin client.BankClient --semantics atmost
```


**Client Interaction - Create Account:**
```
Enter choice: 1


--- Open New Account ---
Enter your name: John Doe
Enter password (max 16 chars): secret123
Enter initial balance: 1000
[Client] Sent request (attempt 1, reqId=1)


*** SUCCESS! Account created ***
Your account number: 1000
```


**Client Interaction - Deposit Money:**
```
Enter choice: 3


--- Deposit Money ---
Enter your name: John Doe
Enter account number: 1000
Enter password: secret123
Enter amount to deposit: 500
[Client] Sent request (attempt 1, reqId=2)


*** SUCCESS! Deposit completed ***
New balance: 1500.0
```


**Client Interaction - View Bank Statement:**
```
Enter choice: 6


--- Bank Statement ---
Enter your name: John Doe
Enter account number: 1000
Enter password: secret123
[Client] Sent request (attempt 1, reqId=3)


=== Bank Statement for Account #1000 ===
Holder: John Doe
Currency: US Dollar
Current Balance: 1500.00


--- Transaction History ---
[2026-02-06 10:30:15] Initial deposit: 1000.00 -> Balance: 1000.00
[2026-02-06 10:31:22] Deposit: 500.00 -> Balance: 1500.00
```


**Server Console Output:**
```
[Server] Received: Message[reqId=1, op=OPEN_ACCOUNT, clientId=1234567890] from 127.0.0.1:54321
[Server] Sent reply: status=0
[Server] Received: Message[reqId=2, op=DEPOSIT, clientId=1234567890] from 127.0.0.1:54321
[Server] Sent reply: status=0
[Server] Received: Message[reqId=3, op=BANK_STATEMENT, clientId=1234567890] from 127.0.0.1:54321
[Server] Sent reply: status=0
```


---


#### Scenario 2: At-Most-Once with Packet Loss (Correct Behavior)


This scenario demonstrates that at-most-once semantics correctly handles packet loss without duplicate execution.


**Terminal 1 - Start Server with 30% Loss:**
```bash
java -cp bin server.BankServer --semantics atmost --loss 0.3
```


**Terminal 2 - Start Client with 30% Loss:**
```bash
java -cp bin client.BankClient --semantics atmost --loss 0.3 --retries 10
```


**Client Console Output (with retries):**
```
Enter choice: 3


--- Deposit Money ---
Enter your name: John Doe
Enter account number: 1000
Enter password: secret123
Enter amount to deposit: 100
[Client] Simulating REQUEST loss (attempt 1)
[Client] Timeout waiting for reply (attempt 1)
[Client] Retrying...
[Client] Sent request (attempt 2, reqId=4)
[Client] Simulating REPLY loss
[Client] Timeout waiting for reply (attempt 2)
[Client] Retrying...
[Client] Sent request (attempt 3, reqId=4)


*** SUCCESS! Deposit completed ***
New balance: 1600.0
```


**Server Console Output:**
```
[Server] Simulating REQUEST loss (packet dropped)
[Server] Received: Message[reqId=4, op=DEPOSIT, clientId=1234567890] from 127.0.0.1:54321
[Server] Simulating REPLY loss (packet dropped)
[Server] Received: Message[reqId=4, op=DEPOSIT, clientId=1234567890] from 127.0.0.1:54321
[Server] Duplicate request detected. Returning cached reply.
[Server] Sent reply: status=0
```


Note: The deposit executes only once despite multiple retries. The server detects the duplicate request and returns the cached reply.


---


#### Scenario 3: At-Least-Once with Packet Loss (Demonstrates Problem)


This scenario demonstrates the risk of duplicate execution with at-least-once semantics.


**Terminal 1 - Start Server with 30% Loss:**
```bash
java -cp bin server.BankServer --semantics atleast --loss 0.3
```


**Terminal 2 - Start Client with 30% Loss:**
```bash
java -cp bin client.BankClient --semantics atleast --loss 0.3 --retries 10
```


**Potential Problem Output:**
```
Enter choice: 3


--- Deposit Money ---
Enter your name: John Doe
Enter account number: 1000
Enter password: secret123
Enter amount to deposit: 100
[Client] Sent request (attempt 1, reqId=5)
[Client] Timeout waiting for reply (attempt 1)
[Client] Retrying...
[Client] Sent request (attempt 2, reqId=5)


*** SUCCESS! Deposit completed ***
New balance: 1800.0   <-- WRONG! Should be 1700.0 (deposit executed twice)
```


**Server Console Output:**
```
[Server] Received: Message[reqId=5, op=DEPOSIT, clientId=1234567890] from 127.0.0.1:54321
[Server] Simulating REPLY loss (packet dropped)
[Server] Received: Message[reqId=5, op=DEPOSIT, clientId=1234567890] from 127.0.0.1:54321
[Server] Sent reply: status=0
```


Note: Without duplicate detection, the deposit executes twice, resulting in an incorrect balance.


---


#### Scenario 4: Monitoring with Multiple Clients


**Terminal 1 - Start Server:**
```bash
java -cp bin server.BankServer --semantics atmost
```


**Terminal 2 - Start Monitoring Client:**
```bash
java -cp bin client.BankClient --semantics atmost
```


```
Enter choice: 5


--- Monitor Updates ---
Enter monitoring duration (seconds): 60
[Client] Sent request (attempt 1, reqId=1)


*** Monitoring started for 60 seconds ***
Waiting for account updates...
```


**Terminal 3 - Start Active Client:**
```bash
java -cp bin client.BankClient --semantics atmost
```


```
Enter choice: 3


--- Deposit Money ---
Enter your name: John Doe
Enter account number: 1000
Enter password: secret123
Enter amount to deposit: 250
[Client] Sent request (attempt 1, reqId=1)


*** SUCCESS! Deposit completed ***
New balance: 1750.0
```


**Monitoring Client Receives Callback:**
```
*** Account Update Received ***
Update Type: DEPOSIT
Account Number: 1000
Holder: John Doe
New Balance: 1750.0
```


---


### Testing Error Handling


**Wrong Password:**
```
Enter choice: 3


--- Deposit Money ---
Enter your name: John Doe
Enter account number: 1000
Enter password: wrongpassword
Enter amount to deposit: 100
[Client] Sent request (attempt 1, reqId=6)


*** ERROR ***
Status: Incorrect password
Details: Incorrect password
```


**Insufficient Balance:**
```
Enter choice: 4


--- Withdraw Money ---
Enter your name: John Doe
Enter account number: 1000
Enter password: secret123
Enter amount to withdraw: 999999
[Client] Sent request (attempt 1, reqId=7)


*** ERROR ***
Status: Insufficient balance
Details: Insufficient balance. Available: 1750.0
```


**Account Not Found:**
```
Enter choice: 3


--- Deposit Money ---
Enter your name: John Doe
Enter account number: 9999
Enter password: secret123
Enter amount to deposit: 100
[Client] Sent request (attempt 1, reqId=8)


*** ERROR ***
Status: Account not found
Details: Account not found
```


---


## Operations


| Operation | Description | Idempotent |
|-----------|-------------|------------|
| Open Account | Create a new bank account with initial balance | No |
| Close Account | Close an existing account and return final balance | No |
| Deposit | Add money to an account | No |
| Withdraw | Remove money from an account | No |
| Monitor | Watch for real-time account updates (callbacks) | Yes |
| Bank Statement | View transaction history | Yes |
| Transfer | Move money between two accounts | No |


## Currency


All accounts use **US Dollar (USD)** as the default currency. Currency selection has been removed from the client interface for simplicity.


## Technical Documentation


See [DEVELOPMENT.md](DEVELOPMENT.md) for detailed technical documentation including:


- System architecture diagrams
- Complete request-response flow documentation
- Message format specifications (header and payload structures)
- Custom marshalling/unmarshalling implementation details
- Invocation semantics implementation (at-least-once vs at-most-once)
- Duplicate detection mechanism
- Experiment design and expected results
- Status codes and error handling
- Monitoring/callback flow








#

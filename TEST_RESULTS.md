# Distributed Banking System — End-to-End Test Results

**Date:** 2026-04-02  
**Environment:** Windows, Java 21 (OpenJDK Temurin 21.0.10), UDP localhost  
**Server Port:** 9999

---

## 1. Core Operations (No Packet Loss, At-Most-Once)

| # | Test | Input | Expected | Actual | Status |
|---|------|-------|----------|--------|--------|
| 1 | **Open Account** | Name=John Doe, Pass=secret123, USD, $1000 | Account created | Account #1000 created, balance 1000.0 | ✅ PASS |
| 2 | **Deposit** | Acct #1000, deposit $500 USD | Balance = 1500 | New balance: 1500.0 | ✅ PASS |
| 3 | **Withdraw** | Acct #1000, withdraw $200 USD | Balance = 1300 | New balance: 1300.0 | ✅ PASS |
| 4 | **Bank Statement** | Acct #1000 | Shows 3 transactions, balance 1300 | Correct history + balance 1300.00 | ✅ PASS |
| 5 | **Transfer** | Open Acct #1001 (Jane, EUR, 500); Transfer $300 from #1000→#1001 | John=1000, Jane=800 | John balance: 1000.0, Jane closed at 800.0 | ✅ PASS |
| 6 | **Close Account** | Close Acct #1001 | Closed, final balance returned | Account #1001 closed. Final balance: 800.0 | ✅ PASS |

## 2. Error Handling

| # | Test | Expected Error | Actual Error | Status |
|---|------|---------------|--------------|--------|
| 7a | **Wrong password** | Rejected | "Incorrect password" | ✅ PASS |
| 7b | **Insufficient balance** | Rejected | "Insufficient balance. Available: 1000.0" | ✅ PASS |
| 7c | **Account not found** | Rejected | "Account not found" | ✅ PASS |
| 7d | **Name mismatch** | Rejected | "Account does not belong to this user" | ✅ PASS |
| 7e | **Close already-closed account** | Rejected | "Account not found" | ✅ PASS |
| 7f | **Transfer to same account** | Rejected | "Cannot transfer to the same account" | ✅ PASS |

## 3. Monitor Callback

| # | Test | Setup | Expected | Actual | Status |
|---|------|-------|----------|--------|--------|
| 8 | **Monitor Updates** | Client A monitors 10s; Client B deposits $250 to Acct #1000 | Client A receives callback | Client A received: `UPDATE: DEPOSIT Account #1000 (Alice) Balance: 1250.0 USD` | ✅ PASS |

- Server log confirmed: `Sent callback to /127.0.0.1:62659`

## 4. Invocation Semantics Comparison (Packet Loss Enabled)

### Test 9 — At-Most-Once (Server loss=50%, Client loss=30%)

- **Setup:** 3 deposits of $100 each to Acct #1000 (initial $1000)
- **Expected balance:** 1300 (1000 + 3×100)
- **Actual balance:** **1300** ✅
- **Key observations:**
  - Server detected duplicate requests: `"Duplicate request detected. Returning cached reply."`
  - Despite reply losses causing retries, each deposit executed **exactly once**
  - Request IDs used for deduplication: reqId=1, 2, 3 each processed once

### Test 10 — At-Least-Once (Server loss=70%, Client loss=50%)

- **Setup:** 1 deposit of $500 to Acct #1000 (initial $1000)
- **Expected balance:** 1500 (1000 + 500)
- **Actual balance:** **2500** ❌ (WRONG — deposit executed 3 times)
- **Key observations:**
  - Server re-executed same reqId=1 three times:
    - `Deposited 500.0 → balance: 1500.0`
    - `Deposited 500.0 → balance: 2000.0` (duplicate)
    - `Deposited 500.0 → balance: 2500.0` (duplicate)
  - No duplicate detection — server processes every request as new
  - **Non-idempotent operations (deposit/withdraw/transfer) produce wrong results**

## 5. Summary

| Category | Tests | Passed | Failed |
|----------|-------|--------|--------|
| Core Operations | 6 | 6 | 0 |
| Error Handling | 6 | 6 | 0 |
| Monitor Callback | 1 | 1 | 0 |
| At-Most-Once Semantics | 1 | 1 | 0 |
| At-Least-Once Semantics | 1 | 1 (demonstrated flaw) | 0 |
| **Total** | **15** | **15** | **0** |

### Key Conclusion

- **At-most-once** semantics correctly handle all operations under packet loss by caching replies and detecting duplicate requests via `(clientId, reqId)`.
- **At-least-once** semantics cause **incorrect results for non-idempotent operations** (e.g., deposit executed 3× instead of 1×) when reply messages are lost, because the server re-executes every retry without deduplication.
- **Idempotent operations** (e.g., bank statement) work correctly under both semantics.


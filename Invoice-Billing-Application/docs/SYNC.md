# SwiftLab Synchronization Engine Architecture

## 1. Overview

The SwiftLab Synchronization Engine enables seamless multi-device collaboration across Windows desktops and future Android mobile clients while preserving local-first autonomy.

```
                      +-----------------------------+
                      |   LOCAL CLIENT TRANSACTION  |
                      +-----------------------------+
                                     |
                                     v
                      +-----------------------------+
                      |  1. Local Database Commit   |
                      |  2. Queue SyncQueueItem     |
                      |     (Status: PENDING)       |
                      +-----------------------------+
                                     |
             +-----------------------+-----------------------+
             |                                               |
    [Offline / Connection Down]                     [Connection Active]
             |                                               |
             v                                               v
    Queue items remain safe                         +-----------------------------+
    in local H2 database                            |  Mark Status: SYNCING       |
    User continues normal work                      |  Batch to Central Cloud API |
                                                    +-----------------------------+
                                                                   |
                                              +--------------------+--------------------+
                                              |                                         |
                                         [Success]                                  [Failure]
                                              |                                         |
                                              v                                         v
                                    Status: SYNCED                            Status: FAILED
                                    Record AppliedSyncKey                     Increment retryCount
                                                                              Exponential backoff
```

---

## 2. Sync Queue Entity & Lifecycle

Every state modification produced while in `CLOUD_SYNC` mode generates an entry in `sync_queue`:
- `id`: Auto-increment local ID.
- `organizationId`: Multi-tenant ownership.
- `deviceId`: Stable UUID of the originating device.
- `entityType`: Target entity class (`Product`, `Sale`, `Purchase`, `Payment`, `Customer`, etc.).
- `entityId`: Local ID of the entity.
- `operation`: `CREATE`, `UPDATE`, `DELETE`.
- `payload`: Serialized JSON snapshot of the entity.
- `status`:
  - `PENDING`: Waiting to be transmitted.
  - `SYNCING`: Currently in-flight to central cloud.
  - `SYNCED`: Successfully processed and confirmed by cloud.
  - `FAILED`: Encountered a transient network or server error; scheduled for retry.
- `retryCount`: Number of transmission attempts.
- `errorMessage`: Diagnostic information if last attempt failed.

---

## 3. Strict Idempotency Architecture

Synchronization across distributed devices is prone to duplicated network packets and retry replays. To prevent duplicate sales, stock movements, invoices, or payments:
1. Each synchronization payload includes an idempotency key:
   $$\text{SyncKey} = \text{deviceId} + ":" + \text{entityType} + ":" + \text{entityId} + ":" + \text{operation}$$
2. The receiving central server queries the `applied_sync_keys` table.
3. If the key already exists:
   - The operation is marked as duplicate and skipped immediately.
   - The server returns an HTTP 200/Success to allow the client queue to mark the item as `SYNCED`.
4. If the key is new:
   - The operation is executed inside a database transaction.
   - The key is recorded in `applied_sync_keys` atomically with the payload execution.

---

## 4. Offline Resilience

- Operations are **never** blocked waiting for an internet connection.
- Transactions are committed locally first, allowing cashier and warehouse workflows to proceed at full speed without network latency.
- When network connectivity is re-established, the background scheduler picks up `PENDING` items in FIFO order and flushes them to the cloud.
- If an individual item fails due to validation or server downtime, it is marked `FAILED` without blocking the local user.

# SwiftLab Distributed Conflict Resolution Strategy

## 1. The Conflict Challenge

In a multi-device distributed system operating offline, concurrent mutations to the same records are inevitable. SwiftLab classifies entities into two distinct categories with tailored conflict resolution strategies:
1. **Additive / Movement-Based Ledger Data** (Inventory, Sales, Payments, Stock Movements)
2. **Master / State-Based Data** (Products, Customers, Suppliers, Warehouses, Settings)

---

## 2. Movement-Based Inventory Conflict Handling

### Why Last-Write-Wins (LWW) Fails for Stock
If Device A sells 10 units offline (reading stock 100 $\rightarrow$ 90) and Device B sells 5 units offline (reading stock 100 $\rightarrow$ 95), using naive Last-Write-Wins would overwrite the central stock with either 90 or 95. In either case, 5 to 10 units of physical sales are completely lost from the ledger.

### SwiftLab Additive Ledger Solution
SwiftLab **never** synchronizes absolute quantity values (e.g. "set stock to 90"). Instead, it synchronizes atomic `StockMovement` records:
- Device A uploads: `StockMovement(type=SALE, delta=-10, ref=Sale-A)`
- Device B uploads: `StockMovement(type=SALE, delta=-5, ref=Sale-B)`

When central cloud receives both:
1. `Movement A` is applied: $100 - 10 = 90$
2. `Movement B` is applied: $90 - 5 = 85$
3. Both movements are recorded in the central audit ledger with full traceability.
4. The final stock is accurately calculated as $85$.

```
Device A (Offline):   Opening 100  -->  Sale -10 (Local: 90)
Device B (Offline):   Opening 100  -->  Sale -5  (Local: 95)
                                          |
                                    [Sync to Cloud]
                                          |
Central Server:       100 - 10 - 5 = 85 (Both movements preserved!)
```

---

## 3. Master Data Conflict Handling

For entity metadata updates (such as product name, customer address, phone number):
1. **Timestamp / Version-Based Determinism:**
   - Each entity maintains an `updatedAt` timestamp.
   - Updates are compared: if an incoming modification has an `updatedAt` later than the server's current timestamp, the update is accepted.
   - If an incoming modification is older than an already-applied edit, the update is rejected or merged per field.
2. **Soft-Delete Propagation:**
   - Deletions set `deleted = true` and `deletedAt = timestamp`.
   - A deletion takes precedence over metadata updates occurring before `deletedAt`.
   - Records are never hard-deleted immediately, ensuring all distributed nodes learn of the tombstone.

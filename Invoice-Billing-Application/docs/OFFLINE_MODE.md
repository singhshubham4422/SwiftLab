# SwiftLab Local-Only & Offline Operations Guide

## 1. Operating Modes

SwiftLab supports two mutually exclusive modes:
1. `LOCAL_ONLY` Mode (Default for standalone desktop installations)
2. `CLOUD_SYNC` Mode (For multi-terminal / multi-device organizations)

---

## 2. Invariants of `LOCAL_ONLY` Mode

When running in `LOCAL_ONLY`:
- **Absolute Local Storage:** All catalog, customer, invoice, inventory, and payment records are saved exclusively to the local H2 file database (`./data/invoicedb.mv.db`).
- **No Cloud Telemetry or Calls:** No HTTP requests are dispatched to Supabase or Render backend servers.
- **ZERO SILENT UPLOAD GUARANTEE:**
  > [!IMPORTANT]
  > Restoring internet connectivity will **NEVER** automatically trigger an upload or migration of data from a `LOCAL_ONLY` installation. The data remains strictly on your device until the user explicitly and intentionally switches modes.

---

## 3. Safe Mode Switching Protocol

Switching from `LOCAL_ONLY` to `CLOUD_SYNC` is a deliberate administrative action:
1. **Explicit Confirmation Modal:**
   The user must navigate to **Settings $\rightarrow$ Synchronization**, click **Enable Cloud Sync**, and check the confirmation checkbox:
   > *"Local Only mode keeps your data on this device. Enabling Cloud Sync will allow selected organization data to be synchronized with the cloud."*
2. **Backend Validation:**
   The endpoint `/sync/mode` or `/api/sync/mode` requires `confirmed: true`. If `confirmed` is absent or false, the server rejects the request with HTTP 400 Bad Request.
3. **Controlled Migration:**
   Upon switching to `CLOUD_SYNC`:
   - An initial sync queue batch is compiled for existing records.
   - The user is notified that synchronization is active.
   - Switching back to `LOCAL_ONLY` immediately stops synchronization and disarms the sync queue.

---

## 4. Device Identification & Privacy

- Every local terminal generates a stable, random UUID upon first launch: `WIN-<UUID>` or `ANDROID-<UUID>`.
- The system **never** reads hardware MAC addresses, motherboard serials, or invasive hardware identifiers.
- The device UUID is stored locally in `app_runtime` and used solely for sync queue attribution and audit logging.

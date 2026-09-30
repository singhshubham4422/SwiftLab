-- ==============================================================================
-- SwiftLab Inventory & Billing Platform - Synchronization Schema & Idempotency
-- ==============================================================================

-- Sync Queue: Queued operations awaiting push to central cloud (or received offline)
CREATE TABLE IF NOT EXISTS sync_queue (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    device_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    operation VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SYNCING, SYNCED, FAILED
    retry_count INT DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Applied Sync Keys: Enforces strict idempotency across all multi-device sync requests
-- Key format: deviceId:entityType:entityId:operation:timestampOrHash
CREATE TABLE IF NOT EXISTS applied_sync_keys (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    sync_key VARCHAR(255) UNIQUE NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    operation VARCHAR(50) NOT NULL,
    applied_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Performance indexes for queue processing & idempotency checks
CREATE INDEX IF NOT EXISTS idx_sync_queue_poll 
    ON sync_queue(organization_id, status, retry_count, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_applied_sync_lookup 
    ON applied_sync_keys(organization_id, sync_key);

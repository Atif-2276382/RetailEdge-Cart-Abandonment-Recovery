## RetailEdge Domain Rules

### Inventory Sync Module
- Inventory updates must be validated before persistence.
- Stock levels cannot become negative.
- Product lookups must be scoped to authorized users.

### Cart Abandonment Recovery System
- Carts are considered abandoned after 30 minutes of inactivity.
- Recovery tracking must support a 24-hour recovery window.
- Recovery analytics endpoints must use cursor-based pagination.
- Abandonment scans must support configurable batch sizes.
- Batch processing must be restartable from the last processed cursor.

### Authorization
- Admin endpoints must enforce role-based authorization.
- Non-admin users must receive HTTP 403 responses for restricted operations.

### Database Design
- Declare indexes on frequently searched fields.
- Define indexes with purpose and query pattern documentation.

### Implementation Standards
- Validate all request payloads before persistence and reject invalid data with clear domain exceptions.
- Enforce authorization checks at the service layer for owner-scoped records and admin-only actions.
- Use JWT subject and authenticated principal information as the source of truth for owner identity; do not trust client-supplied owner IDs.
- Keep cart totals as `BigDecimal` and disallow negative values in all create/update flows.
- Ensure stock changes are validated before saving and never allow negative inventory.
- Keep repository lookups scoped to the authenticated owner whenever returning or mutating private resources.
- Prefer transactional service methods for write operations and recovery state transitions.
- Treat abandoned carts as inactive after 30 minutes of no activity and enforce the 24-hour recovery window.
- Use cursor-based pagination for recovery analytics and bounded batch sizes for periodic scans.
- Design scans to be restartable using the last processed cursor and avoid unbounded reads.
- Use `OffsetDateTime` for timestamps and keep UTC as the default timezone.
- Keep DTOs explicit and do not expose JPA entities directly through API responses.
- Preserve the existing API contract and response shapes when extending functionality.
- Add or update tests alongside behavior changes, especially for authorization, validation, recovery timing, and pagination edge cases.
- Keep comments and documentation aligned with actual business rules and database indexes.
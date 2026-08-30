# Frame Inventory backend

## Behaviour notes

- Frame identifiers use the MariaDB database's case-insensitive collation. Identifiers that differ only by letter case are treated as the same identifier for uniqueness and lookup. This is an accepted take-home constraint; no collation migration is applied.
- `createdDate` and `modifiedDate` are server-owned. CSV `created_date` and `modified_date` values are intentionally ignored when a frame is imported.

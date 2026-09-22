# RetailEdge Architecture and Flow

## System Architecture

```mermaid
flowchart TB
    Client[Client / Admin UI]

    subgraph Security[Security Layer]
        JWT[JWT Validation]
        Roles[Role and Ownership Checks]
    end

    subgraph API[Controller Layer]
        CartController[Cart Abandonment Controller]
        InventoryController[Inventory Controller]
        ExceptionHandler[Global Exception Handler]
    end

    subgraph Business[Service Layer]
        CartService[Cart Abandonment Service]
        InventoryService[Inventory Service]
        Scanner[Abandonment Scanner]
    end

    subgraph Persistence[Repository and ORM Layer]
        CartRepository[Cart Repository]
        InventoryRepository[Inventory Repository]
        JPA[Spring Data JPA / Hibernate]
    end

    PostgreSQL[(PostgreSQL)]

    Client --> JWT
    JWT --> Roles
    Roles --> CartController
    Roles --> InventoryController

    CartController --> CartService
    InventoryController --> InventoryService
    Scanner --> CartService

    CartService --> CartRepository
    InventoryService --> InventoryRepository
    CartRepository --> JPA
    InventoryRepository --> JPA
    JPA --> PostgreSQL

    CartController -. errors .-> ExceptionHandler
    InventoryController -. errors .-> ExceptionHandler
    CartService -. errors .-> ExceptionHandler
    InventoryService -. errors .-> ExceptionHandler
```

## Abandonment Detection Flow

```mermaid
sequenceDiagram
    participant Scheduler
    participant Scanner as Abandonment Scanner
    participant Service as Cart Service
    participant Repo as Cart Repository
    participant DB as PostgreSQL

    Scheduler->>Scanner: Start scan(batchSize, cursor)
    Scanner->>Service: scanAbandoned(cursor, batchSize)
    Service->>Repo: Find ACTIVE carts older than 30 minutes
    Repo->>DB: Execute cursor-based query
    DB-->>Repo: Ordered cart batch
    Repo-->>Service: Cart records

    loop Each cart
        Service->>Service: Mark ABANDONED
        Service->>Service: Set recovery deadline to +24 hours
        Service->>Repo: Save status transition
        Repo->>DB: Persist update
    end

    Service-->>Scanner: processed count and next cursor
    Scanner-->>Scheduler: Resume cursor when more records exist
```

## Recovery Flow

```mermaid
sequenceDiagram
    participant Client
    participant JWT as JWT Security
    participant Controller
    participant Service as Cart Service
    participant Repo as Cart Repository
    participant DB as PostgreSQL

    Client->>JWT: Submit recovery request
    JWT->>JWT: Validate signature, issuer, audience, exp, iat, nbf
    JWT-->>Controller: Authenticated subject

    Controller->>Service: Start or complete recovery
    Service->>Repo: Find cart by ID and authenticated owner ID
    Repo->>DB: Execute owner-scoped lookup
    DB-->>Repo: Cart or empty result
    Repo-->>Service: Authorized cart

    Service->>Service: Verify ABANDONED status
    Service->>Service: Verify recovery deadline
    Service->>Repo: Persist recovery status
    Repo->>DB: Save recovery state
    Service-->>Controller: Response DTO
    Controller-->>Client: Recovery response
```

## Cursor Pagination and Restartable Scans

```mermaid
flowchart LR
    Start[Initial request<br/>cursor=null] --> Query[Query records ordered by ID]
    Query --> Limit[Apply bounded batch size]
    Limit --> Result[Return records and scan result]
    Result --> More{More records?}
    More -- No --> Done[nextCursor=null]
    More -- Yes --> Next[nextCursor=last processed ID]
    Next --> Resume[Restart with next cursor]
    Resume --> Query
```

## Data Model

```mermaid
erDiagram
    CART_ABANDONMENT {
        UUID id PK
        UUID owner_id
        string customer_email
        decimal cart_total
        datetime last_activity_at
        datetime abandoned_at
        datetime recovery_deadline
        string status
        string recovery_status
        datetime recovered_at
        long version
    }

    INVENTORY {
        UUID id PK
        UUID product_id
        UUID owner_id
        int stock
        datetime updated_at
        long version
    }
```

## Security and Data Rules

- JWT subject determines the resource owner; clients cannot submit an arbitrary owner ID.
- JWT signature, issuer, audience, expiration, issued-at, and not-before claims are validated.
- Repository queries are owner-scoped to prevent Broken Object Level Authorization.
- Create, stock mutation, delete, and administrative scan operations require the appropriate role.
- DTOs whitelist request and response fields; entities are not exposed directly.
- Carts become abandoned after 30 minutes of inactivity.
- Recovery is allowed only within the 24-hour recovery window.
- Cursor pagination and bounded batch sizes prevent unbounded database reads.
- PostgreSQL persistence uses Spring Data JPA and Hibernate.
- Currency values use `BigDecimal`.

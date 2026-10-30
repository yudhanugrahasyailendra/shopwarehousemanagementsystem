# Inventory API

REST backend for managing a shop's warehouse inventory: **items**, their **variants** (size, colour, ...),
**prices**, **stock levels**, and **sales** that can never sell more than what is in stock.

Built with Java 17, Spring Boot 3.3, Spring Data JPA and an in-memory H2 database.

## Quick start

**Requirements:** JDK 17 or newer, and Maven 3.9+.

```bash
mvn spring-boot:run
```

The API is now on `http://localhost:8080` and starts with sample data already loaded.

| What | Where |
|---|---|
| Interactive API docs (Swagger UI) | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 console (JDBC URL `jdbc:h2:mem:inventory`, user `sa`, empty password) | http://localhost:8080/h2-console |

Run the tests (unit, API and concurrency tests):

```bash
mvn verify
```

Start with an empty database instead of the sample data: `mvn spring-boot:run -Dspring-boot.run.arguments=--app.seed.enabled=false`

> The database lives in memory: data is reset every time the application restarts.

## Sample data

| Item | Variant ids | Notes |
|---|---|---|
| Classic T-Shirt (base price 19.99) | 1-4 (`TSHIRT-BLK-S/M/L`, `TSHIRT-WHT-M`) | `TSHIRT-WHT-M` has its own price (21.99) and only 3 units |
| Running Sneakers (base price 89.00) | 5-7 (`SNKR-RED-42`, `SNKR-RED-43`, `SNKR-BLU-42`) | `SNKR-RED-43` (id 6) is **sold out**: use it to see the out-of-stock rule |
| Ceramic Mug (base price 7.50) | 8 | No variants were given, so an automatic default variant holds the 60 units |

## API overview

All bodies are JSON. List endpoints are paginated (`?page=0&size=20&sort=name,asc`).

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/items` | Create an item, optionally with its variants |
| `GET` | `/api/items` | List items (`?q=` filters by name) |
| `GET` | `/api/items/{id}` | Get an item with variants and stock |
| `PUT` | `/api/items/{id}` | Update name, description, base price |
| `DELETE` | `/api/items/{id}` | Delete an item and its variants |
| `POST` | `/api/items/{itemId}/variants` | Add a variant |
| `GET` | `/api/items/{itemId}/variants` | List an item's variants |
| `GET` | `/api/variants/{id}` | Get a variant |
| `PUT` | `/api/variants/{id}` | Update sku, size, colour, price (**not** stock) |
| `DELETE` | `/api/variants/{id}` | Delete a variant |
| `PATCH` | `/api/variants/{id}/stock` | Restock (`+n`) or write off (`-n`) |
| `POST` | `/api/sales` | Sell variants; rejected with `409` if any line lacks stock |
| `GET` | `/api/sales`, `/api/sales/{id}` | Sales history |

### Examples

Create an item with variants (a variant without `price` uses the item's `basePrice`):

```bash
curl -X POST http://localhost:8080/api/items -H "Content-Type: application/json" -d '{
  "name": "Hoodie",
  "description": "Fleece hoodie",
  "basePrice": 45.00,
  "variants": [
    {"sku": "HOOD-GRY-M", "size": "M", "color": "Grey", "stockQuantity": 12},
    {"sku": "HOOD-GRY-L", "size": "L", "color": "Grey", "price": 47.50, "stockQuantity": 4}
  ]
}'
```

```json
{
  "id": 4,
  "name": "Hoodie",
  "description": "Fleece hoodie",
  "basePrice": 45.00,
  "totalStock": 16,
  "inStock": true,
  "createdAt": "2026-10-30T01:30:00Z",
  "variants": [
    {"id": 9,  "itemId": 4, "sku": "HOOD-GRY-M", "size": "M", "color": "Grey", "price": null,  "effectivePrice": 45.00, "stockQuantity": 12, "inStock": true},
    {"id": 10, "itemId": 4, "sku": "HOOD-GRY-L", "size": "L", "color": "Grey", "price": 47.50, "effectivePrice": 47.50, "stockQuantity": 4,  "inStock": true}
  ]
}
```

Create an item **without** variants (an automatic default variant carries the stock):

```bash
curl -X POST http://localhost:8080/api/items -H "Content-Type: application/json" \
  -d '{"name": "Tote Bag", "basePrice": 9.90, "initialStock": 30}'
```

Sell 2 black small t-shirts and 1 sneaker:

```bash
curl -X POST http://localhost:8080/api/sales -H "Content-Type: application/json" \
  -d '{"lines": [{"variantId": 1, "quantity": 2}, {"variantId": 5, "quantity": 1}]}'
```

```json
{
  "id": 1,
  "createdAt": "2026-10-30T01:31:00Z",
  "total": 128.98,
  "lines": [
    {"variantId": 1, "sku": "TSHIRT-BLK-S", "itemName": "Classic T-Shirt", "quantity": 2, "unitPrice": 19.99, "lineTotal": 39.98},
    {"variantId": 5, "sku": "SNKR-RED-42",  "itemName": "Running Sneakers", "quantity": 1, "unitPrice": 89.00, "lineTotal": 89.00}
  ]
}
```

Try to sell the sold-out sneaker (variant 6):

```bash
curl -i -X POST http://localhost:8080/api/sales -H "Content-Type: application/json" \
  -d '{"lines": [{"variantId": 6, "quantity": 1}]}'
```

```json
{
  "timestamp": "2026-10-30T01:32:00Z",
  "status": 409,
  "error": "OUT_OF_STOCK",
  "message": "Insufficient stock for SKU 'SNKR-RED-43': requested 1, available 0"
}
```

Restock 20 units, or write off 2 damaged ones:

```bash
curl -X PATCH http://localhost:8080/api/variants/6/stock -H "Content-Type: application/json" -d '{"delta": 20}'
curl -X PATCH http://localhost:8080/api/variants/6/stock -H "Content-Type: application/json" -d '{"delta": -2}'
```

List and search items:

```bash
curl "http://localhost:8080/api/items?q=shirt&page=0&size=10&sort=name,asc"
```

### Errors

Every error uses the same JSON shape, with a stable machine-readable `error` code:

| HTTP | `error` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Invalid field values (response includes `fieldErrors`) |
| 400 | `MALFORMED_REQUEST` / `INVALID_PARAMETER` / `INVALID_REQUEST` | Bad JSON, bad parameter, or a broken business rule |
| 404 | `NOT_FOUND` | Unknown item, variant, sale or URL |
| 409 | `OUT_OF_STOCK` | A sale or write-off needs more units than available |
| 409 | `DUPLICATE_SKU` / `CONFLICT` | SKU already used / other data conflict |
| 500 | `INTERNAL_ERROR` | Unexpected failure (details are logged, not leaked) |

## Design decisions

**Stock lives on variants only.** An item's total stock is the sum of its variants, never stored separately,
so it cannot drift out of sync. An item created without variants gets one automatic default variant
(`ITEM-<id>-DEFAULT`) that carries its stock. This keeps one uniform rule everywhere: *you sell variants*.

**Price fallback.** A variant may override the price; otherwise the item's `basePrice` applies. The price a
customer pays is exposed as `effectivePrice`. Money is `BigDecimal` (scale 2), never `double`.

**Preventing overselling: one atomic SQL statement.** Selling uses
`UPDATE variants SET stock = stock - :qty WHERE id = :id AND stock >= :qty`
(`VariantRepository.decrementStockIfAvailable`). The "is there enough?" check and the decrement are a single
statement, so two customers buying the last unit at the same time cannot both succeed; there is no
read-check-write window to race through. If zero rows are updated, the sale fails with `409 OUT_OF_STOCK`.
I chose this over `@Version` optimistic locking (needs retries) and pessimistic locks (hold locks longer).
A test fires 20 concurrent buyers at 5 units and asserts exactly 5 succeed and stock ends at 0.

**Sales are all-or-nothing.** A sale with several lines runs in one transaction; if any line fails, the whole
transaction rolls back (earlier lines get their stock back, no partial sale is stored). Lines are processed in
variant-id order so concurrent multi-line sales lock rows in the same order and cannot deadlock. Repeated
lines for the same variant are merged.

**Stock changes have only two doors:** a sale, or `PATCH /variants/{id}/stock` with a relative `delta`
(also atomic and never allowed to go below zero). `PUT /variants/{id}` deliberately cannot change stock, so a
stale update can never overwrite concurrent sales with an old number.

**Sales keep a snapshot** (sku, item name, unit price) rather than a foreign key, so history stays correct if
an item is later renamed, repriced or deleted.

**Layering and DTOs.** `controller -> service -> repository`, with DTO records separate from JPA entities,
so the API contract is independent of the schema. Bean Validation guards input; a single
`@RestControllerAdvice` maps exceptions to the uniform error format above.

**Other choices:** `spring.jpa.open-in-view=false`; Hibernate batch fetching to avoid N+1 queries when listing
items with their variants; a page wrapper (`PageResponse`) for a stable pagination JSON; no Lombok, using Java
records and plain entities so it works in any IDE without annotation-processor setup.

## Assumptions

- A single shop and warehouse: no multi-location stock, users, authentication or payments.
- The **SKU** is globally unique and identifies a variant.
- Variants are sold by variant id; selling an "item" means choosing one of its variants.
- Prices are in one currency, with two decimals.
- Deleting an item deletes its variants; past sales are kept.
- Deleting an item's last variant is allowed (the item then simply cannot be sold until a variant is added).
- In-memory H2 is enough for this exercise; switching to PostgreSQL only needs a driver and datasource settings
  (the one H2-specific bit is the console setting in `application.properties`).

## Project structure

```
src/main/java/com/example/inventory
├── controller/   REST endpoints (Items, Variants & stock, Sales)
├── service/      Business rules and transactions (ItemService, VariantService, SaleService)
├── repository/   Spring Data JPA, including the atomic stock queries
├── model/        JPA entities: Item, Variant, Sale, SaleLine
├── dto/          Request/response records with validation rules
├── exception/    Domain exceptions + GlobalExceptionHandler + ApiError
└── config/       DataSeeder (sample data)
src/test/java     Unit test, API tests (MockMvc) and the concurrency test
```

## Possible next steps

Authentication, soft-delete / audit trail of stock movements, low-stock alerts, reservation of stock for unpaid
carts, Flyway migrations with a persistent database, and a Docker setup.

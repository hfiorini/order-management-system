# IS360 Customer Order Management System

A Spring Boot REST API for managing customers, products, and orders with stock management, premium order flagging, and product caching.

## Tech Stack

- **Java 17** + **Spring Boot 3.2.5**
- **Spring Data JPA** (Hibernate) with **H2** in-memory database
- **Caffeine Cache** for product caching (10-minute TTL)
- **Bean Validation** (Jakarta Validation)
- **springdoc-openapi** (Swagger UI)
- **Maven** build

## How to Run

### Prerequisites
- Java 17+ installed
- Maven 3.8+ installed (or use the included Maven wrapper if present)

### Build & Run

```bash
cd order-management-system
mvn clean install
mvn spring-boot:run
```

The application starts on **http://localhost:8080**.

### Run Tests

```bash
mvn test
```

### H2 Console

Browse the in-memory database at **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:orderdb`
- Username: `sa`
- Password: *(leave blank)*

### Swagger

Interactive API documentation available at:
```
http://localhost:8080/swagger-ui/index.html
```

## API Endpoints

### Customers

**POST /customers** — Create a customer
```json
{
  "name": "John Doe",
  "email": "john@example.com"
}
```

### Products

**POST /products** — Create a product
```json
{
  "name": "Widget",
  "price": 29.99,
  "stockQuantity": 100
}
```

**GET /products** — List all products *(cached for 10 minutes)*

**PUT /products/{id}** — Update a product *(invalidates cache)*

### Orders

**POST /orders** — Place an order
```json
{
  "customerId": 1,
  "items": [
    { "productId": 1, "quantity": 3 },
    { "productId": 2, "quantity": 1 }
  ]
}
```

**GET /orders/{customerId}** — Get all orders for a customer

## Business Rules

| Rule | Behavior |
|------|----------|
| Stock check | Each product's stock is validated before the order is placed |
| Stock deduction | Ordered quantities are deducted from product stock |
| Transactional rollback | If *any* product has insufficient stock, the entire order (and all stock changes) is rolled back |
| Premium orders | Orders with a total exceeding $500 are automatically flagged as `premium: true` |

## Validation Rules

| Field | Rule | HTTP Status |
|-------|------|-------------|
| Customer email | Must be unique | 409 Conflict |
| Customer email | Must be valid format | 400 Bad Request |
| Product name | Must not be blank | 400 Bad Request |
| Order items | At least one item required | 400 Bad Request |
| Item quantity | Must be ≥ 1 | 400 Bad Request |

## Caching

- **Product list** (`GET /products`) is cached for **10 minutes** using Caffeine.
- Cache is **invalidated** whenever a product is created (`POST`) or updated (`PUT`).
- Implemented via Spring `@Cacheable` and `@CacheEvict` annotations.

## Project Structure

```
src/main/java/com/invenco/is360/
├── OrderManagementApplication.java   # Main entry point
├── config/
│   └── CacheConfig.java              # Caffeine cache configuration
├── controller/
│   ├── CustomerController.java       # POST /customers
│   ├── ProductController.java        # POST/GET/PUT /products
│   └── OrderController.java          # POST /orders, GET /orders/{id}
├── dto/
│   ├── CustomerRequest.java          # Customer creation request body
│   ├── CustomerResponse.java         # Customer API response
│   ├── ProductRequest.java           # Product creation/update request body
│   ├── ProductResponse.java          # Product API response
│   ├── OrderRequest.java             # Order creation request body
│   ├── OrderItemRequest.java         # Individual line item in order
│   ├── OrderResponse.java            # Order API response (with nested OrderItemResponse)
│   └── ErrorResponse.java            # Standardised error envelope
├── entity/
│   ├── Customer.java                 # @OneToMany → Order
│   ├── Product.java                  # Stock-managed product
│   ├── Order.java                    # @ManyToOne → Customer, @OneToMany → OrderItem
│   └── OrderItem.java               # Join entity: Order ↔ Product with quantity
├── exception/
│   ├── DuplicateEmailException.java
│   ├── InsufficientStockException.java
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java   # @RestControllerAdvice
├── repository/
│   ├── CustomerRepository.java
│   ├── ProductRepository.java
│   └── OrderRepository.java
└── service/
    ├── CustomerService.java
    ├── ProductService.java           # @Cacheable / @CacheEvict
    └── OrderService.java             # @Transactional order placement
```

## Assumptions & Design Decisions

1. **DTO layer separation**: Controllers only work with request/response DTOs — no JPA entity classes are exposed to the API layer. All DTO ↔ entity mapping happens inside the service layer, keeping the API contract independent of the database model.

2. **OrderItem join entity**: An `OrderItem` entity was introduced to properly model the many-to-many relationship between Orders and Products while storing per-line quantity and price. This is a more realistic model than a simple `@ManyToMany`.

3. **Price snapshot**: `OrderItem` captures the `unitPrice` at the time of ordering, so future product price changes don't retroactively alter historical orders.

4. **Pessimistic stock check**: Stock is checked and deducted within a single `@Transactional` method. In a production system with concurrent requests, you'd want pessimistic locking (`@Lock(PESSIMISTIC_WRITE)`) or optimistic locking (`@Version`) on the Product entity.

5. **Premium threshold**: The $500 premium threshold uses strict greater-than (`> 500`), meaning exactly $500.00 is *not* premium.

6. **Cache scope**: Only the "list all products" query is cached. Individual product lookups (used during order placement) hit the database directly to ensure stock accuracy.

7. **H2 in-memory**: Data is lost on restart. For persistence, swap the datasource to PostgreSQL/MySQL with minimal config changes.

## Potential Improvements

- Add pagination to `GET /orders/{customerId}` and `GET /products`
- Add optimistic locking (`@Version`) on Product for concurrent stock safety
- Add authentication/authorization (Spring Security)
- Add order status workflow (PENDING → CONFIRMED → SHIPPED)
- Add integration with a persistent database (PostgreSQL)

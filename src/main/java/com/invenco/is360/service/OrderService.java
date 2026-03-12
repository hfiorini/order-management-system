package com.invenco.is360.service;

import com.invenco.is360.dto.OrderItemRequest;
import com.invenco.is360.dto.OrderRequest;
import com.invenco.is360.entity.Customer;
import com.invenco.is360.entity.Order;
import com.invenco.is360.entity.OrderItem;
import com.invenco.is360.entity.Product;
import com.invenco.is360.exception.InsufficientStockException;
import com.invenco.is360.exception.ResourceNotFoundException;
import com.invenco.is360.repository.OrderRepository;
import com.invenco.is360.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private static final BigDecimal PREMIUM_THRESHOLD = new BigDecimal("500");

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        CustomerService customerService,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.productRepository = productRepository;
    }

    /**
     * Creates an order. The entire operation is transactional — if any product
     * has insufficient stock, all stock deductions are rolled back.
     */
    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. Validate customer exists
        Customer customer = customerService.findById(request.getCustomerId());
        if (customer == null) {
            throw new ResourceNotFoundException(
                    "Customer not found with id: " + request.getCustomerId()
            );
        }

        Order order = new Order();
        order.setCustomer(customer);

        BigDecimal total = BigDecimal.ZERO;

        // 2. Process each item: check stock, deduct, create line item
        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + itemReq.getProductId()
                    ));

            // Check stock availability
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                // This exception causes @Transactional to rollback all changes
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + product.getName()
                                + "': requested " + itemReq.getQuantity()
                                + ", available " + product.getStockQuantity()
                );
            }

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepository.save(product);

            // Create order item
            OrderItem orderItem = new OrderItem(product, itemReq.getQuantity());
            order.addItem(orderItem);

            total = total.add(orderItem.getLineTotal());
        }

        // 3. Set total and premium flag
        order.setTotalAmount(total);
        if (total.compareTo(PREMIUM_THRESHOLD) > 0) {
            order.setPremium(true);
        }

        return orderRepository.save(order);
    }

    /**
     * Fetches all orders for a given customer.
     */
    public List<Order> getOrdersByCustomerId(Long customerId) {
        // Verify customer exists first
        Customer customer = customerService.findById(customerId);
        if (customer == null) {
            throw new ResourceNotFoundException(
                    "Customer not found with id: " + customerId
            );
        }
        return orderRepository.findByCustomer_Id(customerId);
    }
}

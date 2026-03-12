package com.invenco.is360;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invenco.is360.dto.OrderItemRequest;
import com.invenco.is360.dto.OrderRequest;
import com.invenco.is360.entity.Customer;
import com.invenco.is360.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderManagementApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void createCustomer_success() throws Exception {
        Customer customer = new Customer("John Doe", "john@example.com");

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void createCustomer_duplicateEmail_returns409() throws Exception {
        Customer customer = new Customer("John Doe", "john@example.com");
        String json = objectMapper.writeValueAsString(customer);

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void createProduct_success() throws Exception {
        Product product = new Product("Widget", new BigDecimal("29.99"), 100);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.stockQuantity").value(100));
    }

    @Test
    void createProduct_blankName_returns400() throws Exception {
        Product product = new Product("", new BigDecimal("29.99"), 100);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_success_deductsStock() throws Exception {
        // Create customer
        Customer customer = new Customer("Jane", "jane@example.com");
        MvcResult custResult = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isCreated())
                .andReturn();
        Long customerId = objectMapper.readTree(custResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Create product
        Product product = new Product("Gadget", new BigDecimal("100.00"), 50);
        MvcResult prodResult = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andReturn();
        Long productId = objectMapper.readTree(prodResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Create order
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(3);

        OrderRequest orderReq = new OrderRequest();
        orderReq.setCustomerId(customerId);
        orderReq.setItems(List.of(item));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(300.00))
                .andExpect(jsonPath("$.premium").value(false));

        // Verify stock deducted
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockQuantity").value(47));
    }

    @Test
    void createOrder_premiumFlagged() throws Exception {
        Customer customer = new Customer("Premium Buyer", "premium@example.com");
        MvcResult custResult = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andReturn();
        Long customerId = objectMapper.readTree(custResult.getResponse().getContentAsString())
                .get("id").asLong();

        Product product = new Product("Expensive Item", new BigDecimal("200.00"), 10);
        MvcResult prodResult = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andReturn();
        Long productId = objectMapper.readTree(prodResult.getResponse().getContentAsString())
                .get("id").asLong();

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(3); // 3 * 200 = 600 > 500

        OrderRequest orderReq = new OrderRequest();
        orderReq.setCustomerId(customerId);
        orderReq.setItems(List.of(item));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(600.00))
                .andExpect(jsonPath("$.premium").value(true));
    }

    @Test
    void createOrder_insufficientStock_returns400() throws Exception {
        Customer customer = new Customer("Bob", "bob@example.com");
        MvcResult custResult = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andReturn();
        Long customerId = objectMapper.readTree(custResult.getResponse().getContentAsString())
                .get("id").asLong();

        Product product = new Product("Rare Item", new BigDecimal("50.00"), 2);
        MvcResult prodResult = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andReturn();
        Long productId = objectMapper.readTree(prodResult.getResponse().getContentAsString())
                .get("id").asLong();

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(5); // only 2 in stock

        OrderRequest orderReq = new OrderRequest();
        orderReq.setCustomerId(customerId);
        orderReq.setItems(List.of(item));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrdersByCustomer_success() throws Exception {
        // Create customer
        Customer customer = new Customer("Alice", "alice@example.com");
        MvcResult custResult = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andReturn();
        Long customerId = objectMapper.readTree(custResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/orders/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getOrdersByCustomer_notFound_returns404() throws Exception {
        mockMvc.perform(get("/orders/999"))
                .andExpect(status().isNotFound());
    }
}

package com.invenco.is360.dto;

import com.invenco.is360.entity.Customer;

public class CustomerResponse {

    private Long id;
    private String name;
    private String email;

    public CustomerResponse() {}

    public CustomerResponse(Customer entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.email = entity.getEmail();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

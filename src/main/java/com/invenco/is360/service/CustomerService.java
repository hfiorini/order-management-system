package com.invenco.is360.service;

import com.invenco.is360.dto.CustomerRequest;
import com.invenco.is360.dto.CustomerResponse;
import com.invenco.is360.entity.Customer;
import com.invenco.is360.exception.DuplicateEmailException;
import com.invenco.is360.repository.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(
                    "A customer with email '" + request.getEmail() + "' already exists"
            );
        }
        Customer entity = new Customer(request.getName(), request.getEmail());
        Customer saved = customerRepository.save(entity);
        return new CustomerResponse(saved);
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id).orElse(null);
    }
}

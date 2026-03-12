package com.invenco.is360.service;

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

    public Customer createCustomer(Customer customer) {
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new DuplicateEmailException(
                    "A customer with email '" + customer.getEmail() + "' already exists"
            );
        }
        return customerRepository.save(customer);
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id).orElse(null);
    }
}

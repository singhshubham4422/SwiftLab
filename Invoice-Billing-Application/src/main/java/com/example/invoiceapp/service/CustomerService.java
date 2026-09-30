package com.example.invoiceapp.service;

import com.example.invoiceapp.dto.CustomerDTO;
import com.example.invoiceapp.model.Customer;
import com.example.invoiceapp.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepo;

    public CustomerService(CustomerRepository customerRepo) {
        this.customerRepo = customerRepo;
    }

    public List<CustomerDTO> listByOrganization(Long organizationId) {
        if (organizationId == null) return List.of();
        return customerRepo.findByOrganizationIdAndDeletedFalseOrderByNameAsc(organizationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<Customer> findById(Long id, Long organizationId) {
        if (id == null || organizationId == null) return Optional.empty();
        return customerRepo.findByIdAndOrganizationId(id, organizationId)
                .filter(c -> !c.isDeleted());
    }

    public Customer get(Long id, Long organizationId) {
        return findById(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));
    }

    @Transactional
    public Customer save(Long organizationId, CustomerDTO dto) {
        if (organizationId == null) throw new IllegalArgumentException("Organization ID is required");

        Customer customer;
        if (dto.getId() != null) {
            customer = get(dto.getId(), organizationId);
        } else {
            customer = new Customer();
            customer.setOrganizationId(organizationId);
            customer.setCreatedAt(Instant.now());
        }

        customer.setName(dto.getName() != null ? dto.getName().trim() : "Unnamed Customer");
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customer.setTaxNumber(dto.getTaxNumber());
        customer.setCreditLimit(dto.getCreditLimit() != null ? dto.getCreditLimit() : BigDecimal.ZERO);
        customer.setNotes(dto.getNotes());
        customer.setUpdatedAt(Instant.now());

        return customerRepo.save(customer);
    }

    @Transactional
    public void delete(Long id, Long organizationId) {
        Customer c = get(id, organizationId);
        c.markDeleted();
        customerRepo.save(c);
    }

    public CustomerDTO toDTO(Customer c) {
        if (c == null) return null;
        CustomerDTO dto = new CustomerDTO();
        dto.setId(c.getId());
        dto.setPublicId(c.getPublicId());
        dto.setName(c.getName());
        dto.setPhone(c.getPhone());
        dto.setEmail(c.getEmail());
        dto.setAddress(c.getAddress());
        dto.setTaxNumber(c.getTaxNumber());
        dto.setCreditLimit(c.getCreditLimit());
        dto.setNotes(c.getNotes());
        return dto;
    }
}

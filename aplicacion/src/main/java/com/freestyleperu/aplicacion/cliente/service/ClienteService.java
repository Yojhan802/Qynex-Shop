package com.freestyleperu.aplicacion.cliente.service;

import com.freestyleperu.aplicacion.cliente.domain.Customer;
import com.freestyleperu.aplicacion.cliente.dto.request.ClienteRequest;
import com.freestyleperu.aplicacion.cliente.dto.response.ClienteResponse;
import com.freestyleperu.aplicacion.cliente.repository.CustomerRepository;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final CustomerRepository customerRepository;

    public ClienteService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Page<ClienteResponse> listar(String search, EstadoGeneral status, Pageable pageable) {
        return customerRepository.buscar(search, status, pageable).map(this::toResponse);
    }

    public List<ClienteResponse> buscarRapido(String query) {
        return customerRepository.buscarRapido(query).stream().map(this::toResponse).toList();
    }

    public ClienteResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        validarDocumentoUnico(request.docNumber(), null);
        Customer customer = new Customer();
        aplicar(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Customer customer = buscarOFallar(id);
        validarDocumentoUnico(request.docNumber(), customer.getDocNumber());
        aplicar(customer, request);
        return toResponse(customer);
    }

    @Transactional
    public ClienteResponse cambiarEstado(Long id, EstadoGeneral status) {
        Customer customer = buscarOFallar(id);
        customer.setStatus(status);
        return toResponse(customer);
    }

    private void validarDocumentoUnico(String docNumber, String actual) {
        if (docNumber != null && !docNumber.isBlank() && !docNumber.equals(actual)
                && customerRepository.existsByDocNumber(docNumber)) {
            throw new RecursoDuplicadoException("Ya existe un cliente con el documento " + docNumber);
        }
    }

    private void aplicar(Customer customer, ClienteRequest request) {
        customer.setFullName(request.fullName());
        customer.setDocType(request.docType());
        customer.setDocNumber(blankToNull(request.docNumber()));
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setBirthDate(request.birthDate());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private Customer buscarOFallar(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", id));
    }

    private ClienteResponse toResponse(Customer customer) {
        return new ClienteResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getDocType(),
                customer.getDocNumber(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getBirthDate(),
                customer.getStatus(),
                customer.getCreatedAt());
    }
}

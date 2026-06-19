package com.willysoft.productosapi.cliente;

import com.willysoft.productosapi.audit.AuditService;
import com.willysoft.productosapi.cliente.dto.ClienteRequest;
import com.willysoft.productosapi.cliente.dto.ClienteResponse;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ClienteResponse> search(String texto, Pageable pageable) {
        Page<Cliente> page = StringUtils.hasText(texto)
                ? clienteRepository.findByNombreContainingIgnoreCaseOrDocumentoContainingIgnoreCase(texto, texto, pageable)
                : clienteRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public java.util.List<ClienteResponse> findAll() {
        return clienteRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse findById(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    @Transactional
    public ClienteResponse create(ClienteRequest request) {
        Cliente cliente = crearRapido(request.nombre(), request.documento(), request.condicionIva());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        cliente.setDireccion(request.direccion());
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse update(Long id, ClienteRequest request) {
        Cliente cliente = getEntityOrThrow(id);
        String documento = normalizar(request.documento());
        if (documento != null && !documento.equals(cliente.getDocumento())
                && clienteRepository.existsByDocumento(documento)) {
            throw new ConflictException("Ya existe un cliente con el documento: " + documento);
        }
        cliente.setNombre(request.nombre());
        cliente.setDocumento(documento);
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        cliente.setDireccion(request.direccion());
        cliente.setCondicionIva(request.condicionIva() != null ? request.condicionIva() : cliente.getCondicionIva());
        Cliente saved = clienteRepository.save(cliente);
        auditService.log("CLIENTE_ACTUALIZADO", saved.getNombre(), SecurityUtils.currentEmail());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Cliente cliente = getEntityOrThrow(id);
        clienteRepository.delete(cliente);
        auditService.log("CLIENTE_ELIMINADO", cliente.getNombre(), SecurityUtils.currentEmail());
    }

    /** Cliente (entidad) por id, o excepción. Usado por la facturación. */
    @Transactional(readOnly = true)
    public Cliente getEntityOrThrow(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    /**
     * Crea y persiste un cliente con los datos mínimos. Reutilizado por el alta rápida
     * desde la facturación. Valida unicidad del documento si viene informado.
     */
    @Transactional
    public Cliente crearRapido(String nombre, String documento, CondicionIva condicionIva) {
        String doc = normalizar(documento);
        if (doc != null && clienteRepository.existsByDocumento(doc)) {
            throw new ConflictException("Ya existe un cliente con el documento: " + doc);
        }
        Cliente cliente = Cliente.builder()
                .nombre(nombre)
                .documento(doc)
                .condicionIva(condicionIva != null ? condicionIva : CondicionIva.CONSUMIDOR_FINAL)
                .activo(true)
                .creadoPor(SecurityUtils.currentEmail())
                .build();
        Cliente saved = clienteRepository.save(cliente);
        auditService.log("CLIENTE_CREADO", saved.getNombre(), SecurityUtils.currentEmail());
        return saved;
    }

    private String normalizar(String documento) {
        return StringUtils.hasText(documento) ? documento.trim() : null;
    }

    private ClienteResponse toResponse(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getDocumento(), c.getEmail(),
                c.getTelefono(), c.getDireccion(), c.getCondicionIva(), c.isActivo(), c.getFechaCreacion());
    }
}

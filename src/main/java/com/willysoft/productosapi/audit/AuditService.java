package com.willysoft.productosapi.audit;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String accion, String detalle, String usuario) {
        auditLogRepository.save(AuditLog.builder()
                .accion(accion)
                .detalle(detalle)
                .usuario(usuario)
                .fecha(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<AuditLog> ultimas() {
        return auditLogRepository.findTop10ByOrderByFechaDesc();
    }
}

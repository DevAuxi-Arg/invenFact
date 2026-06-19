package com.willysoft.productosapi.user;

import com.willysoft.productosapi.security.SecurityUtils;
import com.willysoft.productosapi.user.dto.ChangePasswordRequest;
import com.willysoft.productosapi.user.dto.UpdateRoleRequest;
import com.willysoft.productosapi.user.dto.UserCreateRequest;
import com.willysoft.productosapi.user.dto.UserResponse;
import com.willysoft.productosapi.user.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios y roles")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    @Operation(summary = "Listar usuarios paginados (ADMIN, CO-ADMIN)")
    public ResponseEntity<Page<UserResponse>> search(
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.search(texto, pageable));
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(userService.findByEmail(SecurityUtils.currentEmail()));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Cambiar la propia contraseña")
    public ResponseEntity<Void> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(SecurityUtils.currentEmail(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    @Operation(summary = "Obtener usuario por id (ADMIN, CO-ADMIN)")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    @Operation(summary = "Crear usuario (ADMIN crea cualquier rol; CO-ADMIN solo BACKOFFICE)")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    @Operation(summary = "Actualizar usuario (CO-ADMIN solo sobre BACKOFFICE)")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar el rol de un usuario (solo ADMIN)")
    public ResponseEntity<UserResponse> updateRole(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar usuario (solo ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

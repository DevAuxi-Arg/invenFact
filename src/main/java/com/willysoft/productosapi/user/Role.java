package com.willysoft.productosapi.user;

/**
 * Roles del sistema.
 *
 * <ul>
 *   <li><b>ADMIN</b>: superusuario. Gestiona usuarios y sistema, único que puede
 *       crear/editar/eliminar otros ADMIN y CO-ADMIN y cambiar roles.</li>
 *   <li><b>COADMIN</b>: admin delegado. Opera el catálogo completo (incluido eliminar)
 *       y puede dar de alta usuarios BACKOFFICE, pero no toca a otros administradores,
 *       roles ni configuración.</li>
 *   <li><b>BACKOFFICE</b>: operativo. Carga y edita el catálogo, sin eliminar ni
 *       acceso a la gestión de usuarios.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    COADMIN,
    BACKOFFICE
}

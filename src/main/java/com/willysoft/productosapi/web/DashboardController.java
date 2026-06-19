package com.willysoft.productosapi.web;

import com.willysoft.productosapi.audit.AuditService;
import com.willysoft.productosapi.category.CategoryRepository;
import com.willysoft.productosapi.category.dto.CategoryResponse;
import com.willysoft.productosapi.category.CategoryService;
import com.willysoft.productosapi.product.ProductRepository;
import com.willysoft.productosapi.user.UserRepository;
import com.willysoft.productosapi.user.UserService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final UserService userService;
    private final AuditService auditService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalProductos", productRepository.count());
        model.addAttribute("totalCategorias", categoryRepository.count());
        model.addAttribute("totalUsuarios", userRepository.count());
        model.addAttribute("usuariosActivos", userRepository.countByActivoTrue());

        // Datos para el gráfico de productos por categoría
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (CategoryResponse c : categoryService.findAll()) {
            labels.add(c.nombre());
            data.add(productRepository.countByCategoriaId(c.id()));
        }
        model.addAttribute("chartLabels", labels);
        model.addAttribute("chartData", data);

        // Últimos usuarios registrados
        var ultimos = userService.search(null,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "fechaCreacion")));
        model.addAttribute("ultimosUsuarios", ultimos.getContent());

        // Auditoría reciente
        model.addAttribute("auditoria", auditService.ultimas());

        return "admin/dashboard";
    }
}

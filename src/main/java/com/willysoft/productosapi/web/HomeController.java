package com.willysoft.productosapi.web;

import com.willysoft.productosapi.category.CategoryRepository;
import com.willysoft.productosapi.cliente.ClienteRepository;
import com.willysoft.productosapi.factura.FacturaRepository;
import com.willysoft.productosapi.parametro.ParametroService;
import com.willysoft.productosapi.product.ProductRepository;
import com.willysoft.productosapi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ClienteRepository clienteRepository;
    private final FacturaRepository facturaRepository;
    private final UserRepository userRepository;
    private final ParametroService parametroService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("totalCategorias", categoryRepository.count());
        model.addAttribute("totalProductos", productRepository.count());
        model.addAttribute("totalClientes", clienteRepository.count());
        model.addAttribute("totalFacturas", facturaRepository.count());
        model.addAttribute("totalUsuarios", userRepository.count());
        try {
            model.addAttribute("dolar", parametroService.getDolar());
        } catch (RuntimeException e) {
            model.addAttribute("dolar", null);
        }
        return "home";
    }
}

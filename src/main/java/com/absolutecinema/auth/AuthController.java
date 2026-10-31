package com.absolutecinema.auth;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

@Controller
public class AuthController {
	private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult result) {
        if (!result.hasErrors() && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Las contraseñas no coinciden");
        }
        if (!result.hasErrors() && userService.emailExists(form.getEmail())) {
            result.rejectValue("email", "duplicate", "Ya existe una cuenta con ese correo");
        }
        if (result.hasErrors()) {
            return "register";
        }
        userService.register(form);
        return "redirect:/login?registered";
    }

    // TEMPORAL: página de inicio mientras no exista la cartelera
    @GetMapping("/")
    public String home(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        return "home";
    }
}

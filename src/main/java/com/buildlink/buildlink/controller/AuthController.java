package com.buildlink.buildlink.controller;

import com.buildlink.buildlink.dto.RegisterDTO;
import com.buildlink.buildlink.entity.Role;
import com.buildlink.buildlink.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        model.addAttribute("roles", Role.values());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterDTO registerDTO,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "auth/register";
        }
        // ✅ Vérification que les mots de passe correspondent
        if (!registerDTO.isPasswordMatching()) {
            model.addAttribute("error", "Les mots de passe ne correspondent pas");
            model.addAttribute("roles", Role.values());
            return "auth/register";
        }
        try {
            userService.register(registerDTO);
            return "redirect:/auth/login?success=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", Role.values());
            return "auth/register";
        }
    }
}
package com.example.employeeapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.employeeapp.model.Employee;
import com.example.employeeapp.service.EmployeeService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {
	private final EmployeeService employeeService;
	private static final String SESSION_USER_EMAIL = "userEmail";

	public AuthController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping("/")
	public String home() {
		return "redirect:/home";
	}

	@GetMapping("/home")
	public String homePage(Model model,HttpSession session) {
		String email = (String) session.getAttribute("userEmail");
		if (email != null) {
			var e = employeeService.findByEmail(email);
			model.addAttribute("loggedIn", true);
			model.addAttribute("employee", e);
		} else {
			model.addAttribute("loggedIn", false);
		}
		return "home";
	}
	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("employee", new Employee());
		return "register";
	}

	@PostMapping("/register")    
	public String registerSubmit(@ModelAttribute Employee e, RedirectAttributes ra) 
	{ 
		employeeService.register(e);
	 ra.addFlashAttribute("success","Registered");
	 return "redirect:/login";
	}

	@GetMapping("/login")
	public String loginForm(Model model) {
		return "login";
	}

	@PostMapping("/login")
	public String loginSubmit(@RequestParam String email, @RequestParam String password,
			RedirectAttributes redirectAttrs, HttpSession session) {
		Employee e = employeeService.authenticate(email, password);
		if (e == null) {
			redirectAttrs.addFlashAttribute("error", "Invalid credentials");
			return "redirect:/login";
		}
		session.setAttribute(SESSION_USER_EMAIL, e.getEmail());
		return "redirect:/profile";
	}

	@GetMapping("/profile")
	public String profile(Model model, HttpSession session, RedirectAttributes redirectAttrs) {
		String email = (String) session.getAttribute("userEmail");
		if (email == null) {
			redirectAttrs.addFlashAttribute("error", "Please log in first");
			return "redirect:/login";
		}
		Employee e = employeeService.findByEmail(email);
		model.addAttribute("employee", e);
		return "profile";
	}

	@GetMapping("/logout")
	public String logout(HttpSession session, RedirectAttributes redirectAttrs) {
		session.invalidate();
		redirectAttrs.addFlashAttribute("success", "Logged out");
		return "redirect:/home";
	}
}

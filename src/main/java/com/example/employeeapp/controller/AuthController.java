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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;

@Controller
@Tag(name = "Authentication", description = "Authentication and registration endpoints")
public class AuthController {
	private final EmployeeService employeeService;
	private final MeterRegistry meterRegistry;
	private final Counter registrationCounter;
	private final Counter loginSuccessCounter;
	private final Counter loginFailureCounter;
	private final Counter logoutCounter;
	private static final String SESSION_USER_EMAIL = "userEmail";

	public AuthController(EmployeeService employeeService, MeterRegistry meterRegistry) {
		this.employeeService = employeeService;
		this.meterRegistry = meterRegistry;
		
		// Initialize counters for actuator metrics
		this.registrationCounter = Counter.builder("auth.registration.total")
			.description("Total number of employee registrations")
			.register(meterRegistry);
		
		this.loginSuccessCounter = Counter.builder("auth.login.success.total")
			.description("Total number of successful login attempts")
			.register(meterRegistry);
		
		this.loginFailureCounter = Counter.builder("auth.login.failure.total")
			.description("Total number of failed login attempts")
			.register(meterRegistry);
		
		this.logoutCounter = Counter.builder("auth.logout.total")
			.description("Total number of logouts")
			.register(meterRegistry);
	}

	@GetMapping("/")
	@Operation(summary = "Redirect to home page", description = "Redirects root path to home page")
	@ApiResponse(responseCode = "302", description = "Redirect to /home")
	public String home() {
		return "redirect:/home";
	}

	@GetMapping("/home")
	@Operation(summary = "Display home page", description = "Shows home page with employee info if logged in")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Home page displayed successfully"),
		@ApiResponse(responseCode = "302", description = "Redirect to login if not authenticated")
	})
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
	@Operation(summary = "Display registration form", description = "Returns the registration form page")
	@ApiResponse(responseCode = "200", description = "Registration form displayed")
	public String registerForm(Model model) {
		model.addAttribute("employee", new Employee());
		return "register";
	}

	@PostMapping("/register")
	@Operation(summary = "Register new employee", description = "Registers a new employee with provided details")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "302", description = "Redirect to login after successful registration"),
		@ApiResponse(responseCode = "400", description = "Invalid input data")
	})
	public String registerSubmit(@ModelAttribute Employee e, RedirectAttributes ra) 
	{ 
		employeeService.register(e);
	 ra.addFlashAttribute("success","Registered");
	 registrationCounter.increment(); // Actuator metric
	 return "redirect:/login";
	}

	@GetMapping("/login")
	@Operation(summary = "Display login form", description = "Returns the login form page")
	@ApiResponse(responseCode = "200", description = "Login form displayed")
	public String loginForm(Model model) {
		return "login";
	}

	@PostMapping("/login")
	@Operation(summary = "Authenticate employee", description = "Authenticates employee with email and password")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "302", description = "Redirect to profile on successful authentication"),
		@ApiResponse(responseCode = "401", description = "Invalid email or password")
	})
	public String loginSubmit(@Parameter(description = "Employee email address") @RequestParam String email, @Parameter(description = "Employee password") @RequestParam String password,
			RedirectAttributes redirectAttrs, HttpSession session) {
		Employee e = employeeService.authenticate(email, password);
		if (e == null) {
			redirectAttrs.addFlashAttribute("error", "Invalid credentials");
			loginFailureCounter.increment(); // Actuator metric for failed login
			return "redirect:/login";
		}
		session.setAttribute(SESSION_USER_EMAIL, e.getEmail());
		loginSuccessCounter.increment(); // Actuator metric for successful login
		return "redirect:/profile";
	}

	@GetMapping("/profile")
	@Operation(summary = "Display employee profile", description = "Shows the logged-in employee's profile")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Profile displayed successfully"),
		@ApiResponse(responseCode = "302", description = "Redirect to login if not authenticated")
	})
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
	@Operation(summary = "Logout employee", description = "Invalidates session and logs out the employee")
	@ApiResponse(responseCode = "302", description = "Redirect to home after logout")
	public String logout(HttpSession session, RedirectAttributes redirectAttrs) {
		session.invalidate();
		redirectAttrs.addFlashAttribute("success", "Logged out");
		logoutCounter.increment(); // Actuator metric for logout
		return "redirect:/home";
	}
}

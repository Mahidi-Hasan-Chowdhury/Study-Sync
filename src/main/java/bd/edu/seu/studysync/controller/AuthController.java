package bd.edu.seu.studysync.controller;

import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.security.JwtService;
import bd.edu.seu.studysync.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    
    /**
     * Display login page
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        return "login";
    }
    
    /**
     * Process login
     */
    @PostMapping("/auth/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        
        // Authenticate user
        Optional<User> userOpt = userService.authenticateUser(username, password);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Generate JWT token
            String token = jwtService.generateToken(user.getUsername(), user.getRole());
            
            // Create cookie with JWT token
            Cookie cookie = new Cookie("JWTtoken", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(86400); // 24 hours
            // cookie.setSecure(true); // Enable in production with HTTPS
            response.addCookie(cookie);
            
            // Redirect to quiz page
            return "redirect:/quiz/dashboard";
        }
        
        // Login failed
        return "redirect:/login?error";
    }
    
    /**
     * Display registration page
     */
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
    
    /**
     * Process registration
     */
    @PostMapping("/auth/register")
    public String register(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/register";
        }
        
        // Validate password length
        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters");
            return "redirect:/register";
        }
        
        try {
            // Register user
            userService.registerUser(username, email, password);
            
            // Success message
            redirectAttributes.addFlashAttribute("success", 
                    "Registration successful! Please login with your credentials.");
            return "redirect:/login";
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
    
    /**
     * Logout - clear JWT cookie
     */
    @GetMapping("/auth/logout")
    public String logout(HttpServletResponse response, RedirectAttributes redirectAttributes) {
        // Clear JWT cookie
        Cookie cookie = new Cookie("JWTtoken", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // Delete cookie
        response.addCookie(cookie);
        
        redirectAttributes.addFlashAttribute("success", "You have been logged out successfully");
        return "redirect:/login";
    }
}

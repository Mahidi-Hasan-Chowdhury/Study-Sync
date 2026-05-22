package bd.edu.seu.studysync.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles requests to the application root.
 * When a user accesses https://study-sync‑vdln.onrender.com/ we redirect them to the login page.
 */
@Controller
public class HomeController {

    /**
     * Redirect the root path to the login page.
     * @return a redirect string to "/login"
     */
    @GetMapping("/")
    public String homeRedirect() {
        return "redirect:/login";
    }
}

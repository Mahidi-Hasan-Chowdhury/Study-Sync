package bd.edu.seu.studysync.controller;

import bd.edu.seu.studysync.model.SystemConfig;
import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.service.DocsService;
import bd.edu.seu.studysync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DocsController {

    private final DocsService docsService;
    private final UserService userService;

    /**
     * Main documentation page
     * GET /docs
     */
    @GetMapping("/docs")
    public String docs(Model model) {
        if (!docsService.isDocsAccessible()) {
            model.addAttribute("contentPage", "docs-error");
            return "layout";
        }

        Map<String, Long> stats = docsService.getLiveStats();
        model.addAttribute("stats", stats);
        model.addAttribute("contentPage", "docs");
        return "layout";
    }

    /**
     * Admin configuration page
     * GET /docs/admin
     */
    @GetMapping("/docs/admin")
    public String docsAdmin(
            @RequestParam(required = false) String passcode,
            Model model,
            RedirectAttributes redirectAttributes) {
        // Check if user is logged in or provides valid passcode
        Optional<User> currentUser = userService.getCurrentUser();
        boolean hasAccess = currentUser.isPresent() || docsService.verifyAdminPasscode(passcode);

        if (!hasAccess) {
            redirectAttributes.addFlashAttribute("error", "Invalid admin credentials");
            return "redirect:/docs";
        }

        model.addAttribute("config", docsService.getConfig());
        model.addAttribute("contentPage", "docs-admin");
        return "layout";
    }

    /**
     * Save admin configuration
     * POST /docs/admin/save
     */
    @PostMapping("/docs/admin/save")
    public String saveConfig(
            @RequestParam boolean docsPublic,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime availableFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime availableTo,
            @RequestParam String adminPasscode,
            @RequestParam String currentPasscode,
            RedirectAttributes redirectAttributes) {
        // Verify current passcode before saving
        if (!docsService.verifyAdminPasscode(currentPasscode)) {
            redirectAttributes.addFlashAttribute("error", "Invalid current passcode");
            return "redirect:/docs/admin";
        }

        SystemConfig config = docsService.getConfig();
        config.setDocsPublic(docsPublic);
        config.setAvailableFrom(availableFrom);
        config.setAvailableTo(availableTo);
        config.setAdminPasscode(adminPasscode);

        docsService.saveConfig(config);

        redirectAttributes.addFlashAttribute("success", "Configuration saved successfully");
        return "redirect:/docs/admin";
    }

    /**
     * Quick access with passcode (redirects to admin if valid)
     * GET /docs/admin/quick?passcode=xxx
     */
    @GetMapping("/docs/admin/quick")
    public String quickAdminAccess(@RequestParam String passcode, RedirectAttributes redirectAttributes) {
        if (docsService.verifyAdminPasscode(passcode)) {
            return "redirect:/docs/admin";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid passcode");
        return "redirect:/docs";
    }
}

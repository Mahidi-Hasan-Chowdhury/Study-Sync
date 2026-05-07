package bd.edu.seu.studysync.controller;

import bd.edu.seu.studysync.model.User;
import bd.edu.seu.studysync.service.PaymentService;
import bd.edu.seu.studysync.service.UserService;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    @GetMapping("/pricing")
    public String pricing(Model model) {
        Optional<User> currentUser = userService.getCurrentUser();
        model.addAttribute("isPro", currentUser.isPresent() && currentUser.get().isPro());
        model.addAttribute("contentPage", "pricing");
        return "layout";
    }

    @GetMapping("/subscribe")
    public String subscribe() {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return "redirect:/login";
        }
        
        try {
            String checkoutUrl = paymentService.createCheckoutSession(currentUser.get().getId());
            return "redirect:" + checkoutUrl;
        } catch (Exception e) {
            return "redirect:/payment/pricing?error=" + e.getMessage();
        }
    }

    @GetMapping("/success")
    public String success(@RequestParam("session_id") String sessionId, Model model) {
        try {
            // Verify the session with Stripe
            Session session = paymentService.getSession(sessionId);
        
            if ("paid".equals(session.getPaymentStatus())) {
                String userId = session.getMetadata().get("userId");
                if (userId != null) {
                    userService.upgradeToPro(userId);
                    model.addAttribute("message", "Payment Successful! You are now a Pro member.");
                } else {
                    // Fallback to current user if metadata is missing (unlikely)
                    Optional<User> currentUser = userService.getCurrentUser();
                    if (currentUser.isPresent()) {
                        userService.upgradeToPro(currentUser.get().getId());
                        model.addAttribute("message", "Payment Successful! You are now a Pro member.");
                    }
                }
            } else {
                return "redirect:/payment/pricing?error=Payment not completed.";
            }
        } catch (Exception e) {
            return "redirect:/payment/pricing?error=" + e.getMessage();
        }

        model.addAttribute("contentPage", "payment-success");
        return "layout";
    }

    @GetMapping("/cancel")
    public String cancel(Model model) {
        model.addAttribute("error", "Payment cancelled.");
        return "redirect:/payment/pricing";
    }
}

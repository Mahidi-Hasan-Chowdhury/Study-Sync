package bd.edu.seu.studysync.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${stripe.api.key.secret}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public String createCheckoutSession(String userId) throws StripeException {
        String domainUrl = "http://localhost:8080"; // Change if deployed

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(domainUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(domainUrl + "/payment/cancel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(999L) // $9.99
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Study Sync Pro")
                                        .setDescription("Unlock 20MB Uploads & Multi-format Support")
                                        .build())
                                .build())
                        .build())
                .putMetadata("userId", userId)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    public Session getSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}

package bd.edu.seu.studysync.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String token = "";
        Cookie[] cookies = request.getCookies();
        
        // Extract JWT token from cookies
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("JWTtoken")) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        
        // If no token found, clear security context
        if (token.isEmpty()) {
            SecurityContextHolder.clearContext();
        } else {
            // Validate token and set authentication
            try {
                String username = jwtService.getUsername(token);
                String role = jwtService.getRole(token);
                
                // Check if token is expired
                if (jwtService.isTokenExpired(token)) {
                    SecurityContextHolder.clearContext();
                } else {
                    // Create authentication object
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            username, null, List.of(authority)
                    );
                    
                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException e) {
                // Invalid token - clear context
                SecurityContextHolder.clearContext();
            }
        }
        
        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}

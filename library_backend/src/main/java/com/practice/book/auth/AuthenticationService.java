package com.practice.book.auth;

import com.practice.book.email.EmailService;
import com.practice.book.email.EmailTemplateName;
import com.practice.book.role.RoleRepository;
import com.practice.book.security.JwtService;
import com.practice.book.token.Token;
import com.practice.book.token.TokenRepository;
import com.practice.book.user.User;
import com.practice.book.user.UserRepository;
import io.jsonwebtoken.Jwts;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    public void register(@Valid RegistrationRequest request) throws MessagingException {
        System.out.println("Registration request: " + request);
        var userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalArgumentException("Role user was not found"));
        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();
        userRepository.save(user);
        sentValidationEmail(user);
    }

    private void sentValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        emailService.sendEmail(user.getEmail(),
                user.getUsername(), EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,newToken, "Account Activation");
    }

    private String generateAndSaveActivationToken(User user) {
        //generate token
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepository.save(token);
        return generatedToken;
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword())
        );
        System.out.println("Authenticated user: " + auth.toString());
        var claim = new HashMap<String,Object>();
        var user = ((User) auth.getPrincipal());
        claim.put("username", user.getUsername());
        var jwtToken = jwtService.generateToken(claim,user);
        return AuthenticationResponse.builder()
                .token(jwtToken).build();
    }

    public void activateAccount(String request_token) throws Exception {
        Token token = tokenRepository.findByToken(request_token).orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if(LocalDateTime.now().isAfter(token.getExpiresAt())){
            sentValidationEmail(token.getUser());
            throw new RuntimeException("Activation token has expired. \n A new token has been sent to email address.");
        }
        var user = userRepository.findById(token.getUser().getId()).orElseThrow(() -> new IllegalArgumentException("Invalid user"));
        System.out.println("Activation token user: "+user.toString());
        user.setEnabled(true);
        userRepository.save(user);
        token.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }
}

package com.instagram.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contact form endpoint.
 *
 * <p>POST /api/contact with {@code {"name","email","message"}}.
 * Forwards the message by email when SMTP is configured
 * (see {@code SMTP_*} / {@code CONTACT_TO} env vars). Without SMTP
 * configuration it answers 503 with a clear message instead of failing.</p>
 */
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${app.contact.to:}")
    private String contactTo;

    @Value("${app.contact.from:}")
    private String contactFrom;

    public ContactController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> sendMessage(
            @RequestBody Map<String, String> request) {

        String name = text(request.get("name"));
        String email = text(request.get("email"));
        String subject = text(request.get("subject"));
        String message = text(request.get("message"));

        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Name, email and message are all required."));
        }

        if (name.length() > 100 || email.length() > 200
                || subject.length() > 200 || message.length() > 5000) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Message is too long. Keep it under 5000 characters."));
        }

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "That email address does not look valid."));
        }

        if (smtpHost.isBlank() || contactTo.isBlank()) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "error",
                    "message", "The contact form is not configured yet. Please email us directly."));
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(contactTo.trim());
            if (!contactFrom.isBlank()) {
                mail.setFrom(contactFrom.trim());
            }
            mail.setReplyTo(email);
            mail.setSubject(subject.isEmpty()
                    ? "Website contact from " + name
                    : "Website contact [" + subject + "] from " + name);
            mail.setText("Name: " + name + "\nEmail: " + email + "\n\n" + message);
            mailSender.send(mail);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Thanks! Your message has been sent."));
        } catch (Exception e) {
            System.err.println("[CONTACT] Mail send failed: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Could not send your message right now. Please try again later."));
        }
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}

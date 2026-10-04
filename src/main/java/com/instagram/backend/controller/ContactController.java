package com.instagram.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Contact form endpoint.
 *
 * <p>POST /api/contact with {@code {"name","email","subject","message"}}.
 * Delivers the message through the Resend HTTPS API (port 443), because
 * cloud hosts routinely block outbound SMTP ports. Needs
 * {@code RESEND_API_KEY} and {@code CONTACT_TO} env vars; without them it
 * answers 503 with a clear message instead of failing.</p>
 */
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${app.contact.to:}")
    private String contactTo;

    @Value("${app.contact.from:onboarding@resend.dev}")
    private String contactFrom;

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

        if (resendApiKey.isBlank() || contactTo.isBlank()) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "error",
                    "message", "The contact form is not configured yet. Please email us directly."));
        }

        try {
            String emailSubject = subject.isEmpty()
                    ? "Website contact from " + name
                    : "Website contact [" + subject + "] from " + name;
            String emailText = "Name: " + name + "\nEmail: " + email + "\n\n" + message;

            String body = JSON.writeValueAsString(Map.of(
                    "from", contactFrom.trim(),
                    "to", List.of(contactTo.trim()),
                    "subject", emailSubject,
                    "text", emailText,
                    "reply_to", email));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    http.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[CONTACT] Resend rejected the message: "
                        + response.statusCode() + " " + response.body());
                return ResponseEntity.internalServerError().body(Map.of(
                        "status", "error",
                        "message", "Could not send your message right now. Please try again later."));
            }

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

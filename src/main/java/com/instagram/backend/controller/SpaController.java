package com.instagram.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the merged React frontend (BrowserRouter) from the same port.
 *
 * <p>The Vite build output lives in {@code src/main/resources/static},
 * so {@code /}, {@code /assets/*} and {@code /favicon.svg} are served
 * as static resources automatically. Client-side routes below have no
 * server-side file and must fall back to {@code index.html} so React
 * Router can render them.</p>
 *
 * <p>If you add a new top-level page in {@code frontend/src/App.jsx},
 * add its path here as well.</p>
 */
@Controller
public class SpaController {

    @GetMapping({
        "/downloader",
        "/how-it-works",
        "/faq",
        "/about",
        "/privacy",
        "/terms",
        "/contact"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}

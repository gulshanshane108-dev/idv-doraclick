package com.instagram.backend.util;

import java.net.URI;
import java.util.Locale;

public final class UrlValidator {
    private UrlValidator() {}

    public static Platform detectPlatform(String url) {
        if (url == null || url.isBlank()) return Platform.UNKNOWN;
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null ||
                    !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return Platform.UNKNOWN;
            }
            host = host.toLowerCase(Locale.ROOT);
            if (host.equals("instagram.com") || host.endsWith(".instagram.com")) return Platform.INSTAGRAM;
            if (host.equals("youtube.com") || host.endsWith(".youtube.com") || host.equals("youtu.be")) return Platform.YOUTUBE;
            if (host.equals("facebook.com") || host.endsWith(".facebook.com") || host.equals("fb.watch")) return Platform.FACEBOOK;
            if (host.equals("tiktok.com") || host.endsWith(".tiktok.com")) return Platform.TIKTOK;
            if (host.equals("x.com") || host.endsWith(".x.com") || host.equals("twitter.com") || host.endsWith(".twitter.com")) return Platform.X;
            return Platform.UNKNOWN;
        } catch (Exception e) {
            return Platform.UNKNOWN;
        }
    }

    public static boolean isSupportedUrl(String url) {
        return detectPlatform(url) != Platform.UNKNOWN;
    }

    public static boolean isValidInstagramUrl(String url) {
        return detectPlatform(url) == Platform.INSTAGRAM;
    }
}

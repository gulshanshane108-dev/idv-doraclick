package com.instagram.backend.util;

public enum Platform {
    INSTAGRAM("Instagram"),
    YOUTUBE("YouTube"),
    FACEBOOK("Facebook"),
    TIKTOK("TikTok"),
    X("X"),
    UNKNOWN("Unknown");

    private final String displayName;

    Platform(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}

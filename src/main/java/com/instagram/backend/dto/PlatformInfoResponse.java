package com.instagram.backend.dto;

import java.util.List;

public class PlatformInfoResponse {
    private String platform;
    private String platformName;
    private boolean supported;
    private String message;
    private List<String> supportedPlatforms;

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }
    public boolean isSupported() { return supported; }
    public void setSupported(boolean supported) { this.supported = supported; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getSupportedPlatforms() { return supportedPlatforms; }
    public void setSupportedPlatforms(List<String> supportedPlatforms) { this.supportedPlatforms = supportedPlatforms; }
}

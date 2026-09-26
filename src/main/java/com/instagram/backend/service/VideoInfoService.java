package com.instagram.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instagram.backend.dto.VideoInfoResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VideoInfoService {
    private final YtDlpService ytDlpService;
    private final ObjectMapper objectMapper;

    public VideoInfoService(YtDlpService ytDlpService, ObjectMapper objectMapper) {
        this.ytDlpService = ytDlpService;
        this.objectMapper = objectMapper;
    }

    public VideoInfoResponse getVideoInfo(String url) {
        try {
            JsonNode root = objectMapper.readTree(ytDlpService.getVideoInfo(url));
            VideoInfoResponse response = new VideoInfoResponse();
            if (root.has("id")) response.setId(root.get("id").asText());
            if (root.has("title")) response.setTitle(root.get("title").asText());
            if (root.has("description")) response.setDescription(root.get("description").asText());
            if (root.has("uploader")) response.setUploader(root.get("uploader").asText());
            if (root.has("channel")) response.setChannel(root.get("channel").asText());
            if (root.has("thumbnail")) response.setThumbnail(root.get("thumbnail").asText());
            if (root.has("duration")) response.setDuration(root.get("duration").asInt());
            if (root.has("width")) response.setWidth(root.get("width").asInt());
            if (root.has("height")) response.setHeight(root.get("height").asInt());
            if (root.has("formats")) response.setFormats(objectMapper.convertValue(root.get("formats"), List.class));
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Unable to get media metadata: " + e.getMessage(), e);
        }
    }
}

package com.example.fakenewsdetector.dto;

import jakarta.validation.constraints.Size;

public class AnalysisRequest {

    @Size(max = 50000, message = "Input text must be less than 50,000 characters.")
    private String text;

    @Size(max = 2083, message = "URL must be less than 2083 characters.")
    private String url;

    private String inputType; // "TEXT" or "URL"

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }
}

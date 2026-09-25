package com.example.fakenewsdetector.service;

import com.example.fakenewsdetector.exception.UrlExtractionException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;

@Service
public class UrlExtractionService {

    public static class ExtractedWebPage {
        private String title;
        private String bodyText;
        private String domain;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBodyText() { return bodyText; }
        public void setBodyText(String bodyText) { this.bodyText = bodyText; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
    }

    /**
     * Connects to a URL, validates structure, cleans boilerplates, and returns clean text content.
     */
    public ExtractedWebPage extract(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new UrlExtractionException("URL cannot be empty.");
        }

        // Validate URL syntax
        URL url;
        try {
            // Using URI to URL conversion as URL constructor is deprecated in newer Java versions
            url = URI.create(urlString.trim()).toURL();
            String protocol = url.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                throw new UrlExtractionException("Only HTTP and HTTPS protocols are supported.");
            }
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new UrlExtractionException("The provided URL format is invalid: " + urlString);
        }

        try {
            // Fetch and parse webpage
            Document doc = Jsoup.connect(urlString)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(8000)
                    .followRedirects(true)
                    .get();

            String title = doc.title();
            
            // Clean boilerplates
            doc.select("nav, footer, header, script, style, iframe, ads, .ads, .menu, .sidebar, .comments, #footer, #header, #navigation, noscript").remove();

            // Extract primary article content if available
            String bodyText = "";
            Element articleElement = doc.select("article, main, .post-content, .article-content, .story-body").first();
            if (articleElement != null) {
                bodyText = articleElement.text();
            }
            
            if (bodyText.trim().isEmpty()) {
                bodyText = doc.body().text();
            }

            if (bodyText.trim().isEmpty()) {
                throw new UrlExtractionException("Failed to extract readable text content from the webpage.");
            }

            // Truncate extremely large text to prevent memory overload
            if (bodyText.length() > 20000) {
                bodyText = bodyText.substring(0, 20000);
            }

            ExtractedWebPage result = new ExtractedWebPage();
            result.setTitle(title != null && !title.trim().isEmpty() ? title.trim() : url.getHost());
            result.setBodyText(bodyText.trim());
            result.setDomain(url.getHost());
            return result;

        } catch (java.net.SocketTimeoutException e) {
            throw new UrlExtractionException("Connection timed out. The server took too long to respond.");
        } catch (java.net.UnknownHostException e) {
            throw new UrlExtractionException("Unable to resolve the host name. Please check your internet connection or the URL domain.");
        } catch (Exception e) {
            throw new UrlExtractionException("Failed to retrieve or parse URL content: " + e.getMessage(), e);
        }
    }
}

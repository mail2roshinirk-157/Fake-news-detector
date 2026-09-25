package com.example.fakenewsdetector;

import com.example.fakenewsdetector.dto.AnalysisRequest;
import com.example.fakenewsdetector.dto.AnalysisResponse;
import com.example.fakenewsdetector.dto.DashboardStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AnalysisApiControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testAnalyzeEndpoint() {
        AnalysisRequest request = new AnalysisRequest();
        request.setText("The Department of Energy announced funding for new research. Stated by officials, these hubs will support solar efficiency today.");
        request.setInputType("TEXT");

        ResponseEntity<AnalysisResponse> response = restTemplate.postForEntity("/api/analyze", request, AnalysisResponse.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        AnalysisResponse body = response.getBody();
        assertNotNull(body.getId());
        assertEquals("LIKELY_GENUINE", body.getPrediction());
        assertTrue(body.getConfidenceScore() >= 60.0);
    }

    @Test
    public void testDashboardEndpoint() {
        ResponseEntity<DashboardStats> response = restTemplate.getForEntity("/api/dashboard", DashboardStats.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        DashboardStats stats = response.getBody();
        assertTrue(stats.getTotalAnalyses() >= 0);
    }

    @Test
    public void testHealthEndpoint() {
        ResponseEntity<Object> response = restTemplate.getForEntity("/api/health", Object.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}

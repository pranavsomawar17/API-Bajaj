package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    public void run(String... args) {
        executeFlow();
    }

    public void executeFlow() {

        WebClient client = WebClient.create();

        // ✅ Step 1: Generate webhook + token
        Map<String, String> request = new HashMap<>();
        request.put("name", "John Doe");
        request.put("regNo", "REG12347");
        request.put("email", "john@example.com");

        Map response = client.post()
                .uri("https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String webhook = (String) response.get("webhook");
        String token = (String) response.get("accessToken");

        System.out.println("Webhook URL: " + webhook);
        System.out.println("Token: " + token);

        // ✅ Step 2: Get SQL Query
        String finalQuery = getSQLQuery();

        // ✅ Step 3: Send result
        sendResult(webhook, token, finalQuery);
    }

    // ✅ SQL QUERY
    public String getSQLQuery() {
        return "SELECT p.AMOUNT AS SALARY, " +
                "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, " +
                "d.DEPARTMENT_NAME " +
                "FROM PAYMENTS p " +
                "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
                "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                "WHERE DAY(p.PAYMENT_TIME) <> 1 " +
                "ORDER BY p.AMOUNT DESC " +
                "LIMIT 1;";
    }

    // ✅ FINAL FIXED METHOD
    private void sendResult(String url, String token, String finalQuery) {

        WebClient webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", token) // ✅ NO "Bearer"
                .defaultHeader("Content-Type", "application/json")
                .build();

        // ✅ Wrap query in JSON body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("finalQuery", finalQuery);

        try {
            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("✅ SUCCESS RESPONSE:");
            System.out.println(response);

        } catch (WebClientResponseException e) {
            System.out.println("❌ ERROR RESPONSE:");
            System.out.println("Status Code: " + e.getStatusCode());
            System.out.println("Body: " + e.getResponseBodyAsString());
        }
    }
}
package com.madhurgram.productservice.bundle.service;

import com.madhurgram.productservice.bundle.dto.BundleCopyRequestDTO;
import com.madhurgram.productservice.bundle.dto.BundleCopyResponseDTO;
import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BundleCopyGeneratorService {

    private final ProductRepository productRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public BundleCopyResponseDTO generateCopy(BundleCopyRequestDTO request) {
        List<Product> products = productRepository.findAllById(request.productIds());
        
        if (products.isEmpty()) {
            return new BundleCopyResponseDTO("New Bundle", "Custom Bundle", "A curated selection of our finest products.");
        }

        if (request.engine() == BundleCopyRequestDTO.EngineType.AI) {
            return generateUsingAI(products);
        } else {
            return generateUsingRules(products);
        }
    }

    private BundleCopyResponseDTO generateUsingRules(List<Product> products) {
        boolean hasGhee = products.stream().anyMatch(p -> p.getName().toLowerCase().contains("ghee"));
        boolean hasJaggery = products.stream().anyMatch(p -> p.getName().toLowerCase().contains("jaggery") || p.getName().toLowerCase().contains("gur"));
        boolean hasOil = products.stream().anyMatch(p -> p.getName().toLowerCase().contains("oil"));
        boolean hasPickle = products.stream().anyMatch(p -> p.getName().toLowerCase().contains("pickle"));

        if (hasGhee && hasJaggery && !hasOil && !hasPickle) {
            return new BundleCopyResponseDTO(
                    "Sweet & Pure",
                    "A2 Ghee & Village Jaggery Wellness Bundle",
                    "Replace refined sugar and processed ghee. Pure A2 Bilona Ghee churned from curd and organic village Jaggery (Gur) — the traditional combination for immunity and morning chai."
            );
        } else if (hasGhee && hasOil && hasPickle) {
            return new BundleCopyResponseDTO(
                    "Rasoi Essentials",
                    "The Complete Traditional Kitchen Bundle",
                    "Everything your kitchen needs — A2 Bilona Ghee for parathas, cold-pressed Mustard Oil for daily cooking, and handcrafted Mango Pickle to complete every meal."
            );
        } else if (hasOil && hasPickle && !hasGhee) {
            return new BundleCopyResponseDTO(
                    "Pickle Pack",
                    "Cold-Pressed Oil & Artisanal Pickle Box",
                    "The two pantry heroes — stone cold-pressed oil and our sun-cured pickle handmade in small batches. Perfect for authentic Indian flavours."
            );
        } else if (hasGhee) {
            return new BundleCopyResponseDTO(
                    "Purity Box",
                    "Premium A2 Ghee Combo",
                    "Experience the finest Bilona churned A2 Ghee paired with our premium village products. Crafted with tradition for your family's health."
            );
        }

        // Generic Fallback
        String productNames = products.stream().map(Product::getName).collect(Collectors.joining(" & "));
        return new BundleCopyResponseDTO(
                "Premium Combo",
                "MadhurGram Artisanal Bundle",
                "A carefully curated bundle featuring " + productNames + ". Handcrafted in the village of Gopiganj, bringing authentic taste and purity straight to your home."
        );
    }

    private BundleCopyResponseDTO generateUsingAI(List<Product> products) {
        String productNames = products.stream().map(Product::getName).collect(Collectors.joining(" + "));
        log.info("Calling Gemini AI for products: {}", productNames);
        
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + geminiApiKey;
        
        String prompt = "You are an expert luxury e-commerce copywriter. Write a premium bundle copy for the following products: " + productNames + 
                        ". Respond strictly in valid JSON format with exactly three keys: 'tabName' (2-3 words, catchy short name), 'name' (premium full bundle name), and 'description' (2-3 sentences explaining the benefits and premium quality). Do not include markdown formatting or backticks around the JSON.";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(
                Map.of("parts", Collections.singletonList(
                        Map.of("text", prompt)
                ))
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
            // Clean up if Gemini returns markdown JSON
            if (text.startsWith("```json")) {
                text = text.substring(7);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            
            JsonNode jsonResponse = objectMapper.readTree(text.trim());
            
            return new BundleCopyResponseDTO(
                    jsonResponse.path("tabName").asText("✨ Smart Bundle"),
                    jsonResponse.path("name").asText("Curated AI Selection"),
                    jsonResponse.path("description").asText("Experience the ultimate harmony of flavors with this exclusive selection.")
            );

        } catch (Exception e) {
            log.error("Failed to generate AI copy, falling back to Rules", e);
            return generateUsingRules(products);
        }
    }
}

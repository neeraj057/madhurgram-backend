package com.madhurgram.productservice.marketing.service;

import com.madhurgram.productservice.marketing.dto.ReviewRequestDTO;
import java.util.List;

public interface ReviewService {
    List<ReviewRequestDTO> getReviewQueue();
    ReviewRequestDTO sendNow(Long id);
    ReviewRequestDTO sendTest(String name, String phone);
}

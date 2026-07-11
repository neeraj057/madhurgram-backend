package com.madhurgram.productservice.returns.service;

import com.madhurgram.productservice.returns.entity.ReturnRequest;
import java.util.List;

public interface ReturnService {
    ReturnRequest createReturnRequest(Long orderId, String phone, String reason);
    ReturnRequest getReturnRequestByOrderId(Long orderId);
    List<ReturnRequest> getAllReturnRequests();
    ReturnRequest approveReturnRequest(Long returnId);
    ReturnRequest rejectReturnRequest(Long returnId);
}

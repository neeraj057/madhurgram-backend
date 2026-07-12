package com.madhurgram.productservice.returns.service;

import com.madhurgram.productservice.returns.dto.ReturnRequestDTO;
import java.util.List;

public interface ReturnService {
    ReturnRequestDTO createReturnRequest(Long orderId, String phone, String reason);
    ReturnRequestDTO getReturnRequestByOrderId(Long orderId);
    List<ReturnRequestDTO> getAllReturnRequests();
    ReturnRequestDTO approveReturnRequest(Long returnId);
    ReturnRequestDTO rejectReturnRequest(Long returnId);

    String generateShippingLabelSvg(Long returnRequestId);
}

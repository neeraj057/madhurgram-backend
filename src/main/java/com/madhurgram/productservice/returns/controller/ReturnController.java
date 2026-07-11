package com.madhurgram.productservice.returns.controller;

import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.returns.entity.ReturnRequest;
import com.madhurgram.productservice.returns.service.ReturnService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/returns")
@CrossOrigin(origins = "*")
public class ReturnController {

    private final ReturnService returnService;
    private final OrderRepository orderRepository;

    public ReturnController(ReturnService returnService, OrderRepository orderRepository) {
        this.returnService = returnService;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<?> requestReturn(
            @RequestParam Long orderId,
            @RequestParam String phone,
            @RequestParam String reason
    ) {
        try {
            ReturnRequest request = returnService.createReturnRequest(orderId, phone, reason);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ReturnRequest> getReturnRequest(@PathVariable Long orderId) {
        ReturnRequest request = returnService.getReturnRequestByOrderId(orderId);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(request);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<ReturnRequest>> getAllReturnRequests() {
        return ResponseEntity.ok(returnService.getAllReturnRequests());
    }

    @PostMapping("/admin/{id}/approve")
    public ResponseEntity<?> approveReturn(@PathVariable Long id) {
        try {
            ReturnRequest approved = returnService.approveReturnRequest(id);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/admin/{id}/reject")
    public ResponseEntity<?> rejectReturn(@PathVariable Long id) {
        try {
            ReturnRequest rejected = returnService.rejectReturnRequest(id);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/label/{id}", produces = "image/svg+xml")
    public ResponseEntity<String> getShippingLabel(@PathVariable Long id) {
        ReturnRequest request = returnService.getAllReturnRequests().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        Order order = orderRepository.findById(request.getOrderId()).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("<svg><text y=\"20\">Order not found</text></svg>");
        }

        String labelDate = request.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String customerName = order.getCustomerName();
        String addressLine1 = order.getAddress();
        String cityState = order.getCityState();
        String pincode = order.getPincode();
        String phone = order.getPhoneNumber();
        Long orderId = order.getId();

        // Beautiful SVG string with a mock barcode
        String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 400 550\" width=\"400\" height=\"550\">\n" +
                "  <rect x=\"10\" y=\"10\" width=\"380\" height=\"530\" fill=\"#FFFFFF\" stroke=\"#000000\" stroke-width=\"3\" rx=\"10\"/>\n" +
                "  <rect x=\"10\" y=\"10\" width=\"380\" height=\"70\" fill=\"#111111\" rx=\"10\"/>\n" +
                "  <text x=\"200\" y=\"45\" font-family=\"sans-serif\" font-size=\"20\" font-weight=\"900\" fill=\"#D4AF37\" text-anchor=\"middle\" letter-spacing=\"1\">MADHURGRAM RETURNING</text>\n" +
                "  <text x=\"200\" y=\"63\" font-family=\"sans-serif\" font-size=\"9\" fill=\"#FAF9F5\" text-anchor=\"middle\">PREPAID RETURN SHIPPING LABEL</text>\n" +
                "  <text x=\"25\" y=\"110\" font-family=\"monospace\" font-size=\"14\" font-weight=\"bold\">CARRIER: DELHIVERY / SPEED POST</text>\n" +
                "  <text x=\"25\" y=\"130\" font-family=\"monospace\" font-size=\"12\">TRACKING: MG-RET-" + id + "-" + orderId + "</text>\n" +
                "  <text x=\"25\" y=\"150\" font-family=\"monospace\" font-size=\"12\">DATE: " + labelDate + "</text>\n" +
                "  <line x1=\"10\" y1=\"170\" x2=\"390\" y2=\"170\" stroke=\"#000000\" stroke-width=\"2\"/>\n" +
                "  <rect x=\"25\" y=\"185\" width=\"65\" height=\"18\" fill=\"#000000\" rx=\"3\"/>\n" +
                "  <text x=\"575\" y=\"198\" font-family=\"sans-serif\" font-size=\"10\" font-weight=\"bold\" fill=\"#FFFFFF\" text-anchor=\"middle\">SHIP TO</text>\n" +
                "  <!-- Correct position for text -->\n" +
                "  <text x=\"57\" y=\"198\" font-family=\"sans-serif\" font-size=\"9\" font-weight=\"bold\" fill=\"#FFFFFF\" text-anchor=\"middle\">SHIP TO</text>\n" +
                "  <text x=\"25\" y=\"225\" font-family=\"sans-serif\" font-size=\"12\" font-weight=\"bold\">MadhurGram Return Fulfillment Center</text>\n" +
                "  <text x=\"25\" y=\"240\" font-family=\"sans-serif\" font-size=\"11\">Plot 45, Sector B, Industrial Area</text>\n" +
                "  <text x=\"25\" y=\"255\" font-family=\"sans-serif\" font-size=\"11\">Indore, Madhya Pradesh - 452001</text>\n" +
                "  <text x=\"25\" y=\"270\" font-family=\"sans-serif\" font-size=\"11\">Contact: +91 98765 43210</text>\n" +
                "  <line x1=\"20\" y1=\"285\" x2=\"380\" y2=\"285\" stroke=\"#CCCCCC\" stroke-width=\"1\" stroke-dasharray=\"5,5\"/>\n" +
                "  <text x=\"25\" y=\"310\" font-family=\"sans-serif\" font-size=\"11\" font-weight=\"bold\" fill=\"#555555\">FROM (SENDER):</text>\n" +
                "  <text x=\"25\" y=\"325\" font-family=\"sans-serif\" font-size=\"12\" font-weight=\"bold\">" + customerName + "</text>\n" +
                "  <text x=\"25\" y=\"340\" font-family=\"sans-serif\" font-size=\"11\">" + addressLine1 + "</text>\n" +
                "  <text x=\"25\" y=\"355\" font-family=\"sans-serif\" font-size=\"11\">" + cityState + " - " + pincode + "</text>\n" +
                "  <text x=\"25\" y=\"370\" font-family=\"sans-serif\" font-size=\"11\">Phone: " + phone + "</text>\n" +
                "  <line x1=\"10\" y1=\"390\" x2=\"390\" y2=\"390\" stroke=\"#000000\" stroke-width=\"2\"/>\n" +
                "  <g transform=\"translate(55, 405)\">\n" +
                "    <rect x=\"0\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"5\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"8\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"15\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"20\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"25\" y=\"0\" width=\"5\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"33\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"38\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"43\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"48\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"55\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"60\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"65\" y=\"0\" width=\"6\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"75\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"80\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"85\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"92\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"98\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"104\" y=\"0\" width=\"5\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"112\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"117\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"122\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"128\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"135\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"140\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"145\" y=\"0\" width=\"6\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"155\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"160\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"165\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"172\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"178\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"184\" y=\"0\" width=\"5\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"192\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"197\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"202\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"208\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"215\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"220\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"225\" y=\"0\" width=\"6\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"235\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"240\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"245\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"252\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"258\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"264\" y=\"0\" width=\"5\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"272\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"277\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"282\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "  </g>\n" +
                "  <text x=\"200\" y=\"477\" font-family=\"monospace\" font-size=\"12\" font-weight=\"bold\" text-anchor=\"middle\">*MG-RET-" + id + "-" + orderId + "*</text>\n" +
                "  <line x1=\"10\" y1=\"495\" x2=\"390\" y2=\"495\" stroke=\"#000000\" stroke-width=\"1\"/>\n" +
                "  <text x=\"200\" y=\"515\" font-family=\"sans-serif\" font-size=\"8\" fill=\"#666666\" font-style=\"italic\" text-anchor=\"middle\">Instructions: 1. Pack items securely. 2. Affix this label to the box. 3. Drop it off at your nearest speed post hub.</text>\n" +
                "</svg>";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/svg+xml"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"return_label_" + id + ".svg\"");
        return new ResponseEntity<>(svg, headers, HttpStatus.OK);
    }
}

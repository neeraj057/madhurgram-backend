package com.madhurgram.productservice.order.service;

import com.madhurgram.productservice.cart.service.TwilioService;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationService.class);
    private final TwilioService twilioService;

    public OrderNotificationService(TwilioService twilioService) {
        this.twilioService = twilioService;
    }

    public void sendOrderStatusNotification(Order order, OrderStatus nextStatus) {
        if (order == null || order.getPhoneNumber() == null) {
            log.warn("Cannot send order status notification: Order details are empty.");
            return;
        }

        String customer = order.getCustomerName() != null ? order.getCustomerName().trim() : "ग्राहक";
        String orderIdStr = "MG-000" + order.getId();
        String message = "";

        switch (nextStatus) {
            case CONFIRMED:
                message = "नमस्ते " + customer + "! आपका MadhurGram ऑर्डर (ID: " + orderIdStr + 
                          ") कन्फर्म हो गया है। हम इसे जल्द ही पैक करके शिप करेंगे। धन्यवाद, टीम MadhurGram 💛";
                break;
            case SHIPPED:
                message = "नमस्ते " + customer + "! आपका MadhurGram ऑर्डर (ID: " + orderIdStr + 
                          ") शिप हो गया है। आपका ट्रैकिंग लिंक: http://localhost:3000/orders/track/" + order.getId() + 
                          " है। धन्यवाद, टीम MadhurGram 💛";
                break;
            case OUT_FOR_DELIVERY:
                message = "नमस्ते " + customer + "! आपका MadhurGram ऑर्डर (ID: " + orderIdStr + 
                          ") आउट फॉर डिलीवरी है। हमारा डिलीवरी पार्टनर जल्द ही आपसे संपर्क करेगा। धन्यवाद, टीम MadhurGram 💛";
                break;
            case DELIVERED:
                message = "नमस्ते " + customer + "! आपका MadhurGram ऑर्डर (ID: " + orderIdStr + 
                          ") सफलतापूर्वक डिलीवर हो गया है। हमें आशा है कि आपको हमारे शुद्ध उत्पाद पसंद आएंगे। कृपया अपना फीडबैक यहाँ दें: http://localhost:3000/feedback। धन्यवाद, टीम MadhurGram 💛";
                break;
            case CANCELLED:
                message = "नमस्ते " + customer + "! आपका MadhurGram ऑर्डर (ID: " + orderIdStr + 
                          ") कैंसिल कर दिया गया है। यदि आपने एडवांस पेमेंट किया था, तो रिफंड 3-5 दिनों में प्रोसेस हो जाएगा। धन्यवाद, टीम MadhurGram 💛";
                break;
            default:
                // No notification for PENDING (as checkout order is placed screen itself acts as confirmation)
                return;
        }

        try {
            log.info("Dispatching WhatsApp status notification ({}) to phone: {}", nextStatus, order.getPhoneNumber());
            twilioService.sendWhatsAppMessage(order.getPhoneNumber(), message);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp status notification for order ID: {}. Error: {}", order.getId(), e.getMessage());
        }
    }
}

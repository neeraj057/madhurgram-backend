package com.madhurgram.productservice.order.service;

import com.madhurgram.productservice.cart.service.TwilioService;
import com.madhurgram.productservice.common.repository.SystemSettingRepository;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service implementation for managing order notifications.
 * Sends WhatsApp updates dynamically using templates loaded from database system settings.
 */
@Slf4j
@Service
public class OrderNotificationService {

    private static final String DEFAULT_CUSTOMER_NAME = "ग्राहक";
    private static final String ORDER_ID_PREFIX = "MG-000";

    // Java-level constant defaults to keep properties clean
    private static final String DEFAULT_TEMPLATE_CONFIRMED = 
            "नमस्ते {customer}! आपका MadhurGram ऑर्डर (ID: {orderId}) कन्फर्म हो गया है। हम इसे जल्द ही पैक करके शिप करेंगे। धन्यवाद, टीम MadhurGram 💛";
    private static final String DEFAULT_TEMPLATE_SHIPPED = 
            "नमस्ते {customer}! आपका MadhurGram ऑर्डर (ID: {orderId}) शिप हो गया है। आपका ट्रैकिंग लिंक: {domain}/orders/track/{id} है। धन्यवाद, टीम MadhurGram 💛";
    private static final String DEFAULT_TEMPLATE_OUT_FOR_DELIVERY = 
            "नमस्ते {customer}! आपका MadhurGram ऑर्डर (ID: {orderId}) आउट फॉर डिलीवरी है। हमारा डिलीवरी पार्टनर जल्द ही आपसे संपर्क करेगा। धन्यवाद, टीम MadhurGram 💛";
    private static final String DEFAULT_TEMPLATE_DELIVERED = 
            "नमस्ते {customer}! आपका MadhurGram ऑर्डर (ID: {orderId}) सफलतापूर्वक डिलीवर हो गया है। हमें आशा है कि आपको हमारे शुद्ध उत्पाद पसंद आएंगे। कृपया अपना फीडबैक यहाँ दें: {domain}/feedback?orderId={id}। धन्यवाद, टीम MadhurGram 💛";
    private static final String DEFAULT_TEMPLATE_CANCELLED = 
            "नमस्ते {customer}! आपका MadhurGram ऑर्डर (ID: {orderId}) कैंसिल कर दिया गया है। यदि आपने एडवांस पेमेंट किया था, तो रिफंड 3-5 दिनों में प्रोसेस हो जाएगा। धन्यवाद, टीम MadhurGram 💛";

    private final TwilioService twilioService;
    private final SystemSettingRepository systemSettingRepository;
    private final String domainUrl;

    /**
     * Constructor injection for OrderNotificationService.
     *
     * @param twilioService           SMS/WhatsApp messaging client
     * @param systemSettingRepository global system settings repository
     * @param domainUrl               frontend website domain URL
     */
    public OrderNotificationService(
            TwilioService twilioService,
            SystemSettingRepository systemSettingRepository,
            @Value("${madhurgram.app.domain-url:http://localhost:3000}") String domainUrl
    ) {
        this.twilioService = twilioService;
        this.systemSettingRepository = systemSettingRepository;
        this.domainUrl = domainUrl;
    }

    /**
     * Dispatches order status transition update messages to the customer phone number.
     *
     * @param order      target active order
     * @param nextStatus newly applied order status
     */
    public void sendOrderStatusNotification(Order order, OrderStatus nextStatus) {
        if (order == null || order.getPhoneNumber() == null) {
            log.warn("Cannot send order status notification: Order details are empty.");
            return;
        }

        String customer = order.getCustomerName() != null ? order.getCustomerName().trim() : DEFAULT_CUSTOMER_NAME;
        String orderIdStr = ORDER_ID_PREFIX + order.getId();
        
        String template = getNotificationTemplate(nextStatus);
        if (template == null || template.trim().isEmpty()) {
            // No template found, or status transition doesn't trigger notification (e.g., PENDING)
            return;
        }

        // Format template by replacing variables
        String message = template
                .replace("{customer}", customer)
                .replace("{orderId}", orderIdStr)
                .replace("{domain}", domainUrl)
                .replace("{id}", String.valueOf(order.getId()));

        try {
            log.info("Dispatching WhatsApp status notification ({}) to phone: {}", nextStatus, order.getPhoneNumber());
            twilioService.sendWhatsAppMessage(order.getPhoneNumber(), message);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp status notification for order ID: {}. Error: {}", order.getId(), e.getMessage());
        }
    }

    private String getNotificationTemplate(OrderStatus status) {
        switch (status) {
            case CONFIRMED:
                return getTemplateFromDb("ORDER_TEMPLATE_CONFIRMED", DEFAULT_TEMPLATE_CONFIRMED);
            case SHIPPED:
                return getTemplateFromDb("ORDER_TEMPLATE_SHIPPED", DEFAULT_TEMPLATE_SHIPPED);
            case OUT_FOR_DELIVERY:
                return getTemplateFromDb("ORDER_TEMPLATE_OUT_FOR_DELIVERY", DEFAULT_TEMPLATE_OUT_FOR_DELIVERY);
            case DELIVERED:
                return getTemplateFromDb("ORDER_TEMPLATE_DELIVERED", DEFAULT_TEMPLATE_DELIVERED);
            case CANCELLED:
                return getTemplateFromDb("ORDER_TEMPLATE_CANCELLED", DEFAULT_TEMPLATE_CANCELLED);
            default:
                return null;
        }
    }

    private String getTemplateFromDb(String settingKey, String defaultTemplate) {
        return systemSettingRepository.findById(settingKey)
                .map(com.madhurgram.productservice.common.entity.SystemSetting::getSettingValue)
                .orElse(defaultTemplate);
    }
}

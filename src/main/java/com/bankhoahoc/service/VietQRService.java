package com.bankhoahoc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
public class VietQRService {

    private static final Logger logger = LoggerFactory.getLogger(VietQRService.class);

    @Value("${vietqr.api.url:https://img.vietqr.io/image/}")
    private String vietQRBaseUrl;

    @Value("${payment.qr.bank-code:970422}")
    private String bankCode; // Mã ngân hàng (BIN): MBBank = 970422

    @Value("${payment.qr.account-number}")
    private String accountNumber;

    @Value("${payment.qr.account-name}")
    private String accountName;

    private final RestTemplate restTemplate;

    public VietQRService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Generate QR code image URL từ VietQR.io
     * Format: https://img.vietqr.io/image/{bankCode}-{accountNo}-compact2.png?amount={amount}&addInfo={addInfo}&accountName={accountName}
     */
    public String generateQRCodeImageUrl(BigDecimal amount, String addInfo) {
        try {
            // Format số tiền: loại bỏ dấu chấm thập phân
            String amountStr = amount.toPlainString();
            if (amountStr.contains(".")) {
                amountStr = String.format("%.0f", amount.doubleValue());
            }

            // URL format: https://img.vietqr.io/image/{bankCode}-{accountNo}-compact2.png?amount={amount}&addInfo={addInfo}&accountName={accountName}
            String qrImageUrl = String.format("%s%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                    vietQRBaseUrl,
                    bankCode,
                    accountNumber,
                    amountStr,
                    java.net.URLEncoder.encode(addInfo != null ? addInfo : "", "UTF-8"),
                    java.net.URLEncoder.encode(accountName, "UTF-8"));

            logger.info("Generated VietQR URL: {}", qrImageUrl);
            return qrImageUrl;
        } catch (Exception e) {
            logger.error("Error generating VietQR URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate VietQR URL", e);
        }
    }

    /**
     * Lấy QR code image dạng Base64 từ URL
     */
    public String getQRCodeImageAsBase64(String imageUrl) {
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                byte[] imageBytes = response.getBody();
                String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                return "data:image/png;base64," + base64;
            } else {
                throw new RuntimeException("Failed to fetch QR code image from VietQR");
            }
        } catch (Exception e) {
            logger.error("Error fetching QR code image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch QR code image", e);
        }
    }
}

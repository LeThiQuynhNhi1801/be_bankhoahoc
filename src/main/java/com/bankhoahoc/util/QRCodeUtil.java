package com.bankhoahoc.util;

import com.bankhoahoc.service.VietQRService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Hashtable;

@Component
public class QRCodeUtil {

    @Value("${payment.qr.bank-name}")
    private String bankName;

    @Value("${payment.qr.account-number}")
    private String accountNumber;

    @Value("${payment.qr.account-name}")
    private String accountName;

    @Value("${payment.qr.template:NH:%s|STK:%s|ST:%s}")
    private String qrTemplate;

    @Value("${vietqr.enabled:true}")
    private boolean vietQREnabled;

    @Autowired(required = false)
    private VietQRService vietQRService;

    /**
     * Tạo nội dung QR code theo chuẩn Việt Nam
     * Hỗ trợ nhiều format:
     * 1. Format đơn giản: STK|ST (chỉ số tài khoản và số tiền)
     * 2. Format đầy đủ: NH|STK|ST|ND
     * 3. Format chỉ số tài khoản (không có số tiền) cho một số app
     */
    public String generateQRContent(BigDecimal amount, String orderNumber) {
        // Format số tiền: loại bỏ dấu chấm thập phân
        String amountStr = amount.toPlainString();
        if (amountStr.contains(".")) {
            amountStr = String.format("%.0f", amount.doubleValue());
        }
        
        // Nội dung thanh toán
        String content = String.format("Thanh toan don hang %s", orderNumber);
        
        // Kiểm tra template để quyết định format
        String qrContent;
        
        if (qrTemplate.contains("%s") && qrTemplate.split("%s").length == 3) {
            // Template có 3 placeholder: NH, STK, ST (bỏ ND)
            qrContent = String.format(qrTemplate, bankName, accountNumber, amountStr);
        } else if (qrTemplate.contains("%s") && qrTemplate.split("%s").length == 2) {
            // Template có 2 placeholder: STK, ST
            qrContent = String.format(qrTemplate, accountNumber, amountStr);
        } else {
            // Template có 4 placeholder: NH, STK, ST, ND (mặc định)
            qrContent = String.format(qrTemplate, bankName, accountNumber, amountStr, content);
        }
        
        // Log để debug
        System.out.println("Generated QR Content: " + qrContent);
        System.out.println("Template used: " + qrTemplate);
        System.out.println("Amount: " + amountStr + ", Account: " + accountNumber);
        
        return qrContent;
    }

    /**
     * Generate QR code image as Base64 string
     * Sử dụng VietQR.io nếu enabled, nếu không thì tự generate
     */
    public String generateQRCodeImage(String qrContent, int width, int height) {
        // Nếu sử dụng VietQR.io
        if (vietQREnabled && vietQRService != null) {
            try {
                // Extract amount từ qrContent hoặc truyền vào từ bên ngoài
                // Tạm thời sử dụng URL trực tiếp
                String addInfo = "Thanh toan don hang";
                if (qrContent.contains("|ND:")) {
                    String[] parts = qrContent.split("\\|ND:");
                    if (parts.length > 1) {
                        addInfo = parts[1];
                    }
                }
                
                // Extract amount từ qrContent
                BigDecimal amount = BigDecimal.ZERO;
                if (qrContent.contains("|ST:")) {
                    try {
                        String[] stParts = qrContent.split("\\|ST:");
                        if (stParts.length > 1) {
                            String amountStr = stParts[1].split("\\|")[0];
                            amount = new BigDecimal(amountStr);
                        }
                    } catch (Exception e) {
                        System.out.println("Could not extract amount from QR content, using default");
                    }
                }
                
                String imageUrl = vietQRService.generateQRCodeImageUrl(amount, addInfo);
                return vietQRService.getQRCodeImageAsBase64(imageUrl);
            } catch (Exception e) {
                System.out.println("Error using VietQR service, falling back to local generation: " + e.getMessage());
                // Fallback to local generation
            }
        }
        
        // Local QR code generation (fallback)
        try {
            // Log QR content để debug
            System.out.println("Generating QR Code Image locally from content: " + qrContent);
            
            Hashtable<EncodeHintType, Object> hintMap = new Hashtable<>();
            // Tăng error correction level để QR code dễ đọc hơn
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hintMap.put(EncodeHintType.MARGIN, 2); // Tăng margin để dễ quét

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, width, height, hintMap);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            image.createGraphics();

            Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.BLACK);

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (bitMatrix.get(i, j)) {
                        graphics.fillRect(i, j, 1, 1);
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generate QR code image sử dụng VietQR với amount và orderNumber
     */
    public String generateQRCodeImage(BigDecimal amount, String orderNumber, int width, int height) {
        // Sử dụng VietQR.io nếu enabled
        if (vietQREnabled && vietQRService != null) {
            try {
                String addInfo = "Thanh toan don hang " + orderNumber;
                String imageUrl = vietQRService.generateQRCodeImageUrl(amount, addInfo);
                return vietQRService.getQRCodeImageAsBase64(imageUrl);
            } catch (Exception e) {
                System.out.println("Error using VietQR service, falling back to local generation: " + e.getMessage());
                // Fallback to local generation
            }
        }
        
        // Fallback: Generate QR content và tạo image local
        String qrContent = generateQRContent(amount, orderNumber);
        return generateQRCodeImage(qrContent, width, height);
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }
}

package com.ruoyi.common.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ruoyi.common.utils.qrcode.MatrixToImageWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Google Authenticator 工具类
 */
public class GoogleAuthenticatorUtil {

    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    /**
     * 生成密钥
     *
     * @return 密钥字符串
     */
    public static String generateSecretKey() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /**
     * 获取二维码 URL
     *
     * @param username  用户名
     * @param secretKey 密钥
     * @return 二维码的 URL
     */
    public static String getQRCodeURL(String username, String secretKey) {
        String issuer = "ruoyi"; // 项目名称
        return String.format("otpauth://totp/%s?secret=%s&issuer=%s", username, secretKey, issuer);
    }

    /**
     * base 64位编码
     * @param username
     * @param secretKey
     * @return
     * @throws Exception
     */
    public static String generateQRCodeBase64(String username, String secretKey) throws Exception {
        String issuer = "sdktms.kerryeas.com";
        String qrCodeURL = String.format("otpauth://totp/%s?secret=%s&issuer=%s", username, secretKey, issuer);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeURL, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", stream);
        byte[] qrCodeBytes = stream.toByteArray();
        return Base64.getEncoder().encodeToString(qrCodeBytes);
    }
    /**
     * 验证动态验证码
     *
     * @param secretKey 密钥
     * @param code      动态验证码
     * @return 验证结果
     */
    public static boolean verifyCode(String secretKey, int code) {
        return gAuth.authorize(secretKey, code);
    }
}

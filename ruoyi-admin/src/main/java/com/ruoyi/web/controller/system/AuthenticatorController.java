package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.GoogleAuthenticatorUtil;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Google Authenticator Controller
 */
@RestController
@RequestMapping("/authenticator")
public class AuthenticatorController {

    @Autowired
    private ISysUserService userService;

    /**
     * 生成密钥和二维码
     */
    @GetMapping("/generate")
    public AjaxResult generateQRCode(@RequestParam String username) throws Exception {
        // 生成 Google Authenticator 密钥
        String secretKey = GoogleAuthenticatorUtil.generateSecretKey();
        String qrCodeURL = GoogleAuthenticatorUtil.generateQRCodeBase64(username, secretKey);

        // 将密钥存储到用户信息中
        SysUser user = userService.selectUserByUserName(username);
        if (user == null) {
            return AjaxResult.error("用户不存在");
        }
        Map<String, String> result = new HashMap<>();
        result.put("qrCodeURL", "data:image/png;base64," + qrCodeURL);
        result.put("secretKey", secretKey);
        return AjaxResult.success(result);
    }

    /**
     * 验证绑定 Google Authenticator
     */
    @PostMapping("/bind")
    public AjaxResult bindGoogleAuthenticator(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String secretKey = params.get("secretKey");
        int googleCode = Integer.parseInt(params.get("googleCode"));

        // 验证动态验证码
        boolean isValid = GoogleAuthenticatorUtil.verifyCode(secretKey, googleCode);
        if (!isValid) {
            return AjaxResult.error("动态验证码无效,绑定失效！");
        }

        // 更新用户的绑定状态
        SysUser user = userService.selectUserByUserName(username);
        if (user == null) {
            return AjaxResult.error("用户不存在");
        }
        user.setGaSecretKey(secretKey);
        userService.updateUser(user);

        return AjaxResult.success("绑定成功");
    }
}

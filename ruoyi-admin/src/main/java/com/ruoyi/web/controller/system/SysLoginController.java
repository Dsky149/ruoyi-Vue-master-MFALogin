package com.ruoyi.web.controller.system;

import java.util.List;
import java.util.Set;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.GoogleAuthenticatorUtil;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysMenuService;

/**
 * 登录验证
 * 
 * @author ruoyi
 */
@RestController
public class SysLoginController
{
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysUserService userService;

    /**
     * 登录方法
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody) {
        AjaxResult ajax = AjaxResult.success();
        //获取用户信息
        SysUser user = userService.selectUserByUserName(loginBody.getUsername());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        if (user.getGaEnabled() == 1) {
            // 如果启用，判断用户是否已绑定密钥
            if (user.getGaSecretKey() == null || user.getGaSecretKey().isEmpty()) {
                // 返回提示，要求绑定GoogleAuthenticator
                ajax.put("msg","要求绑定GoogleAuthenticator");
                ajax.put("needBindGA", true);
                return ajax;
            }
            //判断有没有验证码
            if (loginBody.getGoogleCode() == null || loginBody.getGoogleCode().isEmpty()) {
                // 返回提示，要求绑定GoogleAuthenticator
                ajax.put("msg","谷歌验证吗不能为空");
                ajax.put("needGooogleCode", true);
                return ajax;
            }
            // 如果已绑定，校验动态验证码
            try {
                int googleCode = Integer.parseInt(loginBody.getGoogleCode()); // 假设 code 为动态验证码
                boolean isValid = GoogleAuthenticatorUtil.verifyCode(user.getGaSecretKey(), googleCode);
                if (!isValid) {
                    throw new ServiceException("Google Authenticator 验证码无效");
                }
            } catch (NumberFormatException e) {
                throw new ServiceException("动态验证码格式错误");
            }
        }

        // 生成令牌
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions))
        {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        return ajax;
    }

    /**
     * 获取路由信息
     * 
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }
}

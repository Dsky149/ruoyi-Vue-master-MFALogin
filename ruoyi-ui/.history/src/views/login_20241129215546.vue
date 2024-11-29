<template>
  <div class="login">
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">若依后台管理系统</h3>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          auto-complete="off"
          placeholder="账号"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img"/>
        </div>
      </el-form-item>

      <el-form-item prop="googleCode" v-if="gaEnabled">
        <el-input
          v-model="loginForm.googleCode"
          placeholder="请输入动态验证码"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>

      <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width:100%;"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>Copyright © 2018-2024 ruoyi.vip All Rights Reserved.</span>
    </div>

    <!-- Google Authenticator 绑定弹窗 -->
    <el-dialog
      title="绑定 Google Authenticator"
      :visible.sync="showBindGADialog"
      width="30%">
      <div style="text-align: center;">
        <p>请扫描二维码绑定 Google Authenticator：</p>
        <img :src="qrCode" alt="QR Code" />
        <!-- <p>密钥：{{ secretKey }}</p> -->
        <p>绑定后输入Goole动态验证码完成绑定：</p>
        <el-input v-model="googleCode" placeholder="请输入动态验证码" />
      </div>
      <div slot="footer">
        <el-button @click="cancelBindGA">取消</el-button>
        <el-button type="primary" @click="confirmBindGA">确认绑定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";
import { encrypt, decrypt } from '@/utils/jsencrypt'

export default {
  name: "Login",
  data() {
    return {
      codeUrl: "",
      loginForm: {
        username: "admin",
        password: "admin123",
        rememberMe: false,
        code: "",
        uuid: "",
        googleCode: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }],
        googleCode: [
                  {
                    required: false, // 默认不必填
                    trigger: "blur",
                    validator: (rule, value, callback) => {
                      if (this.gaEnabled && !value) {
                        callback(new Error("请输google Coode!"));
                      } else {
                        callback(); // 校验通过
                      }
                    },
                  },
                ],
      },
      loading: false,
      // 验证码开关
      captchaEnabled: true,
      // 注册开关
      register: false,
      redirect: undefined,
      gaEnabled: false, // 是否启用 Google Authenticator
      showBindGADialog: false, // 是否显示绑定弹窗
      qrCode: "", // Google Authenticator 的二维码
      secretKey: "", // Google Authenticator 的密钥
      googleCode: "", // 动态验证码输入框
    };
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect;
      },
      immediate: true
    }
  },
  created() {
    this.getCode();
    this.getCookie();
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled;
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img;
          this.loginForm.uuid = res.uuid;
        }
      });
    },
    getCookie() {
      const username = Cookies.get("username");
      const password = Cookies.get("password");
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      };
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 });
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 });
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 });
          } else {
            Cookies.remove("username");
            Cookies.remove("password");
            Cookies.remove('rememberMe');
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            alert(res.logSuccess)
            //需要绑定
              if(res.needBindGA == true){
                this.showBindGADialog = true;
                this.getGAQRCode();
                //验证码为空
              }else if(res.needGooogleCode == true){
                  this.gaEnabled = true;
                  this.loading = false;
              }else if(res.logSuccess == true){
                this.$router.push({ path: this.redirect || "/" }).catch(()=>{});
              }
          }).catch(() => {
            this.loading = false;
            if (this.captchaEnabled) {
              this.getCode();
            }
          });
        }
      });
    },
    getGAQRCode() {
      this.$axios.get(`/authenticator/generate?username=${this.loginForm.username}`).then(res => {
        this.qrCode = res.data.data.qrCodeURL
        this.secretKey = res.data.data.secretKey;
      });
    },
    confirmBindGA() {
        //二维码搞进去
        this.loginForm.googleCode = this.googleCode;
        this.showBindGADialog = false;
        this.loading = false;
        this.gaEnabled = true;
        if (this.captchaEnabled) {
          this.getCode();
        }
        this.handleLogin();//登录
    },
    cancelBindGA() {
      this.showBindGADialog = false;
      this.loading = false;
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss">
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}

.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
  .el-input {
    height: 38px;
    input {
      height: 38px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
  }
}
.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: 38px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 38px;
}
</style>

package com.hmall.common.interceptors;

import cn.hutool.core.util.StrUtil;
import com.hmall.common.utils.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 获取用户信息（HTTP header 名称不区分大小写，但 Spring Cloud Gateway 会转换为小写）
        // 优先尝试小写（因为网关传递的是小写）
        String userInfo = request.getHeader("user-info");
        // 如果没找到，尝试大小写混合（兼容性）
        if (StrUtil.isBlank(userInfo)) {
            userInfo = request.getHeader("user-Info");
        }
        if(StrUtil.isNotBlank(userInfo)){
            try {
                Long userId = Long.valueOf(userInfo);
                UserContext.setUser(userId);
                System.out.println("UserInfoInterceptor: 设置用户ID = " + userId);
            } catch (NumberFormatException e) {
                System.err.println("UserInfoInterceptor: 用户ID格式错误: " + userInfo);
            }
        } else {
            System.err.println("UserInfoInterceptor: 未找到用户信息 header");
        }

        //放行
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 移除用户信息
        UserContext.removeUser();
    }


}

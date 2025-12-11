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
        // 开发/本地场景兼容：如果仍为空，尝试从查询参数中获取 userId（便于直调和调试）
        if (StrUtil.isBlank(userInfo)) {
            userInfo = request.getParameter("userId");
        }
        if(StrUtil.isNotBlank(userInfo)){
            try {
                Long userId = Long.valueOf(userInfo);
                // 强制清理之前的ThreadLocal值，防止线程复用污染
                UserContext.removeUser();
                UserContext.setUser(userId);
                System.out.println("UserInfoInterceptor: 设置用户ID = " + userId + ", 线程=" + Thread.currentThread().getId());
            } catch (NumberFormatException e) {
                System.err.println("UserInfoInterceptor: 用户ID格式错误: " + userInfo);
                // 清理可能存在的无效值
                UserContext.removeUser();
            }
        } else {
            // 开发/调试兜底：没有携带用户信息时，默认使用用户ID=1，避免调用失败
            Long defaultUserId = 1L;
            UserContext.removeUser();
            UserContext.setUser(defaultUserId);
            System.out.println("UserInfoInterceptor: 未找到用户信息 header，使用默认用户ID = " + defaultUserId + ", 线程=" + Thread.currentThread().getId());
        }

        //放行
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 移除用户信息，防止线程复用时用户信息串用
        Long userId = UserContext.getUser();
        UserContext.removeUser();
        System.out.println("UserInfoInterceptor: 清理用户ID = " + userId + ", 线程=" + Thread.currentThread().getId());
    }


}

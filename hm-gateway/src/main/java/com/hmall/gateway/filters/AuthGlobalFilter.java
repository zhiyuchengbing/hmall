package com.hmall.gateway.filters;


import cn.hutool.core.text.AntPathMatcher;
import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.util.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter  implements GlobalFilter, Ordered {
    private final AuthProperties authProperties;
    private  final JwtTool jwtTool;
    private  final AntPathMatcher antPathMatcher = new AntPathMatcher();


    @Override

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1 获取请求


        ServerHttpRequest request = exchange.getRequest();


        //2 判断是否需要做登录拦截
        if(isExcludePath(request.getPath().toString())){
            //放行
            return chain.filter(exchange);
            
        }


        //3获取token

        String token = null;
        List<String> headers= request.getHeaders().get("authorization");
        if (headers!=null && !headers.isEmpty()){
            token = headers.get(0);
            // 去掉 Bearer 前缀（如果存在）
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
        }
        Long userId = null;


        //4 校验并解析tokrn
        try {
            userId = jwtTool.parseToken(token);

        }catch (UnauthorizedException  e){
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();

        }
        //传递用户信息
        System.out.println("AuthGlobalFilter: 解析到的userId = " + userId);
        if (userId == null) {
            System.err.println("AuthGlobalFilter: userId 为空，返回 401");
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        String userInfo = userId.toString();
        // 注意：Spring Cloud Gateway 会将 header 名称转换为小写，所以使用小写名称
        ServerWebExchange swe = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo)).build();
        System.out.println("AuthGlobalFilter: 传递用户信息 header user-info = " + userInfo);

        return chain.filter(swe);
    }

    private boolean isExcludePath(String Path) {


        for (String excludePath : authProperties.getExcludePaths()) {
            if(antPathMatcher.match(excludePath,Path)){
                return true;
            }

        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }


}

package com.hmall.gateway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {

    private final NacosConfigManager nacosConfigManager;
    private final RouteDefinitionWriter writer;
    private final String dataId = "gateway-router.json";
    private final  String group = "DEFAULT_GROUP";
    private final Set<String> routeIds = new HashSet<>();


    @PostConstruct
    public void  initRouteConfigListener() throws NacosException {
        log.info("初始化路由加载器");
        //项目启动 先拉去配置，并添加配置监听器
        String configInfo = nacosConfigManager.getConfigService().getConfigAndSignListener(dataId, group, 5000, new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                //2监听到配置变更，需要去更新路由表
                log.info("监听到路由配置变更：{}", configInfo);
                updateConfigInfo(configInfo);
            }
        });
        //3 第一次读取到配置，需要更新到路由表
        log.info("首次加载路由配置：{}", configInfo);
        updateConfigInfo(configInfo);
    }

    public void updateConfigInfo(String configInfo){
        try {
            List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
            //4 更新路由表
            for(String routeId: routeIds){
                writer.delete(Mono.just(routeId)).subscribe();

            }
            routeIds.clear();


            for(RouteDefinition routeDefinition: routeDefinitions){
                log.info("更新路由：{}", routeDefinition.getId());
                writer.save(Mono.just(routeDefinition)).subscribe();
                //记录路由Id
                routeIds.add(routeDefinition.getId());
            }
        } catch (Exception e) {
            log.error("更新路由配置失败", e);
        }
    }

}

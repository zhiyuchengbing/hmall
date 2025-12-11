package com.hmall.cart.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.cart.config.CartProperties;
import com.hmall.cart.domain.dto.CartFormDTO;
import com.hmall.cart.domain.po.Cart;
import com.hmall.cart.domain.vo.CartVO;
import com.hmall.cart.mapper.CartMapper;
import com.hmall.cart.service.ICartService;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.UserContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 购物车服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor

public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {
//    private final RestTemplate restTemplate;
//    private final DiscoveryClient discoveryClient;
    private  final ItemClient itemClient;
    private final CartProperties cartProperties;



    @Override
    public void addItem2Cart(CartFormDTO cartFormDTO) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        System.out.println("CartServiceImpl.addItem2Cart: 当前用户ID = " + userId + ", 线程=" + Thread.currentThread().getId());
        
        if (userId == null) {
            System.err.println("CartServiceImpl.addItem2Cart: 用户ID为空，可能存在线程污染问题! 线程=" + Thread.currentThread().getId());
            throw new BizIllegalException("用户未登录或用户信息异常");
        }

        // 2.判断是否已经存在
        if (checkItemExists(cartFormDTO.getItemId(), userId)) {
            // 2.1.存在，则更新数量
            baseMapper.updateNum(cartFormDTO.getItemId(), userId);
            return;
        }
        // 2.2.不存在，判断是否超过购物车数量
        checkCartsFull(userId);

        // 3.新增购物车条目
        Cart cart = BeanUtils.copyBean(cartFormDTO, Cart.class);
        cart.setUserId(userId);
        save(cart);
    }

    @Override
    public List<CartVO> queryMyCarts() {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        System.out.println("CartServiceImpl.queryMyCarts: 当前用户ID = " + userId + ", 线程=" + Thread.currentThread().getId());
        
        if (userId == null) {
            System.err.println("CartServiceImpl.queryMyCarts: 用户ID为空，可能存在线程污染问题! 线程=" + Thread.currentThread().getId());
            throw new BizIllegalException("用户未登录或用户信息异常");
        }
        
        // 2.查询我的购物车列表
        List<Cart> carts = lambdaQuery().eq(Cart::getUserId, userId).list();
        System.out.println("CartServiceImpl.queryMyCarts: 用户ID " + userId + " 查询到购物车数量 = " + (carts != null ? carts.size() : 0) + ", 线程=" + Thread.currentThread().getId());
        
        if (CollUtils.isEmpty(carts)) {
            return CollUtils.emptyList();
        }

        // 2.转换VO
        List<CartVO> vos = BeanUtils.copyList(carts, CartVO.class);

        // 3.处理VO中的商品信息
        handleCartItems(vos);

        return vos;
    }

    private void handleCartItems(List<CartVO> vos) {
        //TODO //获取商品id
        // 1.获取商品id
        Set<Long> itemIds = vos.stream().map(CartVO::getItemId).collect(Collectors.toSet());
        // 2.获取商品信息
        //2.1 根据服务名称获取服务的实例列表
//        List<ServiceInstance> instances = discoveryClient.getInstances("item-service");
//        if(CollUtils.isEmpty(instances)){
//            return;
//        }
//        //2.2 手写负载均衡，从实例列表中选一个实例
//        ServiceInstance instance = instances.get(RandomUtil.randomInt(instances.size()));
//
//
//        // 2.查询商品
////        List<ItemDTO> items = itemService.queryItemByIds(itemIds);
//        // 发送http请求
//        ResponseEntity<List<ItemDTO>> response = restTemplate.exchange(
//                instance.getUri() + "/item/?ids={ids}",
//                HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<List<ItemDTO>>() {
//                },
//                Map.of("ids", CollUtils.join(itemIds, ","))
//
//
//        );    //  .var  得到返回值
//
//        if (!response.getStatusCode().is2xxSuccessful()) {
//            return;
//
//        }
//        List<ItemDTO> items = response.getBody();
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (CollUtils.isEmpty(items)) {
            return;
        }
        // 3.转为 map
        Map<Long, ItemDTO> itemMap = items.stream()
                .collect(Collectors.toMap(ItemDTO::getId, Function.identity()));
        // 4.写入vo
        for (CartVO v : vos) {
            ItemDTO item = itemMap.get(v.getItemId());
            if (item == null) {
                continue;
            }
            v.setNewPrice(item.getPrice());
            v.setStatus(item.getStatus());
            v.setStock(item.getStock());
        }

    }

    @Override
    @Transactional
    public void removeByItemIds(Collection<Long> itemIds) {
        Long userId = UserContext.getUser();
        System.out.println("CartServiceImpl.removeByItemIds: 当前用户ID = " + userId + ", 线程=" + Thread.currentThread().getId());
        
        if (userId == null) {
            System.err.println("CartServiceImpl.removeByItemIds: 用户ID为空，可能存在线程污染问题! 线程=" + Thread.currentThread().getId());
            throw new BizIllegalException("用户未登录或用户信息异常");
        }
        
        QueryWrapper<Cart> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(Cart::getUserId, userId)
                .in(Cart::getItemId, itemIds);
        remove(queryWrapper);
    }

    private void checkCartsFull(Long userId) {
        Integer count = lambdaQuery().eq(Cart::getUserId, userId).count();
        if (count >= cartProperties.getMaxItems()) {
            throw new BizIllegalException(StrUtil.format("用户购物车商品不能超过{}", cartProperties.getMaxItems()));
        }
    }

    private boolean checkItemExists(Long itemId, Long userId) {
        Integer count = lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getItemId, itemId)
                .count();
        return count > 0;
    }
}

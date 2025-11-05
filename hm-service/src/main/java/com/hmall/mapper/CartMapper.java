package com.hmall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmall.domain.po.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 购物车 Mapper 接口
 * </p>
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    /**
     * 更新购物车中指定商品的数量 +1
     *
     * @param itemId 商品ID（Long类型）
     * @param userId 用户ID（Long类型）
     */
    @Update("UPDATE cart SET num = num + 1 WHERE user_id = #{userId} AND item_id = #{itemId}")
    void updateNum(@Param("itemId") Long itemId, @Param("userId") Long userId);
}

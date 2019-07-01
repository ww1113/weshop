package tech.wetech.weshop.wechat.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.wetech.weshop.common.enums.ResultStatus;
import tech.wetech.weshop.common.exception.BizException;
import tech.wetech.weshop.common.query.Criteria;
import tech.wetech.weshop.common.utils.Constants;
import tech.wetech.weshop.common.utils.IdGenerator;
import tech.wetech.weshop.order.api.CartApi;
import tech.wetech.weshop.order.api.OrderApi;
import tech.wetech.weshop.order.api.OrderExpressApi;
import tech.wetech.weshop.order.api.OrderGoodsApi;
import tech.wetech.weshop.order.enums.OrderStatusEnum;
import tech.wetech.weshop.order.enums.PayStatusEnum;
import tech.wetech.weshop.order.po.Cart;
import tech.wetech.weshop.order.po.Order;
import tech.wetech.weshop.order.po.OrderExpress;
import tech.wetech.weshop.order.po.OrderGoods;
import tech.wetech.weshop.order.query.OrderQuery;
import tech.wetech.weshop.user.api.AddressApi;
import tech.wetech.weshop.user.api.RegionApi;
import tech.wetech.weshop.user.po.Address;
import tech.wetech.weshop.wechat.service.WechatOrderService;
import tech.wetech.weshop.wechat.vo.HandleOptionVO;
import tech.wetech.weshop.wechat.vo.OrderDetailVO;
import tech.wetech.weshop.wechat.vo.OrderListVO;
import tech.wetech.weshop.wechat.vo.OrderSubmitParamVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
public class WechatOrderServiceImpl implements WechatOrderService {

    @Autowired
    private OrderApi orderApi;

    @Autowired
    private OrderGoodsApi orderGoodsApi;

    @Autowired
    private OrderExpressApi orderExpressApi;

    @Autowired
    private CartApi cartApi;

    @Autowired
    private AddressApi addressApi;

    @Autowired
    private RegionApi regionApi;

    @Override
    public List<OrderListVO> queryOrderList(OrderQuery orderQuery) {
        List<Order> orderList = orderApi.queryByCriteria(Criteria.of(Order.class).andEqualTo(Order::getUserId, Constants.CURRENT_USER_ID).page(orderQuery.getPageNum(), orderQuery.getPageSize())).getData();
        List<OrderListVO> orderVOList = new LinkedList<>();
        for (Order order : orderList) {
            OrderListVO orderVO = new OrderListVO(order)
                    .setGoodsList(orderGoodsApi.queryList(new OrderGoods().setOrderId(order.getId())).getData())
                    .setHandleOption(new HandleOptionVO(order))
                    .setOrderStatusText(order.getPayStatus().getName());
            orderVOList.add(orderVO);
        }
        return orderVOList;
    }

    @Override
    public OrderDetailVO queryOrderDetail(Integer orderId) {
        Order order = Optional.ofNullable(orderApi.queryById(orderId).getData())
                .orElseThrow(() -> new BizException(ResultStatus.RECORD_NOT_EXIST));

        OrderDetailVO.OrderInfoVO orderInfoVO = new OrderDetailVO.OrderInfoVO(order)
                .setOrderExpress(orderExpressApi.queryOne(new OrderExpress().setOrderId(orderId)).getData());

        orderInfoVO.setProvinceName(
                regionApi.queryNameById(orderInfoVO.getProvince()).getData()
        ).setCityName(
                regionApi.queryNameById(orderInfoVO.getCity()).getData()
        ).setDistrictName(
                regionApi.queryNameById(orderInfoVO.getDistrict()).getData()
        );
        orderInfoVO.setFullRegion(
                orderInfoVO.getProvinceName() + orderInfoVO.getCityName() + orderInfoVO.getDistrictName()
        );

        List<OrderGoods> orderGoodsList = orderGoodsApi.queryList(new OrderGoods().setOrderId(orderId)).getData();

        return new OrderDetailVO(orderInfoVO, orderGoodsList, new HandleOptionVO(order));
    }

    @Override
    public Order submitOrder(OrderSubmitParamVO orderSubmitParamDTO) {
        Address checkedAddress = addressApi.queryById(orderSubmitParamDTO.getAddressId()).getData();
        if (checkedAddress == null) {
            throw new BizException(ResultStatus.PLEASE_SELECT_SHIPPING_ADDRESS);
        }

        //获取要购买的商品
        List<Cart> checkedGoodsList = cartApi.queryList(
                new Cart()
                        .setUserId(Constants.CURRENT_USER_ID)
                        .setSessionId(Constants.SESSION_ID)
                        .setChecked(true)
        ).getData();
        if (checkedGoodsList.isEmpty()) {
            throw new BizException(ResultStatus.PLEASE_SELECT_SHIPPING_ADDRESS);
        }

        //统计商品总价
        BigDecimal goodsTotalPrice = BigDecimal.ZERO;
        for (Cart cart : checkedGoodsList) {
            goodsTotalPrice = goodsTotalPrice.add(
                    cart.getRetailPrice().multiply(new BigDecimal(cart.getNumber()))
            );
        }

        //运费价格
        BigDecimal freightPrice = BigDecimal.ZERO;

        //获取订单使用的优惠券
        BigDecimal couponPrice = BigDecimal.ZERO;
        if (orderSubmitParamDTO.getCouponId() != null) {
            //计算优惠券的价格 未实现
        }

        // 订单价格计算  实际价格 = 商品价格 + 运费价格 - 优惠券价格
        BigDecimal orderTotalPrice = goodsTotalPrice.add(freightPrice).subtract(couponPrice);
        // 减去其它支付的金额后，要实际支付的金额
        BigDecimal actualPrice = orderTotalPrice.subtract(new BigDecimal(0.00));
        Date currentTime = new Date();

        Order orderInfo = new Order();
        orderInfo.setOrderSN(IdGenerator.INSTANCE.nextId());
        orderInfo.setUserId(Constants.CURRENT_USER_ID);

        //收货地址和运费
        orderInfo.setConsignee(checkedAddress.getName());
        orderInfo.setMobile(checkedAddress.getMobile());
        orderInfo.setProvince(checkedAddress.getProvinceId());
        orderInfo.setCity(checkedAddress.getCityId());
        orderInfo.setDistrict(checkedAddress.getDistrictId());
        orderInfo.setAddress(checkedAddress.getAddress());
        orderInfo.setFreightPrice(new BigDecimal(0.00));

        //留言
        orderInfo.setPostscript(orderSubmitParamDTO.getPostscript());

        //使用优惠券
        orderInfo.setCouponId(0);
        orderInfo.setCouponPrice(couponPrice);
        orderInfo.setCreateTime(currentTime);
        orderInfo.setGoodsPrice(goodsTotalPrice);
        orderInfo.setOrderPrice(orderTotalPrice);
        orderInfo.setActualPrice(actualPrice);

        //订单状态：提交订单
        orderInfo.setOrderStatus(OrderStatusEnum.SUBMIT_ORDER);
        //支付状态：待付款
        orderInfo.setPayStatus(PayStatusEnum.PENDING_REFUND);

        orderApi.create(orderInfo);

        //统计商品总价
        List<OrderGoods> orderGoodsList = new LinkedList<>();
        for (Cart goodsItem : checkedGoodsList) {
            OrderGoods orderGoods = new OrderGoods();
            orderGoods.setOrderId(orderInfo.getId());
            orderGoods.setGoodsId(goodsItem.getGoodsId());
            orderGoods.setGoodsSn(goodsItem.getGoodsSn());
            orderGoods.setProductId(goodsItem.getProductId());
            orderGoods.setGoodsName(goodsItem.getGoodsName());
            orderGoods.setListPicUrl(goodsItem.getListPicUrl());
            orderGoods.setMarketPrice(goodsItem.getMarketPrice());
            orderGoods.setRetailPrice(goodsItem.getRetailPrice());
            orderGoods.setNumber(goodsItem.getNumber());
            orderGoods.setGoodsSpecificationNameValue(goodsItem.getGoodsSpecificationNameValue());
            orderGoods.setGoodsSpecificationIds(goodsItem.getGoodsSpecificationIds());

            orderGoodsList.add(orderGoods);
        }
        orderGoodsApi.createBatch(orderGoodsList);

        //清空购物车已购买商品
        cartApi.delete(new Cart()
                .setUserId(Constants.CURRENT_USER_ID)
                .setSessionId(Constants.SESSION_ID)
                .setChecked(true)
        );
        return orderInfo;
    }
}

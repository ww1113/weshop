package tech.wetech.weshop.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.wetech.weshop.po.Cart;
import tech.wetech.weshop.service.CartService;
import tech.wetech.weshop.utils.Constants;
import tech.wetech.weshop.utils.Result;
import tech.wetech.weshop.vo.CartCheckedVO;
import tech.wetech.weshop.vo.CartCheckoutVO;
import tech.wetech.weshop.vo.CartParamVO;
import tech.wetech.weshop.vo.CartResultVO;

import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/wechat/cart")
@Validated
public class WechatCartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/index")
    public Result<CartResultVO> getCart() {
        return Result.success(cartService.getCart());
    }

    @PostMapping("/add-to-cart")
    public Result<CartResultVO> addGoodsToCart(@RequestBody @Validated CartParamVO cartParamVO) {
        cartService.addGoodsToCart(cartParamVO);
        return Result.success(cartService.getCart());
    }

    @PostMapping("/update-cart-goods")
    public Result<CartResultVO> updateCartGoods(@RequestBody @Validated(CartParamVO.CartUpdateChecks.class) CartParamVO cartParamVO) {
        cartService.updateGoods(cartParamVO);
        return Result.success(cartService.getCart());
    }

    @PostMapping("/delete")
    public Result<CartResultVO> deleteCartGoods(@NotNull Integer cartId) {
        cartService.deleteById(cartId);
        return Result.success(cartService.getCart());
    }

    @PostMapping("/checked")
    public Result<CartResultVO> checkedCartGoods(@RequestBody @Validated CartCheckedVO cartCheckedVO) {
        cartService.updateNotNull(new Cart()
                .setId(cartCheckedVO.getCartId())
                .setChecked(cartCheckedVO.getChecked()));
        return Result.success(cartService.getCart());
    }

    @GetMapping("/goods-count")
    public Result<Integer> goodsCount() {
        int count = cartService.count(new Cart()
                .setUserId(Constants.CURRENT_USER_ID)
                .setSessionId(Constants.SESSION_ID));
        return Result.success(count);
    }

    @GetMapping("/checkout")
    public Result<CartCheckoutVO> checkoutCartGoods(Integer addressId, Integer couponId) {
        return Result.success(cartService.checkoutCart(addressId, couponId));
    }


}
package com.exequt.cart.application;

import com.exequt.cart.api.dto.AddCartItemRequest;
import com.exequt.cart.api.dto.CartResponse;
import com.exequt.cart.api.dto.CreateCartResponse;
import com.exequt.cart.domain.Cart;
import com.exequt.cart.persistence.CartRepository;
import com.exequt.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartApplicationService {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;

    public CartApplicationService(CartRepository cartRepository, CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.cartMapper = cartMapper;
    }

    @Transactional
    public CreateCartResponse createCart() {
        Cart cart = Cart.create();
        Cart saved = cartRepository.save(cart);
        return cartMapper.toCreateCartResponse(saved);
    }

    @Transactional
    public CartResponse addItem(Long cartId, AddCartItemRequest request) {
        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new NotFoundException("Cart not found"));
        cart.addItem(request.getProductId(), request.getQuantity(), request.getPrice());
        return cartMapper.toCartResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long cartId) {
        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new NotFoundException("Cart not found"));
        cart.getItemsView().size();
        return cartMapper.toCartResponse(cart);
    }
}

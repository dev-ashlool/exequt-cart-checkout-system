package com.exequt.cart.api;

import com.exequt.cart.api.dto.AddCartItemRequest;
import com.exequt.cart.api.dto.CartResponse;
import com.exequt.cart.api.dto.CreateCartResponse;
import com.exequt.cart.application.CartApplicationService;
import com.exequt.common.response.GenericResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carts")
@Validated
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartApplicationService cartApplicationService;

    public CartController(CartApplicationService cartApplicationService) {
        this.cartApplicationService = cartApplicationService;
    }

    @PostMapping
    public ResponseEntity<GenericResponse<CreateCartResponse>> createCart() {
        log.info("Creating new cart");
        CreateCartResponse body = cartApplicationService.createCart();
        URI location = URI.create("/carts/" + body.getId());
        return ResponseEntity.created(location).body(GenericResponse.success(body));
    }

    @PostMapping("/{cartId}/items")
    public ResponseEntity<GenericResponse<CartResponse>> addItem(
            @PathVariable Long cartId, @Valid @RequestBody AddCartItemRequest request) {
        log.info("Adding item to cart cartId={} productId={}", cartId, request.getProductId());
        CartResponse body = cartApplicationService.addItem(cartId, request);
        return ResponseEntity.ok(GenericResponse.success(body));
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<GenericResponse<CartResponse>> getCart(@PathVariable Long cartId) {
        log.info("Fetching cart cartId={}", cartId);
        CartResponse body = cartApplicationService.getCart(cartId);
        return ResponseEntity.ok(GenericResponse.success(body));
    }
}

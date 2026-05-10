package com.exequt.cart.application;

import com.exequt.cart.domain.Cart;
import com.exequt.cart.domain.CartStatus;
import com.exequt.cart.persistence.CartRepository;
import com.exequt.common.exception.ConflictException;
import com.exequt.common.exception.NotFoundException;
import com.exequt.order.api.dto.OrderResponse;
import com.exequt.order.application.CreateOrderItemCommand;
import com.exequt.order.application.OrderCommandService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutApplicationService {

    private final CartRepository cartRepository;
    private final OrderCommandService orderCommandService;

    public CheckoutApplicationService(CartRepository cartRepository, OrderCommandService orderCommandService) {
        this.cartRepository = cartRepository;
        this.orderCommandService = orderCommandService;
    }

    /**
     * Locks the cart row for update first so concurrent checkouts for the same cart are serialized and
     * duplicate orders cannot be created.
     */
    @Transactional
    public CheckoutResult checkout(Long cartId) {
        Cart cart = cartRepository.findByIdForUpdate(cartId).orElseThrow(() -> new NotFoundException("Cart not found"));

        Optional<OrderResponse> existing = orderCommandService.findExistingOrderByCartId(cartId);
        if (existing.isPresent()) {
            return new CheckoutResult(existing.get(), false);
        }

        if (cart.getStatus() != CartStatus.OPEN) {
            throw new ConflictException("Cart cannot be checked out because it is not open");
        }
        cart.validateNotEmpty();

        List<CreateOrderItemCommand> lines = cart.getItemsView().stream()
                .map(i -> new CreateOrderItemCommand(i.getProductId(), i.getQuantity(), i.getPrice()))
                .toList();

        cart.checkout();
        OrderResponse orderResponse = orderCommandService.createOrderFromCartSnapshot(cartId, lines);
        cartRepository.save(cart);
        return new CheckoutResult(orderResponse, true);
    }
}

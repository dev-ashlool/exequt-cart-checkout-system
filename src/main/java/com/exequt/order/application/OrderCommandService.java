package com.exequt.order.application;

import com.exequt.order.api.dto.OrderResponse;
import com.exequt.order.domain.CartItemSnapshot;
import com.exequt.order.domain.Order;
import com.exequt.order.persistence.OrderRepository;
import com.exequt.common.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderCommandService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }
    
    @Transactional(readOnly = true)
    public Optional<OrderResponse> findExistingOrderByCartId(Long cartId) {
        return orderRepository.findByCartId(cartId).map(this::toResponseWithItemsLoaded);
    }

    public OrderResponse createOrderFromCartSnapshot(Long cartId, List<CreateOrderItemCommand> lines) {
        List<CartItemSnapshot> snapshots = lines.stream()
                .map(c -> new CartItemSnapshot(c.productId(), c.quantity(), c.price()))
                .toList();
        Order order = Order.createFromCartSnapshot(cartId, snapshots);
        Order saved = orderRepository.save(order);
        return toResponseWithItemsLoaded(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponseWithItemsLoaded(order);
    }

    private OrderResponse toResponseWithItemsLoaded(Order order) {
        order.getItemsView().size();
        return orderMapper.toOrderResponse(order);
    }
}

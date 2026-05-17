package com.exequt.order.application;

import com.exequt.common.exception.ConflictException;
import com.exequt.common.exception.NotFoundException;
import com.exequt.order.api.dto.OrderResponse;
import com.exequt.order.domain.CartItemSnapshot;
import com.exequt.order.domain.Order;
import com.exequt.order.domain.OrderStatus;
import com.exequt.order.persistence.OrderRepository;
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
				.map(c -> new CartItemSnapshot(c.productId(), c.quantity(), c.price())).toList();
		Order order = Order.createFromCartSnapshot(cartId, snapshots);
		Order saved = orderRepository.save(order);
		return toResponseWithItemsLoaded(saved);
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrder(Long orderId) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
		return toResponseWithItemsLoaded(order);
	}

	/**
	 * Locks the order row and transitions CREATED or PAYMENT_FAILED to
	 * PENDING_PAYMENT when starting a new payment attempt.
	 */
	@Transactional
	public void ensureOrderReadyForNewPaymentAttempt(Long orderId) {
		Order order = orderRepository.findByIdForUpdate(orderId)
				.orElseThrow(() -> new NotFoundException("Order not found"));

		if (order.getStatus() == OrderStatus.PAID) {
			throw new ConflictException("Order is already paid");
		}

		if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
			throw new ConflictException("Order already has a payment in progress");
		}

		if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.PAYMENT_FAILED) {
			order.startPayment();
			orderRepository.save(order);
			return;
		}

		throw new ConflictException("Order is not payable for payment");
	}

	@Transactional
	public void markOrderAsPaid(Long orderId) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
		order.markPaid();
		orderRepository.save(order);
	}

	@Transactional
	public void markOrderPaymentFailed(Long orderId) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
		order.markPaymentFailed();
		orderRepository.save(order);
	}

	private OrderResponse toResponseWithItemsLoaded(Order order) {
		order.getItemsView().size();
		return orderMapper.toOrderResponse(order);
	}
}

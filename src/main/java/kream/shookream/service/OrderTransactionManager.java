package kream.shookream.service;

import kream.shookream.domain.Order;
import kream.shookream.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderTransactionManager {

    private final OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order saveOrder(Order newOrder) {
        return orderRepository.save(newOrder);
    }
}

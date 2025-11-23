package kream.shookream.repository;

import kream.shookream.domain.OrderTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTicketRepository extends JpaRepository<OrderTicket, Long> {
}

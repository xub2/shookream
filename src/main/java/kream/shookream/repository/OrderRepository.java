package kream.shookream.repository;

import kream.shookream.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("select o from Order o join fetch o.orderTickets where o.id = :id")
    Optional<Order> findWithOrderTicketsById(@Param("id") Long id);

    @Query("select o from Order o join fetch o.orderTickets ot join fetch ot.ticket where o.id = :id")
    Optional<Order> findWithOrderDetailAndTicketById(@Param("id") Long id);
}

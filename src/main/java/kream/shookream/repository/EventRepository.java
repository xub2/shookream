package kream.shookream.repository;

import jakarta.persistence.LockModeType;
import kream.shookream.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // 비관적 락 적용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findWithPessimisticLockById(@Param("id") Long id); // SELECT FOR UPDATE 문

    @Lock(LockModeType.OPTIMISTIC)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findWithOptimisticLockById(@Param("id") Long id); // SELECT FOR UPDATE 문
}

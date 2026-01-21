package com.bankhoahoc.repository;

import com.bankhoahoc.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.course WHERE o.user.id = :userId")
    List<Order> findByUserId(@Param("userId") Long userId);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    @EntityGraph(attributePaths = {"items", "items.course", "user"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findById(@Param("id") Long id);
    
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.course LEFT JOIN FETCH o.user ORDER BY o.createdAt DESC")
    List<Order> findAll();
    
    List<Order> findByStatus(Order.OrderStatus status);
}

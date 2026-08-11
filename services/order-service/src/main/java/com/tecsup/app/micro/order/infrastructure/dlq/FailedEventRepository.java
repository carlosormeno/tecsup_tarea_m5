package com.tecsup.app.micro.order.infrastructure.dlq;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {
}

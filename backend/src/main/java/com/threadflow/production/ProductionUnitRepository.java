package com.threadflow.production;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionUnitRepository extends JpaRepository<ProductionUnit, Long> {
    Optional<ProductionUnit> findByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCase(String code);
    List<ProductionUnit> findAllByOrderByNameAsc();
}
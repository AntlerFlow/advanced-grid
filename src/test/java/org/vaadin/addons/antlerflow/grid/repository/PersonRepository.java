package org.vaadin.addons.antlerflow.grid.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vaadin.addons.antlerflow.grid.entity.PersonEntity;
import org.vaadin.addons.antlerflow.grid.filter.PersonFilter;

public interface PersonRepository extends JpaRepository<PersonEntity, Long> {

    @Query(
            "SELECT p FROM PersonEntity p WHERE (:#{#filter.name} IS NULL OR p.firstName LIKE %:#{#filter.name}%"
                    + " OR p.lastName LIKE %:#{#filter.name}%)"
                    + " AND (:#{#filter.ageGreaterEqual} IS NULL OR p.age >= :#{#filter.ageGreaterEqual})"
                    + " AND (:#{#filter.ageLessEqual} IS NULL OR p.age <= :#{#filter.ageLessEqual})")
    Page<PersonEntity> findAll(@Param("filter") PersonFilter filter, Pageable pageable);
}

package geumjeongyahak.domain.event.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import geumjeongyahak.domain.event.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    long countByIsDeletedFalseAndEventDateGreaterThanEqual(LocalDate eventDate);

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Page<Event> findAllByIsDeletedFalseAndEventDateBetween(
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Page<Event> findAllByIsDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    List<Event> findAllByIsDeletedFalseOrderByEventDateAscStartTimeAscIdAsc();

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Optional<Event> findByIdAndIsDeletedFalse(Long id);
}

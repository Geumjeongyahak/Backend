package geumjeongyahak.domain.sitecontent.repository;

import geumjeongyahak.domain.sitecontent.entity.SiteHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteHistoryRepository extends JpaRepository<SiteHistory, Long> {

    List<SiteHistory> findAllByOrderByHistoryDateAscSortOrderAscIdAsc();
}

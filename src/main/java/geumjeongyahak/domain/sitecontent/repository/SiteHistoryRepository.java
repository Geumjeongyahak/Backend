package geumjeongyahak.domain.sitecontent.repository;

import geumjeongyahak.domain.sitecontent.entity.SiteHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiteHistoryRepository extends JpaRepository<SiteHistory, Long> {

    List<SiteHistory> findAllByOrderByHistoryDateAscSortOrderAscIdAsc();

    @Query("select count(p) > 0 from SiteHistoryPhoto p where p.file.id = :fileId")
    boolean existsPhotoByFileId(@Param("fileId") UUID fileId);
}

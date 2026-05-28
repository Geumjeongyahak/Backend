package geumjeongyahak.domain.sitecontent.repository;

import geumjeongyahak.domain.sitecontent.entity.SiteContent;
import geumjeongyahak.domain.sitecontent.enums.SiteContentType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteContentRepository extends JpaRepository<SiteContent, Long> {

    @EntityGraph(attributePaths = "items")
    List<SiteContent> findAllByContentTypeInOrderBySortOrderAscIdAsc(Collection<SiteContentType> contentTypes);

    boolean existsByContentType(SiteContentType contentType);

    boolean existsByContentTypeAndIdNot(SiteContentType contentType, Long id);
}

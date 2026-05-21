package geumjeongyahak.domain.file.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;

@Service
@RequiredArgsConstructor
public class FileProxyService {

    private final FileRepository fileRepository;

    @Transactional(readOnly = true)
    public File getReferenceById(UUID fileId) {
        return fileRepository.getReferenceById(fileId);
    }
}

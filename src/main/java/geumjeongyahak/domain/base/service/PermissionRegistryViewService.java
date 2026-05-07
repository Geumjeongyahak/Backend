package geumjeongyahak.domain.base.service;

import geumjeongyahak.domain.base.model.PermissionRegistry;
import geumjeongyahak.domain.base.model.PermissionDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionRegistryViewService {

    public List<PermissionDefinition> getGlobalPermissions() {
        return PermissionRegistry.getGlobalPermissions();
    }
}

package geumjeongyahak.domain.channel.repository;

import org.springframework.data.jpa.domain.Specification;
import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;

public final class ChannelSpecs {

    private ChannelSpecs() {
    }

    public static Specification<Channel> withoutDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<Channel> containsName(String name) {
        return (root, query, cb) -> cb.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<Channel> hasChannelType(ChannelType channelType) {
        return (root, query, cb) -> cb.equal(root.get("channelType"), channelType);
    }

    public static Specification<Channel> hasBindingType(ChannelBindingType bindingType) {
        return (root, query, cb) -> cb.equal(root.get("bindingType"), bindingType);
    }

    public static Specification<Channel> hasIsActive(Boolean isActive) {
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<Channel> hasIsDefault(Boolean isDefault) {
        return (root, query, cb) -> cb.equal(root.get("isDefault"), isDefault);
    }

    public static Specification<Channel> hasRefId(Long refId) {
        return (root, query, cb) -> cb.equal(root.get("refId"), refId);
    }
}

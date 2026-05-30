package geumjeongyahak.domain.channel.service;

import geumjeongyahak.domain.channel.entity.Channel;
import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;
import geumjeongyahak.domain.channel.enums.ChannelBindingType;
import geumjeongyahak.domain.channel.enums.ChannelType;
import geumjeongyahak.domain.channel.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemChannelService {

    private final ChannelRepository channelRepository;

    @Transactional
    public Channel ensureDepartmentChannel(Long departmentId, String departmentName) {
        return channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType.DEPARTMENT, departmentId)
                .orElseGet(() -> createLinkedChannel(
                        departmentName,
                        departmentName + " 부서 전용 채널",
                        ChannelType.DEPARTMENT,
                        departmentId,
                        false,
                        ChannelAccessLevel.READ_WRITE
                ));
    }

    @Transactional
    public Channel ensureClassroomChannel(Long classroomId, String classroomName) {
        return channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType.CLASSROOM, classroomId)
                .orElseGet(() -> createLinkedChannel(
                        classroomName,
                        classroomName + " 분반 전용 채널",
                        ChannelType.CLASSROOM,
                        classroomId,
                        false,
                        ChannelAccessLevel.READ_WRITE
                ));
    }

    @Transactional
    public void deactivateClassroomChannel(Long classroomId) {
        channelRepository.findByChannelTypeAndBindingTypeAndRefIdAndIsDeletedFalse(
                ChannelType.CLASSROOM,
                ChannelBindingType.DOMAIN_LINKED,
                classroomId
            )
            .ifPresent(channel -> {
                channel.setActive(false);
                log.info("분반 연동 채널 비활성화 완료 - classroomId: {}, channelId: {}", classroomId, channel.getId());
            });
    }

    @Transactional
    public Channel ensureNoticeChannel(String name, String description, boolean isDefault) {
        return channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType.NOTICE, null)
                .orElseGet(() -> createSeedChannel(
                        name,
                        description,
                        ChannelType.NOTICE,
                        null,
                        isDefault,
                        ChannelAccessLevel.READ_ONLY,
                        true
                ));
    }

    @Transactional
    public Channel ensureEventChannel(String name, String description, boolean isDefault) {
        return channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType.EVENT, null)
                .orElseGet(() -> createSeedChannel(
                        name,
                        description,
                        ChannelType.EVENT,
                        null,
                        isDefault,
                        ChannelAccessLevel.READ_ONLY,
                        true
                ));
    }

    @Transactional
    public Channel ensureResourceChannel(String name, String description, boolean isDefault) {
        return channelRepository.findByChannelTypeAndRefIdAndIsDeletedFalse(ChannelType.RESOURCE, null)
                .orElseGet(() -> createSeedChannel(
                        name,
                        description,
                        ChannelType.RESOURCE,
                        null,
                        isDefault,
                        ChannelAccessLevel.READ_ONLY,
                        false
                ));
    }

    private Channel createLinkedChannel(
            String name,
            String description,
            ChannelType channelType,
            Long refId,
            boolean isDefault,
            ChannelAccessLevel accessLevel
    ) {
        Channel channel = channelRepository.save(Channel.builder()
                .name(name)
                .description(description)
                .channelType(channelType)
                .bindingType(ChannelBindingType.DOMAIN_LINKED)
                .refId(refId)
                .accessLevel(accessLevel)
                .isDefault(isDefault)
                .isActive(true)
                .build());

        log.info("시스템 채널 생성 완료 - type: {}, refId: {}, channelId: {}", channelType, refId, channel.getId());
        return channel;
    }

    private Channel createSeedChannel(
            String name,
            String description,
            ChannelType channelType,
            Long refId,
            boolean isDefault,
            ChannelAccessLevel accessLevel,
            boolean allowGuestRead
    ) {
        Channel channel = channelRepository.save(Channel.builder()
                .name(name)
                .description(description)
                .channelType(channelType)
                .bindingType(ChannelBindingType.STANDALONE)
                .refId(refId)
                .accessLevel(accessLevel)
                .allowGuestRead(allowGuestRead)
                .isDefault(isDefault)
                .isActive(true)
                .build());

        log.info("기본 채널 생성 완료 - type: {}, channelId: {}", channelType, channel.getId());
        return channel;
    }
}

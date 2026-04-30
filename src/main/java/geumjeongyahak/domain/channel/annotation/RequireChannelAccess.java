package geumjeongyahak.domain.channel.annotation;

import geumjeongyahak.domain.channel.enums.ChannelAccessLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireChannelAccess {
    ChannelAccessLevel minLevel() default ChannelAccessLevel.READ_ONLY;
    String channelIdParam() default "channelId";
}

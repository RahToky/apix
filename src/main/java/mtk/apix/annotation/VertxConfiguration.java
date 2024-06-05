package mtk.apix.annotation;

import mtk.apix.constant.DefaultVertxConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To put on the main class of the application if you want to specify some configurations related to vertex
 *
 * @author mahatoky rasolonirina
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VertxConfiguration {
    int eventLoopPoolSize() default DefaultVertxConfig.EVENT_POOL_SIZE;

    int idleTimeout() default DefaultVertxConfig.IDLE_TIMEOUT;

    boolean compressionSupported() default DefaultVertxConfig.COMPRESSION_SUPPORTED;
}

package mtk.apix.annotation;

import java.lang.annotation.*;

/**
 * All of these defaults are the real defaults of VertxOptions
 *
 * @author mahatoky rasolonirina
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApixVertxOptions {
    int eventLoopPoolSize() default 2;

    int workerPoolSize() default 20;

    int internalBlockingPoolSize() default 20;

    long maxEventLoopExecuteTime() default 2_000_000_000;

    long maxWorkerExecuteTime() default 60_000_000_000L;

    long warningExceptionTime() default 5_000_000_000L;

    long blockedThreadCheckInterval() default 1_000_000_000L;

    String haGroup() default "__DEFAULT__";

    boolean haEnable() default false;

    boolean disableTCCL() default false;

    boolean useDaemonThread() default false;

    boolean preferNativeTransport() default false;

}

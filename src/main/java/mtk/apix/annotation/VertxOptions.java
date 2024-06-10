package mtk.apix.annotation;

/**
 * All of these defaults are the real defaults of VertxOptions
 *
 * @author mahatoky rasolonirina
 */
public @interface VertxOptions {
    int eventLoopPoolSize() default 2;

    int workerPoolSize() default 20;

    int internalBlockingPoolSize() default 20;

    long maxEventLoopExecuteTime() default 2_000_000_000;

    long maxWorkerExecuteTime() default 60_000_000_000L;

    long warningExceptionTime() default 5_000_000_000L;

    long blockedThreadCheckInterval() default 1_000_000_000L;

    String haGroup() default "__DEFAULT__";

    String clusterHost() default "";

    int clusterPort() default 0;
}

package mtk.apix.annotation;

import mtk.apix.constant.DefaultVertxConfig;

/**
 * All of these defaults are the real defaults of Vertx HttpServerOptions
 *
 * @author mahatoky rasolonirina
 */
public @interface HttpServerOptions {

    int port() default DefaultVertxConfig.PORT;

    String host() default "";

    boolean compressionSupported() default false;

    boolean decompressionSupported() default true;

    int maxWebsocketFrameSize() default 65_536;

    int idleTimeout() default -1;

    int maxInitialLineLength() default 4_096;

    int maxHeaderSize() default 8_192;

    boolean handle100ContinueAutomatically() default false;

    boolean tcpFastOpen() default false;

    boolean reuseAddress() default true;
}

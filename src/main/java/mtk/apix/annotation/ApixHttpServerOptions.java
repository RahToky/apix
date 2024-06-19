package mtk.apix.annotation;

import mtk.apix.constant.ApixDefaultConfiguration;

import java.lang.annotation.*;

/**
 * All of these defaults are the real defaults of Vertx HttpServerOptions
 *
 * @author mahatoky rasolonirina
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApixHttpServerOptions {

    int port() default ApixDefaultConfiguration.PORT;

    boolean compressionSupported() default false;

    int compressionLevel() default 6;

    int maxWebsocketFrameSize() default 65536;

    int maxWebSocketMessageSize() default 262144;

    int maxChunkSize() default 8192;

    int maxFormAttributeSize() default 8192;

    int maxFormFields() default 256;

    int maxFormBufferedBytes() default 1024;

    boolean http2ClearTextEnabled() default true;

    int http2ConnectionWindowSize() default -1;

    boolean decompressionSupported() default false;

    boolean acceptUnmaskedFrames() default false;

    int decoderInitialBufferSize() default 128;

    boolean perFrameWebSocketCompressionSupported() default true;

    boolean perMessageWebSocketCompressionSupported() default true;

    boolean webSocketPreferredClientNoContext() default false;

    boolean webSocketAllowServerNoContext() default false;

    boolean registerWebSocketWriteHandlers() default false;

    int webSocketCompressionLevel() default 6;

    int webSocketClosingTimeout() default 10;

    int http2RstFloodMaxRstFramePerWindow() default 200;

    int http2RstFloodWindowDuration() default 30;

    boolean ssl() default false;
}

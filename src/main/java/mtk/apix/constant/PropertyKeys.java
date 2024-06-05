package mtk.apix.constant;

/**
 * Application-specific keys for the application.properties file
 * @author mahatoky rasolonirina
 */
public final class PropertyKeys {
    private PropertyKeys(){}

    public final static String APP_PORT = "apix.port";
    public final static String SHOW_LOG = "apix.debug";
    public final static String VERTX_EVENT_LOOP_POOL_SIZE = "apix.vertx.eventLoopPoolSize";
    public final static String VERTX_IDLE_TIMEOUT = "apix.vertx.IdleTimeout";
    public final static String VERTX_COMPRESSION_SUPPORTED = "apix.vertx.compressionSupported";
}

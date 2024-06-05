package mtk.apix.util;

import io.vertx.ext.web.RoutingContext;

/**
 * Custom interceptor must implement this interface to be considerate as valid interceptor
 * Custom interceptor must also be annotated with {@link mtk.apix.annotation.Interceptor}
 * @author mahatoky rasolonirina
 */
public interface ApixInterceptor {
    void intercept(RoutingContext routingContext);
}

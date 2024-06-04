package mtk.apix.util;

import io.vertx.ext.web.RoutingContext;

/**
 * @author mahatoky rasolonirina
 */
public interface ApixInterceptor {
    void intercept(RoutingContext routingContext);
}

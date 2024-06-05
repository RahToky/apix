package mtk.apix.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To put in front of a parameter of a method To retrieve the path parameters (path variables)
 * For example:
 * - endpoint : /api/users/:id
 * - method :
 * {@code
 *  public void getUserName(RoutingContext ctx, @PathParam("id") long id) {
 *      ctx.response().send(service.getUserById(id).toString());
 *  }
 * }
 *
 * @author mahatoky rasolonirina
 * @see PathVariable
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PathParam {
    String value();
}

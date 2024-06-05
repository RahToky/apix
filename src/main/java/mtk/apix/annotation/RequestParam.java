package mtk.apix.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be placed in front of a parameter of a method to retrieve query parameters.
 * For example:
 * - endpoint = /api/users?origin=www.my-website.com
 * - method = {@code getUsers(RoutingContext ctx, @RequestParam("origin") String origin)}
 *
 * @author mahatoky rasolonirina
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestParam {
    String value();
}

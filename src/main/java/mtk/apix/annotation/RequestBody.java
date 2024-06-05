package mtk.apix.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To put in front of a parameter of a method To retrieve the request body
 * For example:
 * - endpoint : /api/users (POST)
 * - method :
 * {@code
 *  @PostMapping()
 *  public void addUser(RoutingContext ctx, @RequestBody("user") User user) {
 *      long id = service.save(user);
 *      if(id>0)
 *        ctx.response().setStatus(201).send(id);
 *      else
 *        ctx.response().setStatus(500).send("error occurred");
 *  }
 * }
 *
 * @author mahatoky rasolonirina
 * @see PathVariable
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestBody {
}

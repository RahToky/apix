package mtk.apix.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To put on a class component which aims to provide a specific response for unhandled exceptions.
 * The class must contain at least one method annotated with {@link ExceptionHandler} for it to work, and the method must have at least one parameter of type RoutingContext of vertx
 *
 * @author mahatoky rasolonirina
 * @see Component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestControllerAdvice {
}

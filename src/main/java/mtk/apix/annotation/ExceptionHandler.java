package mtk.apix.annotation;

import mtk.apix.util.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To put on a method of a class which is annotated with RestControllerAdvice.
 * The method must have at least one parameter of type RoutingContext of vertX
 *
 * @author mahatoky rasolonirina
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionHandler {
    Class<? extends Throwable> value() default Throwable.class;

    String produce() default MediaType.APPLICATION_JSON;
}

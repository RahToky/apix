package mtk.apix.annotation;

import mtk.apix.util.MediaType;

import java.lang.annotation.*;

/**
 * @author mahatoky rasolonirina
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExceptionHandler {
    Class<? extends Throwable> value() default Throwable.class;

    String produce() default MediaType.APPLICATION_JSON;
}

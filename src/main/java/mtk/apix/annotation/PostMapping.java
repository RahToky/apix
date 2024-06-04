package mtk.apix.annotation;

import mtk.apix.util.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author mahatoky rasolonirina
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostMapping {
    String[] value() default {};

    String consume() default MediaType.APPLICATION_JSON;

    String produce() default MediaType.APPLICATION_JSON;

    String[] headers() default {};
}

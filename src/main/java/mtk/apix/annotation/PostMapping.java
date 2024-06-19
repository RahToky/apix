package mtk.apix.annotation;

import mtk.apix.util.MediaType;

import java.lang.annotation.*;

/**
 * @author mahatoky rasolonirina
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostMapping {
    String[] value() default {};

    String consume() default MediaType.APPLICATION_JSON;

    String produce() default MediaType.APPLICATION_JSON;

    String[] headers() default {};
}

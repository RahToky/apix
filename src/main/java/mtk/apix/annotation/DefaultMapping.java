package mtk.apix.annotation;

import mtk.apix.util.MediaType;

import java.lang.annotation.*;

/**
 * Method annotated with this annotation will be used as default mapping for every non-existent endpoint
 * @author mahatoky rasolonirina
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DefaultMapping {

    String produce() default MediaType.APPLICATION_JSON;
}

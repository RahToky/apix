package mtk.apix.annotation;

import mtk.apix.util.MediaType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method annotated with this annotation will be used as default mapping for every non-existent endpoint
 * @author mahatoky rasolonirina
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DefaultMapping {

    String produce() default MediaType.APPLICATION_JSON;
}

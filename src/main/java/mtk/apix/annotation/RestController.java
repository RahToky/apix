package mtk.apix.annotation;

import java.lang.annotation.*;

/**
 * @author mtk_ext
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestController {
    String prefix() default "";
}

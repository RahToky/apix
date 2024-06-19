package mtk.apix.annotation;

import java.lang.annotation.*;

/**
 * @author mahatoky rasolonirina
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Value {
    String value();
    String defaultValue() default "";
}

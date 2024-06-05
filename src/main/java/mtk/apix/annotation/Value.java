package mtk.apix.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To put on a property of a class to indicate that its value is found in an application.properties file by specifying the key.
 * It is possible to add a default value if the key cannot be found in the file or if the file does not exist
 * @author mahatoky rasolonirina
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Value {
    String value();

    String defaultValue() default "";
}

package mtk.apix.exception;

/**
 * @author mahatoky rasolonirina
 */
public class NoSuchBeanDefinitionException extends RuntimeException {

    public NoSuchBeanDefinitionException(Class<?> aClass) {
        super("No qualifying bean of type '" + aClass + "' available");
    }

}

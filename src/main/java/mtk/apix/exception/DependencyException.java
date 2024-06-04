package mtk.apix.exception;

/**
 * @author mahatoky
 */
public class DependencyException extends RuntimeException {

    public DependencyException(){}

    public DependencyException(String message){
        super(message);
    }

    public DependencyException(Throwable throwable){
        super(throwable);
    }

}

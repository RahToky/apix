# properties file:
 To specify the properties file, put environment in argument like (--local, --dev, --prod). App will use application.properties as default if no environment is specified
### sample:
````
apix.port=9204 # default port is 9204 if not specified
apix.debug=true #to show/hide log default true
apix.vertx.eventLoopPoolSize=int - for vertx loop pool size
apix.vertx.IdleTimeout=int - for vertx idle timeout
apix.vertx.compressionSupported=bool - for vertx compression support
````

# Annotations:
> Annotation is priority before application properties
### mandatory:
- @ApixApplication: (on main class)
- @Component: (on class) for component class
- @Service: (on class) for service class
- @Repository: (on class) for repository class
- @RestController: (on class) for controller
- @GetMapping: (on method) to create http get endpoint
- @PostMapping: (on method) to create http post endpoint
- @PutMapping: (on method) to create http put endpoint
- @DeleteMapping: (on method) to create http delete endpoint
### optionals:
- @Autowired: (on field) to inject dependency. Class using this must annotate as component
- @ComponentScan: (on main class) to specify base package
- @VertxConfiguration: (on main class) for vertx configuration, can be replaced by properties file.
- @PathVariable: (on parameter) get variable in url ex: "/api/:id". "id" is the param
- @PathParam: (on parameter) same as @PathVariable
- @RequestParam: (on parameter) get query param in url ex: "/api?name=Boo". "name" is the param
- @RequestBody: (on parameter) parse body to object
- @PostConstruct: (on method) auto run method after constructor
- @Interceptor: (on class) class must extend ApixInterceptor to intercept all request before controller
- @Value: (on field) to inject value from properties
- @Bean: (on method) to create bean (instance managed by Apix), method must return object. Objet will be a component
- @Configuration: (on class) like a component but specially to create bean.
- @RestControllerAdvice: (on class) to catch unhandled exception and create appropriate response
- @ExceptionHandler: (on method) method must have a RoutingContext parameter and wrapped in class annotate with @RestControllerAdvice

# Dependency Injection
 For dependency injection, annotate field with @Autowired an ApixContainer will inject automatically the instance.
 Only class annotate with @RestController, @RestControllerAdvice, @Component, @Service, @Repository, @ApixApplication and Object returned by method annotate with @Bean can be considered as a component.
 All methods of a component can use other component as parameter and the injection will be done automatically.
 

# Sample Code
## Main class

````
@ApixApplication
@ComponentScan("mg.app")
@VertxConfiguration(idleTimeout=30000)
public class MyApp {
    public static void main(String[] args){
        Apix.run(main.class, args);
    }
}
`````
## RestController

````
@RestController("/api")
public class MyController {
    
    @Autowired
    private IService service;

    @DefaultMapping
    public void notFound(RoutingContext ctx){
        ctx.response().end("your endpoint does not exist");
    }

    @GetMapping("/index")
    public void index(RoutingContext ctx){
        ctx.response().end("welcome to " + service.getAppName());
    }
}
````
## Service
````
@Service
public class ServiceImpl implement IService {
    
    @Value("app.name", defaultValue="MyApi")
    private String appName;

    @Override
    public String getAppName(){
        return appName;
    }
    
}
````
## Configuration
````
@Configuration
public class MyConfig {

    @Bean
    public StringUtil createStringUtilComponent(){
        return new StringUtil();
    }
    
}
````
## RestControllerAdvice
````
@RestControllerAdvice
public class MyControllerAdvice {

    @ExceptionHandler(UserNotFoundException.class)
    public void handleUNFE(RoutingContext ctx, Throwable t){
        ctx.response().end("user not found:" + t.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public void handleGlobalException(RoutingContext ctx, Throwable t){
        ctx.response().end("error occured:" + t.getMessage());
    }
    
}
````
## Interceptor
````
@Interceptor
public class MyInterceptor extends ApixInterceptor{

    @Override
    public void intercept(RoutingContext ctx){
        System.out.println("call on endpoint: " + ctx.request().absoluteURI());
        ctx.next();
    }
    
}
````


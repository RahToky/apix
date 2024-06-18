package mtk.apix;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import mtk.apix.annotation.*;
import mtk.apix.constant.DefaultVertxConfig;
import mtk.apix.constant.PropertyKeys;
import mtk.apix.exception.DependencyException;
import mtk.apix.util.ClassUtil;
import mtk.apix.util.ConsoleLog;
import mtk.apix.util.Environment;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Based on vertx-web 4.5.7
 * Main class that the user should consider.
 * The application is started using the static run method
 *
 * @author mahatoky rasolonirina
 */
@SuppressWarnings("unchecked")
public class Apix {

    private static Apix instance;
    private final ApixContainer apixContainer;
    private final ApixProperties apixProperties;
    private Vertx vertx;
    private final static int DEFAULT_PORT = 9204;
    private int port;
    private Handler<HttpServer> onSuccessHandler;
    private Handler<Throwable> onFailureHandler;
    private Environment env;
    private Class<?> mainClass;

    private Apix() {
        port = DEFAULT_PORT;
        env = Environment.DEFAULT;
        apixContainer = new ApixContainer();
        apixProperties = new ApixProperties();
    }

    public static Apix getInstance() {
        if (instance == null) {
            instance = new Apix();
        }
        return instance;
    }

    /**
     * Main class is mandatory to locate package and all needed resources
     * Args parameter is mandatory to know environment, it's primordial to choose the appropriate application properties file, see command below:
     * - for application-local.properties: --local (priority 1)
     * - for application-dev.properties: --dev (priority 2)
     * - for application-prod.properties: --prod (priority 3)
     * Warning: if command contains all environment command, application use the lower priority
     *
     * @param mainClass the main class of your application. Must annotate with {@link ApixApplication}
     * @param args
     */
    public static void run(Class<?> mainClass, String[] args) {
        run(mainClass, args, null, null);
    }

    /**
     * Main class is mandatory to locate package and all needed resources
     * Args parameter is mandatory to know environment, it's primordial to choose the appropriate application properties file, see command below:
     * - for application-local.properties: --local (priority 1)
     * - for application-dev.properties: --dev (priority 2)
     * - for application-prod.properties: --prod (priority 3)
     * Warning: if command contains all environment command, application use the lower priority
     *
     * @param mainClass        the main class of your application. Must annotate with {@link ApixApplication}
     * @param args
     * @param onSuccessHandler to handle success (optional)
     * @param onFailureHandler to handle failure (optional)
     */
    public static void run(Class<?> mainClass, String[] args, Handler<HttpServer> onSuccessHandler, Handler<Throwable> onFailureHandler) {
        try {
            if (!mainClass.isAnnotationPresent(ApixApplication.class)) {
                throw new RuntimeException("Main class must annotated with @ApixApplication");
            }

            Apix apix = Apix.getInstance();
            apix.onSuccessHandler = onSuccessHandler;
            apix.onFailureHandler = onFailureHandler;
            apix.mainClass = mainClass;

            List<String> argsList = Arrays.asList(args);
            if (argsList.contains("--local")) {
                apix.env = Environment.LOCAL;
            } else if (argsList.contains("--dev")) {
                apix.env = Environment.DEV;
            } else if (argsList.contains("--prod")) {
                apix.env = Environment.PROD;
            }

            apix.displayApixLogo();
            apix.apixProperties.init(mainClass, apix.env);
            apix.initVertx();
            apix.showLog(apix.apixProperties.getApplicationProperties());
            apix.apixContainer.addComponent(apix.vertx.getClass(), apix.vertx);
            apix.apixContainer.init(mainClass, apix.apixProperties.getApplicationProperties(), apix.env);

            if (!apix.apixContainer.getRestControllers().isEmpty()) {
                Router router = Router.router(apix.vertx);
                router.route().handler(BodyHandler.create());
                apix.fixPort(apix.apixProperties.getApplicationProperties());
                apix.createInterceptor(router);
                apix.createEndpoints(router);
                apix.createDefaultEndpoint(router);
                apix.createControllerAdvice(router);
                apix.startServer(router, httpServer -> apix.apixContainer.invokeAllPostConstructComponentsMethod());
            } else {
                ConsoleLog.warn("Server not started: no controller found!");
            }
        } catch (Exception e) {
            throw new DependencyException(e);
        }
    }

    private void initVertx() {
        VertxOptions vertxOptions = new VertxOptions();

        Integer eventLoopPoolSize = DefaultVertxConfig.EVENT_POOL_SIZE;

        try {
            if (mainClass != null && mainClass.isAnnotationPresent(VertxConfiguration.class)) {
                eventLoopPoolSize = mainClass.getAnnotation(VertxConfiguration.class).eventLoopPoolSize();
            } else if (apixProperties.getApplicationProperties() != null && apixProperties.getApplicationProperties().containsKey(PropertyKeys.VERTX_EVENT_LOOP_POOL_SIZE)) {
                eventLoopPoolSize = ClassUtil.valueOf(apixProperties.getApplicationProperties().getProperty(PropertyKeys.VERTX_EVENT_LOOP_POOL_SIZE), Integer.class, DefaultVertxConfig.EVENT_POOL_SIZE);
            }
        } finally {
            if (eventLoopPoolSize == null)
                eventLoopPoolSize = DefaultVertxConfig.EVENT_POOL_SIZE;
        }

        vertxOptions.setEventLoopPoolSize(eventLoopPoolSize);
        vertx = Vertx.vertx(vertxOptions);
    }

    private HttpServerOptions createServerOptions() {
        HttpServerOptions httpServerOptions = new HttpServerOptions();
        Integer idleTimeout = DefaultVertxConfig.IDLE_TIMEOUT;
        Boolean compressionSupported = DefaultVertxConfig.COMPRESSION_SUPPORTED;
        try {
            if (mainClass.isAnnotationPresent(VertxConfiguration.class)) {
                idleTimeout = mainClass.getAnnotation(VertxConfiguration.class).idleTimeout();
                compressionSupported = mainClass.getAnnotation(VertxConfiguration.class).compressionSupported();
            } else if (apixProperties.getApplicationProperties().containsKey(PropertyKeys.VERTX_IDLE_TIMEOUT)) {
                idleTimeout = ClassUtil.valueOf(apixProperties.getApplicationProperties().getProperty(PropertyKeys.VERTX_IDLE_TIMEOUT), Integer.class, DefaultVertxConfig.IDLE_TIMEOUT);
                compressionSupported = ClassUtil.valueOf(apixProperties.getApplicationProperties().getProperty(PropertyKeys.VERTX_COMPRESSION_SUPPORTED), Boolean.class, DefaultVertxConfig.COMPRESSION_SUPPORTED);
            }
        } finally {
            if (idleTimeout == null)
                idleTimeout = DefaultVertxConfig.IDLE_TIMEOUT;
            if (compressionSupported == null)
                compressionSupported = DefaultVertxConfig.COMPRESSION_SUPPORTED;
        }
        httpServerOptions.setCompressionSupported(compressionSupported);
        httpServerOptions.setIdleTimeout(idleTimeout);
        return httpServerOptions;
    }

    private void startServer(Router router, Handler<HttpServer> onStart) {
        HttpServer httpServer = vertx.createHttpServer(createServerOptions());
        httpServer
                .requestHandler(router)
                .listen(port)
                .onSuccess(server -> {
                    ConsoleLog.forcedLog(ConsoleLog.Level.INFO, "HTTP server started on port " + server.actualPort() + " (" + env.name() + ") - visit http://localhost:" + server.actualPort() + "/");
                    if (onStart != null)
                        onStart.handle(server);
                    if (onSuccessHandler != null)
                        onSuccessHandler.handle(server);
                })
                .onFailure(throwable -> {
                    httpServer.close();
                    ConsoleLog.error(new Throwable("Can't start server on port " + port, throwable));
                    if (onFailureHandler != null)
                        onFailureHandler.handle(throwable);
                });
    }


    /**
     * Create interceptor (front controller) if a class annotate with {@link Interceptor} and inherit {@link mtk.apix.util.ApixInterceptor} is present
     *
     * @param router
     */
    private void createInterceptor(Router router) {
        Map.Entry<Class<?>, Object> interceptor = apixContainer.getInterceptor();
        ConsoleLog.trace("Interceptor: " + (interceptor != null ? interceptor.getKey().getName() : "none"));
        if (interceptor != null) {
            router.route().handler(routingContext -> {
                try {
                    Method interceptMethod = interceptor.getKey().getMethod("intercept", RoutingContext.class);
                    interceptMethod.invoke(interceptor.getValue(), routingContext);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * Create all route according to all method annotate with {@link RestController} and these method annotate with all method annotation like {@link GetMapping}, {@link PostMapping},{@link PutMapping},{@link DefaultMapping},{@link DefaultMapping}
     * Throws an RuntimeException if a method annotated with http method annotation exists, but it contains no argument of type {@link RoutingContext}
     *
     * @param router
     */
    private void createEndpoints(Router router) {
        List<Object> controllers = apixContainer.getRestControllers();
        List<Object> components = Collections.singletonList(apixContainer.getComponents().values());
        int validCreatedEndpoint = 0;
        for (Object controller : controllers) {
            Method[] apiMethods = controller.getClass().getDeclaredMethods();
            String endpointPrefix = controller.getClass().getAnnotation(RestController.class).prefix();
            for (Method method : apiMethods) {
                if (ClassUtil.isMethodAnnotatedWithAny(method, ApixContainer.httpMethodAnnotation)) {
                    Parameter[] parameters = method.getParameters();
                    if (parameters.length > 0 && ClassUtil.contains(parameters, RoutingContext.class)) {
                        if (method.isAnnotationPresent(PostMapping.class)) {
                            validCreatedEndpoint++;
                            Arrays.asList(method.getAnnotation(PostMapping.class).value()).forEach(endPoint -> {
                                router.post(endpointPrefix + endPoint)
                                        .consumes(method.getAnnotation(PostMapping.class).consume())
                                        .produces(method.getAnnotation(PostMapping.class).produce())
                                        .handler(routingContext -> ClassUtil.invokeHttpMethod(controller, method, routingContext, components));
                            });
                        }
                        if (method.isAnnotationPresent(DeleteMapping.class)) {
                            validCreatedEndpoint++;
                            Arrays.asList(method.getAnnotation(DeleteMapping.class).value()).forEach(endPoint -> {
                                router.delete(endpointPrefix + endPoint)
                                        .consumes(method.getAnnotation(DeleteMapping.class).consume())
                                        .produces(method.getAnnotation(DeleteMapping.class).produce())
                                        .handler(routingContext -> ClassUtil.invokeHttpMethod(controller, method, routingContext, components));
                            });
                        }
                        if (method.isAnnotationPresent(PutMapping.class)) {
                            validCreatedEndpoint++;
                            Arrays.asList(method.getAnnotation(PutMapping.class).value()).forEach(endPoint -> {
                                router.put(endpointPrefix + endPoint)
                                        .consumes(method.getAnnotation(PutMapping.class).consume())
                                        .produces(method.getAnnotation(PutMapping.class).produce())
                                        .handler(routingContext -> ClassUtil.invokeHttpMethod(controller, method, routingContext, components));
                            });
                        }
                        if (method.isAnnotationPresent(GetMapping.class)) {
                            validCreatedEndpoint++;
                            Arrays.asList(method.getAnnotation(GetMapping.class).value()).forEach(endPoint -> {
                                Route route = router.get(endpointPrefix + endPoint);
                                String consume = method.getAnnotation(GetMapping.class).consume();
                                if (!consume.isEmpty()) {
                                    route.consumes(consume);
                                }
                                route.produces(method.getAnnotation(GetMapping.class).produce());
                                route.handler(routingContext -> ClassUtil.invokeHttpMethod(controller, method, routingContext, components));
                            });
                        }
                    } else {
                        String parameterTypes = Arrays.stream(parameters)
                                .map(Parameter::getType)
                                .map(Class::getSimpleName)
                                .collect(Collectors.joining(", "));
                        throw new RuntimeException("Can't create endpoint for method '" + method.getName() + "(" + parameterTypes + ")', cause: no suitable parameter found.");
                    }
                }
            }
        }
        ConsoleLog.trace("Controllers: (" + controllers.size() + ") found, httpMethod: (" + validCreatedEndpoint + ") found");
    }

    /**
     * Default endpoint is method annotated with {@link DefaultMapping} wrapped in a Class annotated with {@link RestController}
     * At least, method must have one argument and must have class inherit from {@link RoutingContext}
     *
     * @param router
     */
    private void createDefaultEndpoint(Router router) {
        for (Object controller : apixContainer.getRestControllers()) {
            Method[] methods = controller.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (ClassUtil.isMethodAnnotatedWithAny(method, DefaultMapping.class)) {
                    Parameter[] parameters = method.getParameters();
                    if (parameters.length > 0 && ClassUtil.contains(parameters, RoutingContext.class)) {
                        router.route().produces(method.getAnnotation(DefaultMapping.class).produce())
                                .handler(routingContext -> ClassUtil.invokeHttpMethod(controller, method, routingContext, Collections.singletonList(apixContainer.getComponents().values())));
                        ConsoleLog.trace("Default endpoint: " + controller.getClass().getName() + "." + method.getName());
                        return;
                    }
                }
            }
        }
        ConsoleLog.trace("No specified default endpoint found");
    }

    /**
     * Create route to handle all unhandled exception
     * A controller advise is a class annotated with {@link RestControllerAdvice}
     * Must contain at least one argument of type {@link RoutingContext} or which inherits from this one
     *
     * @param router
     */
    private void createControllerAdvice(Router router) {
        List<Object> controllersAdvice = apixContainer.getControllersAdvice();
        ConsoleLog.trace("ControllersAdvices : (" + controllersAdvice.size() + ") found");
        if (!controllersAdvice.isEmpty()) {
            router.errorHandler(500, routingContext -> {
                try {
                    Throwable cause = routingContext.failure().getCause().getCause();
                    List<Object> finalDependencies = new ArrayList<>(apixContainer.getComponents().values());
                    finalDependencies.add(cause);
                    finalDependencies.add(routingContext);
                    for (Object controller : controllersAdvice) {
                        Method[] methods = controller.getClass().getDeclaredMethods();
                        Method globalExceptionMetod = null;
                        for (Method method : methods) {
                            if (ClassUtil.isMethodAnnotatedWithAny(method, ExceptionHandler.class)) {
                                if (RuntimeException.class.equals(method.getAnnotation(ExceptionHandler.class).value())) {
                                    globalExceptionMetod = method;
                                }
                                Parameter[] parameters = method.getParameters();
                                if (parameters.length > 0 && ClassUtil.contains(parameters, RoutingContext.class) && method.getAnnotation(ExceptionHandler.class).value().equals(cause.getClass())) {
                                    ClassUtil.invokeMethod(controller, method, finalDependencies);
                                    return;
                                }
                            }
                        }

                        if (globalExceptionMetod != null && !routingContext.response().ended()) {
                            ClassUtil.invokeMethod(controller, globalExceptionMetod, finalDependencies);
                            return;
                        }
                    }
                } catch (Exception e) {
                    if (!routingContext.response().ended())
                        routingContext.response().end(routingContext.failure().getMessage());
                }
            });
        }
    }

    private void displayApixLogo() {
        System.out.println("       _       ____     _   __     __                              ");
        System.out.println("      / \\     |  _ \\   | |  \\ \\   / /                              ");
        System.out.println("     / _ \\    | |_) |  | |   \\ \\ / /       0 4 / 2 0 2 4           ");
        System.out.println("    / ___ \\   |  _ /   | |   / / \\ \\       V E R S I O N . 1.0.1   ");
        System.out.println("   /_/   \\_\\  |_|      |_|  /_/   \\_\\                              \n");
    }

    /**
     * It will look for the port in the given parameter, if it does not find one or if there is an error, it will use the default port {@link Apix#DEFAULT_PORT}
     *
     * @param properties
     */
    private void fixPort(Properties properties) {
        try {
            String strPort = properties.getProperty(PropertyKeys.APP_PORT);
            this.port = Integer.parseInt(strPort);
        } catch (Exception e) {
            this.port = DEFAULT_PORT;
        }
    }

    private void showLog(Properties properties) {
        if (properties != null && properties.containsKey(PropertyKeys.SHOW_LOG)) {
            try {
                ConsoleLog.getInstance().setShow(Boolean.parseBoolean(properties.getProperty(PropertyKeys.SHOW_LOG)));
            } catch (Exception e) {

            }
        }
    }

    public static Vertx vertx() {
        return Apix.getInstance().vertx;
    }

    protected Apix onSuccessHandler(Handler<HttpServer> onSuccessHandler) {
        this.onSuccessHandler = onSuccessHandler;
        return this;
    }

    public Apix onFailureHandler(Handler<Throwable> onFailureHandler) {
        this.onFailureHandler = onFailureHandler;
        return this;
    }

    public void setEnv(Environment env) {
        this.env = env;
    }

    public Environment getEnv() {
        return env;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return (T) getInstance().apixContainer.getComponent(beanClass);
    }

    public static Properties getProperties() {
        return getInstance().apixProperties.getApplicationProperties();
    }
}

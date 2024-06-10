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
import mtk.apix.constant.ApixDefaultConfiguration;
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
 *
 * @author mahatoky rasolonirina
 */
@SuppressWarnings("unchecked")
public class Apix {

    private static Apix instance;
    private final ApixContainer apixContainer;
    private final ApixProperties apixProperties;
    private Handler<HttpServer> onSuccessHandler;
    private Handler<Throwable> onFailureHandler;
    private Environment env;
    private Class<?> mainClass;
    private Vertx vertx;
    private VertxOptions vertxOptions;
    private HttpServerOptions httpServerOptions;
    private int port;

    private Apix() {
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
        getInstance().runApp(mainClass, args);
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
        Apix apix = getInstance();
        apix.onSuccessHandler(onSuccessHandler);
        apix.onFailureHandler(onFailureHandler);
        apix.runApp(mainClass, args);
    }

    public void runApp(Class<?> mainClass, String[] args) {
        try {
            if (!mainClass.isAnnotationPresent(ApixApplication.class)) {
                throw new RuntimeException("Main class must annotate with @ApixApplication");
            }
            this.mainClass = mainClass;
            List<String> argsList = Arrays.asList(args);
            if (argsList.contains("--local")) {
                this.env = Environment.LOCAL;
            } else if (argsList.contains("--dev")) {
                this.env = Environment.DEV;
            } else if (argsList.contains("--prod")) {
                this.env = Environment.PROD;
            }

            this.displayApixLogo();
            this.apixProperties.init(mainClass, this.env);
            this.initVertx();
            this.showLog(this.apixProperties.getApplicationProperties());
            this.apixContainer.addComponent(this.vertx.getClass(), this.vertx);
            this.apixContainer.init(mainClass, this.apixProperties.getApplicationProperties(), this.env);

            System.out.println("--------> controllersSize=" + apixContainer.getRestControllers().size());
            if (!this.apixContainer.getRestControllers().isEmpty()) {
                Router router = Router.router(this.vertx);
                router.route().handler(BodyHandler.create());
                this.fixPort(this.apixProperties.getApplicationProperties());
                this.createInterceptor(router);
                this.createEndpoints(router);
                this.createDefaultEndpoint(router);
                this.createControllerAdvice(router);
                this.startServer(router, httpServer -> this.apixContainer.invokeAllPostConstructComponentsMethod());
            } else {
                ConsoleLog.warn("Server not started: no controller found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void initVertx() {
        try {
            if (vertxOptions == null) {
                vertxOptions = new VertxOptions();
                if (mainClass.isAnnotationPresent(ApixVertxOptions.class)) {
                    ApixVertxOptions apixVertxOptions = mainClass.getAnnotation(ApixVertxOptions.class);
                    vertxOptions.setEventLoopPoolSize(apixVertxOptions.eventLoopPoolSize());
                    vertxOptions.setDisableTCCL(apixVertxOptions.disableTCCL());
                    vertxOptions.setBlockedThreadCheckInterval(apixVertxOptions.blockedThreadCheckInterval());
                    vertxOptions.setWorkerPoolSize(apixVertxOptions.workerPoolSize());
                    vertxOptions.setInternalBlockingPoolSize(apixVertxOptions.internalBlockingPoolSize());
                    vertxOptions.setMaxEventLoopExecuteTime(apixVertxOptions.maxEventLoopExecuteTime());
                    vertxOptions.setMaxWorkerExecuteTime(apixVertxOptions.maxWorkerExecuteTime());
                    vertxOptions.setWarningExceptionTime(apixVertxOptions.warningExceptionTime());
                    vertxOptions.setHAGroup(apixVertxOptions.haGroup());
                    vertxOptions.setHAEnabled(apixVertxOptions.haEnable());
                    vertxOptions.setUseDaemonThread(apixVertxOptions.useDaemonThread());
                    vertxOptions.setPreferNativeTransport(apixVertxOptions.preferNativeTransport());
                }
            }
        } catch (Exception e) {
            ConsoleLog.error(e);
        } finally {
            vertx = Vertx.vertx(vertxOptions);
        }
    }

    private void startServer(Router router, Handler<HttpServer> onStart) {
        try {
            if (httpServerOptions == null) {
                httpServerOptions = new HttpServerOptions();
                if (mainClass.isAnnotationPresent(ApixHttpServerOptions.class)) {
                    ApixHttpServerOptions apixHttpServerOptions = mainClass.getAnnotation(ApixHttpServerOptions.class);
                    httpServerOptions.setPort(port != 0 ? port : apixHttpServerOptions.port());
                    httpServerOptions.setSsl(apixHttpServerOptions.ssl());
                    httpServerOptions.setCompressionSupported(apixHttpServerOptions.compressionSupported());
                    httpServerOptions.setCompressionLevel(apixHttpServerOptions.compressionLevel());
                    httpServerOptions.setMaxWebSocketFrameSize(apixHttpServerOptions.maxWebsocketFrameSize());
                    httpServerOptions.setMaxWebSocketMessageSize(apixHttpServerOptions.maxWebSocketMessageSize());
                    httpServerOptions.setMaxChunkSize(apixHttpServerOptions.maxChunkSize());
                    httpServerOptions.setMaxFormAttributeSize(apixHttpServerOptions.maxFormAttributeSize());
                    httpServerOptions.setMaxFormFields(apixHttpServerOptions.maxFormFields());
                    httpServerOptions.setMaxFormBufferedBytes(apixHttpServerOptions.maxFormBufferedBytes());
                    httpServerOptions.setHttp2ClearTextEnabled(apixHttpServerOptions.http2ClearTextEnabled());
                    httpServerOptions.setHttp2ConnectionWindowSize(apixHttpServerOptions.http2ConnectionWindowSize());
                    httpServerOptions.setDecompressionSupported(apixHttpServerOptions.decompressionSupported());
                    httpServerOptions.setAcceptUnmaskedFrames(apixHttpServerOptions.acceptUnmaskedFrames());
                    httpServerOptions.setDecoderInitialBufferSize(apixHttpServerOptions.decoderInitialBufferSize());
                    httpServerOptions.setPerFrameWebSocketCompressionSupported(apixHttpServerOptions.perFrameWebSocketCompressionSupported());
                    httpServerOptions.setPerMessageWebSocketCompressionSupported(apixHttpServerOptions.perMessageWebSocketCompressionSupported());
                    httpServerOptions.setWebSocketPreferredClientNoContext(apixHttpServerOptions.webSocketPreferredClientNoContext());
                    httpServerOptions.setWebSocketAllowServerNoContext(apixHttpServerOptions.webSocketAllowServerNoContext());
                    httpServerOptions.setRegisterWebSocketWriteHandlers(apixHttpServerOptions.registerWebSocketWriteHandlers());
                    httpServerOptions.setCompressionLevel(apixHttpServerOptions.compressionLevel());
                    httpServerOptions.setWebSocketClosingTimeout(apixHttpServerOptions.webSocketClosingTimeout());
                    httpServerOptions.setHttp2RstFloodMaxRstFramePerWindow(apixHttpServerOptions.http2RstFloodMaxRstFramePerWindow());
                    httpServerOptions.setHttp2RstFloodWindowDuration(apixHttpServerOptions.http2RstFloodWindowDuration());
                }
            }
        } catch (Exception e) {
            ConsoleLog.error(e);
        } finally {
            HttpServer httpServer = vertx.createHttpServer(httpServerOptions);
            httpServer
                    .requestHandler(router)
                    .listen()
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
                if (ClassUtil.isMethodAnnotatedWith(method, ApixContainer.httpMethodAnnotation)) {
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
                if (ClassUtil.isMethodAnnotatedWith(method, DefaultMapping.class)) {
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
                            if (ClassUtil.isMethodAnnotatedWith(method, ExceptionHandler.class)) {
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
        System.out.println("     / _ \\    | |_) |  | |   \\ \\ / /       0 6 / 2 0 2 4           ");
        System.out.println("    / ___ \\   |  _ /   | |   / / \\ \\       V E R S I O N . 2.0.0   ");
        System.out.println("   /_/   \\_\\  |_|      |_|  /_/   \\_\\                              \n");
    }

    /**
     * It will look for the port in the given parameter, if it does not find one or if there is an error, it will use the default port {@link ApixDefaultConfiguration#PORT}
     *
     * @param properties
     */
    private void fixPort(Properties properties) {
        try {
            String strPort = properties.getProperty(PropertyKeys.APP_PORT);
            this.port = Integer.parseInt(strPort);
        } catch (Exception e) {
            this.port = ApixDefaultConfiguration.PORT;
        }
    }

    public Apix setPort(int port) {
        this.port = port;
        return this;
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

    public Apix onSuccessHandler(Handler<HttpServer> onSuccessHandler) {
        this.onSuccessHandler = onSuccessHandler;
        return this;
    }

    public Apix onFailureHandler(Handler<Throwable> onFailureHandler) {
        this.onFailureHandler = onFailureHandler;
        return this;
    }

    public Apix setEnv(Environment env) {
        this.env = env;
        return this;
    }

    public Apix setVertxOptions(VertxOptions vertxOptions) {
        this.vertxOptions = vertxOptions;
        return this;
    }

    public Apix setHttpServerOptions(HttpServerOptions httpServerOptions) {
        this.httpServerOptions = httpServerOptions;
        return this;
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

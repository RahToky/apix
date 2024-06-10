package mtk.apix;

import mtk.apix.annotation.*;
import mtk.apix.exception.DependencyException;
import mtk.apix.exception.NoSuchBeanDefinitionException;
import mtk.apix.util.ApixInterceptor;
import mtk.apix.util.ClassUtil;
import mtk.apix.util.ConsoleLog;
import mtk.apix.util.Environment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author mahatoky rasolonirina
 */
@SuppressWarnings("unchecked")
class ApixContainer {

    private final Map<Class<?>, Object> components = new HashMap<>();
    public static Class<? extends Annotation>[] componentsAnnotations;
    public static Class<? extends Annotation>[] controllerAnnotations;
    public static Class<? extends Annotation>[] httpMethodAnnotation;
    private Properties applicationProperties = new Properties();


    static {
        componentsAnnotations = new Class[]{RestController.class, Service.class, Repository.class, Component.class, RestControllerAdvice.class, Interceptor.class, Configuration.class};
        controllerAnnotations = new Class[]{RestController.class, RestControllerAdvice.class};
        httpMethodAnnotation = new Class[]{GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class, DefaultMapping.class};
    }

    public ApixContainer() {
    }

    /**
     * Scan all component classes in giving package. Component class is marked with @Component, @RestController, @Service and @Repository
     * Instantiate every class and store in {@link #components} properties as key value (class, instance)
     * Instantiate all properties annotated with @Autowired
     *
     * @param mainClass   to locate appropriate properties according to his ClassLoader
     * @param environment to select what application properties use
     */
    public void init(Class<?> mainClass, Properties properties, Environment environment) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        this.applicationProperties = properties;
        initAllComponents(mainClass, getBasePackage(mainClass));
        fillComponentsFieldsMarkedWithValue();
    }

    /**
     * Instantiate all component market with {@link ApixContainer#componentsAnnotations}
     * Inject all autowired fields
     * Inject all fields marked with {@link Value}
     * Save every created instance in {@link #components}
     *
     * @param basePackage
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void initAllComponents(Class<?> mainClass, String basePackage) throws InstantiationException, IllegalAccessException {
        Set<Class<?>> componentsClasses = ClassUtil.getAnnotatedClass(mainClass, basePackage, ApixContainer.componentsAnnotations);
        if (componentsClasses.isEmpty())
            return;
        Set<Class<?>> tempComponentsClasses = new HashSet<>(componentsClasses);

        while (!tempComponentsClasses.isEmpty()) {
            for (Iterator<Class<?>> iterator = tempComponentsClasses.iterator(); iterator.hasNext(); ) {
                Class<?> componentClass = iterator.next();
                if (isComponentReadyForInstantiation(componentClass)) {
                    Object objectInstance = componentClass.newInstance();
                    fillComponentFieldsMarkedWithValue(objectInstance);
                    runAllConfigurationBeanCreation(objectInstance);
                    instantiateAllAutowiredFields(objectInstance);
                    components.put(componentClass, objectInstance);
                    iterator.remove();
                } else if (!iterator.hasNext()) {
                    throw new NoSuchBeanDefinitionException(getUnsatisfiedAutowired(componentClass));
                }
            }
        }
    }

    /**
     * A component is ready if:
     * - no autowired fields
     * - or all autowired fields are available in {@link #components}
     *
     * @param componentClass
     * @return
     */
    private boolean isComponentReadyForInstantiation(Class<?> componentClass) {
        List<Field> autowiredFields = ClassUtil.getCurrentAndInheritedAnnotatedFields(componentClass, Autowired.class);
        if (autowiredFields.isEmpty()) {
            return true;
        }
        for (Field field : autowiredFields) {
            if (getFirstAssignableComponent(field.getType()) == null) {
                return false;
            }
        }
        return true;
    }

    private Class<?> getUnsatisfiedAutowired(Class<?> componentClass){
        List<Field> autowiredFields = ClassUtil.getCurrentAndInheritedAnnotatedFields(componentClass, Autowired.class);
        for (Field field : autowiredFields) {
            if (getAssignableClasses(field.getType()).isEmpty()) {
                return field.getType();
            }
        }
        return null;
    }

    /**
     * Only component class benefit from dependency injection. And only properties annotated with @Autowired are managed.
     * * Find all properties annotated with @Autowired, find instance in {@link #components} then inject instance to properties
     * * If no instance found in {@link #components}, throw {@link DependencyException}
     *
     * @param component
     */
    private void instantiateAllAutowiredFields(Object component) {
        List<Field> objectFields = ClassUtil.getCurrentAndInheritedAnnotatedFields(component.getClass(), Autowired.class);
        for (Field field : objectFields) {
            Object autowiredComponentField = getFirstAssignableComponent(field.getType());
            if (autowiredComponentField != null) {
                try {
                    field.setAccessible(true);
                    field.set(component, autowiredComponentField);
                } catch (IllegalAccessException e) {
                    throw new DependencyException(e);
                }
            } else {
                throw new DependencyException("Can't inject field '" + field.getType().getCanonicalName() + "' on '" + component.getClass().getCanonicalName() + "." + field.getName() + "' class. No instance found in container!");
            }
        }
    }

    /**
     * Call all {@link #components}'methods annotated with {@link PostConstruct}
     */
    public void invokeAllPostConstructComponentsMethod() {
        for (Object component : components.values()) {
            this.invokePostConstructMethods(component);
        }
    }

    /**
     * Call given component methods annotated with {@link PostConstruct}
     *
     * @param component
     */
    private void invokePostConstructMethods(Object component) {
        List<Method> postConstructMethods = ClassUtil.getCurrentAndInheritedAnnotatedMethods(component.getClass(), PostConstruct.class);
        for (Method method : postConstructMethods) {
            try {
                method.setAccessible(true);
                ClassUtil.invokeMethod(component, method, Collections.singletonList(components.values()));
            } catch (Exception e) {
                ConsoleLog.error(e);
            }
        }
    }

    /**
     * Fill all components properties when marked with {@link Value}
     */
    private void fillComponentsFieldsMarkedWithValue() {
        components.forEach((aClass, o) -> fillComponentFieldsMarkedWithValue(o));
    }


    private void fillComponentFieldsMarkedWithValue(Object component) {
        Class<?> aClass = component.getClass();
        List<Field> fields = ClassUtil.getCurrentAndInheritedFields(aClass);
        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            if (field.isAnnotationPresent(Value.class) && (field.getType().isPrimitive() || String.class.isAssignableFrom(fieldType) || Boolean.class.isAssignableFrom(fieldType) || Number.class.isAssignableFrom(fieldType))) {
                try {
                    field.setAccessible(true);
                    String propKey = field.getAnnotation(Value.class).value();
                    String defaultValue = field.getAnnotation(Value.class).defaultValue();
                    String val = applicationProperties.getProperty(propKey);
                    Object convertedVal = ClassUtil.valueOf(val, fieldType);
                    Object finalConvertedVal = convertedVal != null ? convertedVal : ClassUtil.valueOf(defaultValue, fieldType);
                    field.set(component, finalConvertedVal);
                } catch (Exception e) {
                    ConsoleLog.warn("Can't inject field " + field.getName() + " at " + aClass.getCanonicalName() + ", possibly wrong type or incorrect value");
                }
            }
        }
    }

    public List<Object> getRestControllers() {
        List<Object> controllers = new ArrayList<>();
        components.forEach((aClass, o) -> {
            if (ClassUtil.isClassAnnotatedWith(aClass, new Class[]{RestController.class})) {
                controllers.add(o);
            }
        });
        return controllers;
    }

    public List<Object> getControllersAdvice() {
        List<Object> controllers = new ArrayList<>();
        components.forEach((aClass, o) -> {
            if (ClassUtil.isClassAnnotatedWith(aClass, new Class[]{RestControllerAdvice.class})) {
                controllers.add(o);
            }
        });
        return controllers;
    }

    public Map.Entry<Class<?>, Object> getInterceptor() {
        for (Map.Entry<Class<?>, Object> componentEntrySet : components.entrySet()) {
            if (componentEntrySet.getKey().isAnnotationPresent(Interceptor.class) && ApixInterceptor.class.isAssignableFrom(componentEntrySet.getKey())) {
                return componentEntrySet;
            }
        }
        return null;
    }

    private void runAllConfigurationBeanCreation(Object configuration) {
        if (configuration.getClass().isAnnotationPresent(Configuration.class)) {
            List<Method> methods = ClassUtil.getCurrentAndInheritedAnnotatedMethods(configuration.getClass(), Bean.class);
            for (Method method : methods) {
                if (method.getReturnType() != void.class) {
                    Object bean = ClassUtil.invokeMethod(configuration, method, Collections.singletonList(components.values()));
                    if (bean != null && !components.containsKey(bean.getClass())) {
                        components.put(bean.getClass(), bean);
                    }
                }
            }
        }
    }

    /**
     * First look for the instance in {@link #components}, if none is found then we look for a child instance
     *
     * @param componentClass
     * @return
     */
    public Object getFirstAssignableComponent(Class<?> componentClass) {
        Object component = components.get(componentClass);
        if (component != null) {
            return component;
        } else {
            for (Class<?> storedComponentClass : components.keySet()) {
                if (componentClass.isAssignableFrom(storedComponentClass)) {
                    return components.get(storedComponentClass);
                }
            }
            return null;
        }
    }

    public List<Class<?>> getAssignableClasses(Class<?> componentClass){
        ArrayList<Class<?>> assignableClasses = new ArrayList<>();
        for (Class<?> storedComponentClass : components.keySet()) {
            if (componentClass.isAssignableFrom(storedComponentClass)) {
                assignableClasses.add(storedComponentClass);
            }
        }
        return assignableClasses;
    }

    public Properties getApplicationProperties() {
        return applicationProperties;
    }

    /**
     * Get all instantiated components
     *
     * @return unmodifiable map
     */
    public Map<Class<?>, Object> getComponents() {
        return Collections.unmodifiableMap(components);
    }

    public String getBasePackage(Class<?> mainClass) {
        if (mainClass.isAnnotationPresent(ComponentScan.class) && !mainClass.getAnnotation(ComponentScan.class).value().isEmpty()) {
            return mainClass.getAnnotation(ComponentScan.class).value();
        } else {
            return mainClass.getName().substring(0, mainClass.getName().lastIndexOf("."));
        }
    }

    public void addComponent(Class<?> aClass, Object component) {
        components.put(aClass, component);
    }
}

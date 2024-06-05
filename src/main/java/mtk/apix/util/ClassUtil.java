package mtk.apix.util;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import mtk.apix.annotation.PathParam;
import mtk.apix.annotation.PathVariable;
import mtk.apix.annotation.RequestBody;
import mtk.apix.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Utility class to help all operation about class
 *
 * @author mahatoky rasolonirina
 */
@SuppressWarnings("unchecked")
public final class ClassUtil {
    private ClassUtil() {
    }

    /**
     * Only for packaged application (JAR)
     * Find all classes annotate width the given annotation list
     *
     * @param mainClass   main class of application
     * @param packageName package to scan
     * @param annotations list of annotation
     * @return set of all class annotate with given list of annotation
     */
    public static Set<Class<?>> getJarAnnotatedClass(Class<?> mainClass, String packageName, Class<? extends Annotation>[] annotations) {
        Set<Class<?>> classes = new HashSet<>();
        try {
            File jarFile = new File(mainClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (!entry.isDirectory() && entry.getName().startsWith(packageName.replaceAll("[.]", "/")) && entry.getName().endsWith(".class")) {
                        try {
                            String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                            Class<?> clazz = mainClass.getClassLoader().loadClass(className);
                            for (Class<? extends Annotation> annotation : annotations) {
                                if (clazz.isAnnotationPresent(annotation)) {
                                    classes.add(clazz);
                                    break;
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            ConsoleLog.error(e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return classes;
    }


    /**
     * For non packaged application
     * Find all classes annotate width the given annotation list
     *
     * @param mainClass   main class of application
     * @param packageName package to scan
     * @param annotations list of annotation
     * @return all class annotate with given list of annotation
     */
    public static Set<Class<?>> getAnnotatedClass(Class<?> mainClass, String packageName, Class<? extends Annotation>[] annotations) {
        String classpath = System.getProperty("java.class.path");
        if (!classpath.contains(";") && classpath.endsWith(".jar")) {
            return getJarAnnotatedClass(mainClass, packageName, annotations);
        }
        Set<Class<?>> classes = new HashSet<>();
        ClassLoader classLoader = mainClass.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(packageName.replaceAll("[.]", "/"))) {
            if (stream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                reader.lines()
                        .forEach(line -> {
                                    if (line.endsWith(".class")) {
                                        Class<?> clazz = null;
                                        try {
                                            clazz = Class.forName(packageName + "." + line.substring(0, line.lastIndexOf(".class")));
                                            for (Class<? extends Annotation> annotation : annotations) {
                                                if (clazz.isAnnotationPresent(annotation)) {
                                                    classes.add(clazz);
                                                    break;
                                                }
                                            }
                                        } catch (ClassNotFoundException e) {
                                            ConsoleLog.error(e);
                                        }
                                    } else {
                                        classes.addAll(getAnnotatedClass(mainClass, packageName + "." + line, annotations));
                                    }
                                }
                        );
            } else {
                throw new Exception("-stream null for " + packageName);
            }
        } catch (Exception e) {
            ConsoleLog.warn(e.getMessage());
        }

        return classes;
    }

    /**
     * Test if class is annotated with any of given annotation list
     *
     * @param aClass      class to check
     * @param annotations list of annotation
     * @return result of check
     */
    public static boolean isClassAnnotatedWithAny(Class<?> aClass, Class<? extends Annotation>[] annotations) {
        for (Class<? extends Annotation> annotation : annotations) {
            if (aClass.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if given method is annotated with any of given annotation list
     *
     * @param method      method to check
     * @param annotations list of annotation
     * @return result of check
     */
    @SafeVarargs
    public static boolean isMethodAnnotatedWithAny(Method method, Class<? extends Annotation>... annotations) {
        for (Class<? extends Annotation> annotation : annotations) {
            if (method.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if one of the given parameters contains one which assignable from the given class
     *
     * @param parameters all parameters to check
     * @param aClass     a class to find
     * @return true or false according to the result of check
     */
    public static boolean contains(Parameter[] parameters, Class<?> aClass) {
        for (Parameter p : parameters) {
            if (RoutingContext.class.isAssignableFrom((Class<?>) p.getParameterizedType())) {
                return true;
            }
        }
        return false;
    }

    public static <T> T valueOf(String str, Class<T> toClass) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return valueOf(str, toClass, null);
    }

    /**
     * Convert given string to an over objet according to given class
     *
     * @param str                initial string value to convert
     * @param toClass            class of the target output value
     * @param defaultValueIfNull
     * @param <T>                type of output class
     * @return converted value of given string
     */
    public static <T> T valueOf(String str, Class<T> toClass, T defaultValueIfNull) {
        if (String.class.isAssignableFrom(toClass)) {
            return (T) str;
        }
        if (str == null || str.isEmpty()) {
            return defaultValueIfNull;
        }
        if (toClass.isPrimitive() || Number.class.isAssignableFrom(toClass) || Boolean.class.isAssignableFrom(toClass)) {
            if (toClass.isPrimitive()) {
                if (toClass.equals(int.class)) {
                    toClass = (Class<T>) Integer.class;
                } else if (toClass.equals(double.class)) {
                    toClass = (Class<T>) Double.class;
                } else if (toClass.equals(float.class)) {
                    toClass = (Class<T>) Float.class;
                } else if (toClass.equals(long.class)) {
                    toClass = (Class<T>) Long.class;
                } else if (toClass.equals(short.class)) {
                    toClass = (Class<T>) Short.class;
                } else if (toClass.equals(byte.class)) {
                    toClass = (Class<T>) Byte.class;
                } else if (toClass.equals(char.class)) {
                    toClass = (Class<T>) Character.class;
                } else if (toClass.equals(boolean.class)) {
                    toClass = (Class<T>) Boolean.class;
                } else {
                    throw new IllegalArgumentException("Primitive type not supporter for : " + toClass);
                }
            }

            try {
                Method valueOfMethod = toClass.getMethod("valueOf", String.class);
                return (T) valueOfMethod.invoke(null, str);
            } catch (Exception e) {
                return defaultValueIfNull;
            }
        } else {
            return defaultValueIfNull;
        }
    }

    /**
     * Take the given class annotate methods and all inherited annotate methods
     *
     * @param aClass
     * @param annotation
     * @return list of all methods marked with the given annotation
     */
    public static List<Method> getOwnAndInheritedAnnotatedMethods(Class<?> aClass, Class<? extends Annotation> annotation) {
        Method[] declaredMethods = aClass.getDeclaredMethods();
        List<Method> allMethods = Arrays.stream(declaredMethods).filter(method -> method.isAnnotationPresent(annotation)).collect(Collectors.toList());

        Class<?> superClass = aClass.getSuperclass();
        while (superClass != null) {
            Method[] inheritedMethods = superClass.getDeclaredMethods();
            allMethods.addAll(Arrays.stream(inheritedMethods).filter(method -> method.isAnnotationPresent(annotation)).collect(Collectors.toList()));
            superClass = superClass.getSuperclass();
        }

        return allMethods;
    }

    /**
     * take all methods marked with the given annotation
     *
     * @param aClass
     * @return list of all fields
     */
    public static List<Field> getOwnAndInheritedFields(Class<?> aClass) {
        Field[] declaredFields = aClass.getDeclaredFields();
        List<Field> allFields = new ArrayList<>(Arrays.asList(declaredFields));

        Class<?> superClass = aClass.getSuperclass();
        while (superClass != null) {
            Field[] inheritedFields = superClass.getDeclaredFields();
            allFields.addAll(Arrays.asList(inheritedFields));
            superClass = superClass.getSuperclass();
        }

        return allFields;
    }

    /**
     * Take the given class annotate fields and all inherited annotate fields
     *
     * @param aClass
     * @param annotation
     * @return list of all fields annotate with given annotation
     */
    public static List<Field> getOwnAndInheritedAnnotatedFields(Class<?> aClass, Class<? extends Annotation> annotation) {
        List<Field> allFields = new ArrayList<>();

        Field[] declaredFields = aClass.getDeclaredFields();
        Arrays.stream(declaredFields).filter(field -> field.isAnnotationPresent(annotation)).forEach(allFields::add);

        Class<?> superClass = aClass.getSuperclass();
        while (superClass != null) {
            Field[] inheritedFields = superClass.getDeclaredFields();
            Arrays.stream(inheritedFields).filter(field -> field.isAnnotationPresent(annotation)).forEach(allFields::add);
            superClass = superClass.getSuperclass();
        }

        return allFields;
    }

    /**
     * Specific invocation due to given list of object (dependencies) as parameters
     * The method will be invoked, and if one of the parameters is available in the list of dependencies then we use it, otherwise we set it to null
     *
     * @param instance     instance of the objet
     * @param method       target method
     * @param dependencies list of objects that can be used as parameters of the method to invoke
     * @return result of invocation
     */
    public static Object invokeMethod(Object instance, Method method, List<Object> dependencies) {
        try {
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = findInstance((Class<?>) parameters[i].getParameterizedType(), dependencies);
            }
            return method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A method can be invoked if and only if it has at least one parameter of type RoutingContext
     * The method will be invoked, and if one of the parameters is available in the list of dependencies then we use it, otherwise we set it to null
     *
     * @param instance       instance of the objet
     * @param method         target method
     * @param routingContext vertx RoutingContext
     * @param dependencies   list of objects that can be used as parameters of the method to invoke
     */
    public static void invokeHttpMethod(Object instance, Method method, RoutingContext routingContext, List<Object> dependencies) {
        try {
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < args.length; i++) {
                Parameter parameter = parameters[i];
                Class<?> paramClass = (Class<?>) parameter.getParameterizedType();
                if (parameter.isAnnotationPresent(PathParam.class)) {
                    String pathParam = parameter.getAnnotation(PathParam.class).value();
                    pathParam = (pathParam == null || pathParam.isEmpty()) ? parameter.getName() : pathParam;
                    args[i] = valueOf(routingContext.pathParam(pathParam), paramClass);
                } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                    String pathVar = parameter.getAnnotation(PathVariable.class).value();
                    pathVar = (pathVar == null || pathVar.isEmpty()) ? parameter.getName() : pathVar;
                    args[i] = valueOf(routingContext.pathParam(pathVar), paramClass);
                } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                    String requestParam = parameter.getAnnotation(RequestParam.class).value();
                    requestParam = (requestParam == null || requestParam.isEmpty()) ? parameter.getName() : requestParam;
                    args[i] = ClassUtil.valueOf(routingContext.request().getParam(requestParam), paramClass);
                } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                    args[i] = routingContext.body().asJsonObject().mapTo(paramClass);
                } else if (RoutingContext.class.isAssignableFrom(paramClass)) {
                    args[i] = routingContext;
                } else if (HttpServerResponse.class.isAssignableFrom(paramClass)) {
                    args[i] = routingContext.response();
                } else if (HttpServerRequest.class.isAssignableFrom(paramClass)) {
                    args[i] = routingContext.request();
                } else {
                    args[i] = findInstance(paramClass, dependencies);
                }
            }
            method.invoke(instance, args);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Find in the list of given instance an instance which class is assignable from given class
     *
     * @param aClass    class of the instance to find
     * @param instances list of instance
     * @return the instance
     */
    public static Object findInstance(Class<?> aClass, Object... instances) {
        for (Object instance : instances) {
            if (aClass.isAssignableFrom(instances.getClass())) {
                return instance;
            }
        }
        return null;
    }

    /**
     * Find the current application path.
     * If the application is packaged in JAR then it returns the folder of its location otherwise we consider the application as being the main folder to return
     * @param mainProjectClass main application class
     * @return the path
     * @throws URISyntaxException
     */
    public static String getCurrApplicationPath(Class<?> mainProjectClass) throws URISyntaxException {
        String jarPath = mainProjectClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        return jarPath.endsWith(".jar") ? new File(jarPath).getParent() : new File(jarPath).getPath();
    }
}

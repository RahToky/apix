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
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

import static io.vertx.ext.web.impl.Utils.getClassLoader;

/**
 * @author mahatoky rasolonirina
 */
@SuppressWarnings("unchecked")
public final class ClassUtil {
    private ClassUtil() {
    }

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

    public static boolean isClassAnnotatedWith(Class<?> aClass, Class<? extends Annotation>[] annotations) {
        for (Class<? extends Annotation> annotation : annotations) {
            if (aClass.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    public static boolean isMethodAnnotatedWith(Method method, Class<? extends Annotation>... annotations) {
        for (Class<? extends Annotation> annotation : annotations) {
            if (method.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

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

    public static <T> T valueOf(String str, Class<T> toClass, T defaultValueIfNull) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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

    public static List<Method> getCurrentAndInheritedAnnotatedMethods(Class<?> aClass, Class<? extends Annotation> annotation) {
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

    public static List<Field> getCurrentAndInheritedFields(Class<?> aClass) {
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

    public static List<Field> getCurrentAndInheritedAnnotatedFields(Class<?> aClass, Class<? extends Annotation> annotation) {
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

    public static Object findInstance(Class<?> aClass, Object... instances) {
        for (Object instance : instances) {
            if (aClass.isAssignableFrom(instances.getClass())) {
                return instance;
            }
        }
        return null;
    }

    public static String getCurrProjectPath(Class<?> mainProjectClass) throws URISyntaxException {
        String jarPath = mainProjectClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        return jarPath.endsWith(".jar") ? new File(jarPath).getParent() : new File(jarPath).getPath();
    }

    public static boolean isJar(Class<?> mainClass) throws URISyntaxException {
        return mainClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().endsWith(".jar");
    }
}

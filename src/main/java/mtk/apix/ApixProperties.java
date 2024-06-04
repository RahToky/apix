package mtk.apix;

import mtk.apix.util.ClassUtil;
import mtk.apix.util.ConsoleLog;
import mtk.apix.util.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author mahatoky rasolonirina
 */
class ApixProperties {
    private Properties applicationProperties = new Properties();
    public static final Map<Environment, String> APP_PROPS_FILENAMES;

    static {
        APP_PROPS_FILENAMES = new HashMap<>();
        APP_PROPS_FILENAMES.put(Environment.DEFAULT, "application.properties");
        APP_PROPS_FILENAMES.put(Environment.LOCAL, "application-local.properties");
        APP_PROPS_FILENAMES.put(Environment.DEV, "application-dev.properties");
        APP_PROPS_FILENAMES.put(Environment.PROD, "application-prod.properties");
    }

    public ApixProperties() {

    }

    public void init(Class<?> mainClass, Environment environment) {
        init(mainClass, environment, "");
    }

    public void init(Class<?> mainClass, Environment environment, String configDir) {
        try {
            Path path = null;
            if (configDir != null && !configDir.trim().isEmpty() && !"/".equals(configDir.trim()) && !"./".equals(configDir.trim())) {
                URI filename = new URI(configDir).normalize().resolve(APP_PROPS_FILENAMES.get(environment));
                path = Paths.get(filename.isAbsolute() ? filename : new URI(ClassUtil.getCurrProjectPath(mainClass)).resolve(filename));
                if (Files.exists(path)) {
                    try (InputStream input2 = Files.newInputStream(path)) {
                        applicationProperties.load(input2);
                        ConsoleLog.trace("Used properties: " + path);
                    } catch (Exception e) {
                        throw new Exception(path + "", e);
                    }
                } else {
                    throw new Exception(path + "not found");
                }
            } else {
                List<String> prioritizedPropsPaths = getPrioritizedPropertiesPaths(mainClass, environment);
                path = Paths.get(prioritizedPropsPaths.get(0)).normalize();
                if (Files.exists(path)) {
                    try (InputStream input2 = Files.newInputStream(path)) {
                        applicationProperties.load(input2);
                        ConsoleLog.trace("Used properties: " + path);
                    } catch (Exception e) {
                        throw new Exception(path + "", e);
                    }
                } else {
                    try (InputStream input = mainClass.getClassLoader().getResourceAsStream(prioritizedPropsPaths.get(1))) {
                        if (input == null)
                            return;
                        applicationProperties.load(input);
                        ConsoleLog.trace("Used properties: (from src) " + prioritizedPropsPaths.get(1));
                    } catch (Exception e) {
                        throw new Exception(prioritizedPropsPaths.get(1), e);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleLog.warn("Can't load properties cause: " + e.getMessage());
        }
    }

    /**
     * Give all properties paths by priority
     * 1 - outside of project
     * 2 - in project src
     *
     * @param environment
     * @return
     */
    public List<String> getPrioritizedPropertiesPaths(Class<?> mainClass, Environment environment) throws URISyntaxException {
        String jarPath = mainClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        List<String> paths = new ArrayList<>(2);
        if (jarPath.endsWith(".jar")) {
            paths.add(new File(jarPath).getParent() + "/" + APP_PROPS_FILENAMES.get(environment));
        } else {
            paths.add(new File(System.getProperty("user.dir")).getPath() + "/" + APP_PROPS_FILENAMES.get(environment));
        }
        paths.add(APP_PROPS_FILENAMES.get(environment));
        return paths;
    }

    public Properties getApplicationProperties() {
        return applicationProperties;
    }
}

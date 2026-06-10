import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

public class TestReflection2 {
    public static void main(String[] args) throws Exception {
        File f = new File("c:\\Users\\saiad\\.m2\\repository\\dev\\langchain4j\\langchain4j-core\\1.0.0-beta3\\langchain4j-core-1.0.0-beta3.jar");
        URLClassLoader loader = new URLClassLoader(new URL[]{f.toURI().toURL()});
        
        System.out.println("--- TokenUsage ---");
        Class<?> tokenUsageClass = loader.loadClass("dev.langchain4j.model.output.TokenUsage");
        for (Method m : tokenUsageClass.getDeclaredMethods()) {
            System.out.println(m.getName() + " " + java.util.Arrays.toString(m.getParameterTypes()));
        }
    }
}

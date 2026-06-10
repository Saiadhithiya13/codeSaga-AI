import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

public class TestReflection {
    public static void main(String[] args) throws Exception {
        File f = new File("c:\\Users\\saiad\\.m2\\repository\\dev\\langchain4j\\langchain4j-core\\1.0.0-beta3\\langchain4j-core-1.0.0-beta3.jar");
        URLClassLoader loader = new URLClassLoader(new URL[]{f.toURI().toURL()});
        
        System.out.println("--- StreamingChatResponseHandler ---");
        Class<?> handlerClass = loader.loadClass("dev.langchain4j.model.chat.response.StreamingChatResponseHandler");
        for (Method m : handlerClass.getDeclaredMethods()) {
            System.out.println(m.getName() + " " + java.util.Arrays.toString(m.getParameterTypes()));
        }
        
        System.out.println("--- ChatResponse ---");
        Class<?> responseClass = loader.loadClass("dev.langchain4j.model.chat.response.ChatResponse");
        for (Method m : responseClass.getDeclaredMethods()) {
            System.out.println(m.getName() + " " + java.util.Arrays.toString(m.getParameterTypes()));
        }
    }
}

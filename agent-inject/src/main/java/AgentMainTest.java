import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class AgentMainTest {
    public static void agentmain(String agentArgs, Instrumentation inst)
    {
        inst.addTransformer(new MyTransformer(),true);
        for (Class<?> clazz : inst.getAllLoadedClasses()){
            if (clazz.getName().equals("org.apache.catalina.core.ApplicationFilterChain")){
                try {
                    inst.retransformClasses(clazz);
                } catch (UnmodifiableClassException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

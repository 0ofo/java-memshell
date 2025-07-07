import com.sun.tools.attach.VirtualMachine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.ProtectionDomain;

public class test {
    public static void main(String[] args) throws Exception{
        String pid = getPID("org.apache.catalina.startup.Bootstrap");
        if (pid==null){
            throw new RuntimeException("未找到目标进程");
        }
        String jar = getJar(test.class);
        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent(jar);
    }
    // 执行jps -l，并获取类名对应的进程号
    public static String getPID(String className){
        try {
            Process process = Runtime.getRuntime().exec("jps -l");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(className)){
                    return line.split(" ")[0];
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    public static String getJar(Class<?> clazz){
        // 获取类所在的JAR包路径
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        // 提取JAR包名称
        String path = location.getPath();
        if (System.getProperty("os.name").toLowerCase().contains("win")&&path.startsWith("/"))
            path = path.substring(1);
        return path;
    }
}

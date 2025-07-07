import javassist.*;

import java.util.Base64;

public class DumpBase64 {
    public static void main(String[] args) throws Exception{
        ClassPool pool = ClassPool.getDefault();
        // 从类路径获取CtClass对象
        CtClass ctClass = pool.get("Inject");

        // 转换为字节数组
        byte[] classBytes = ctClass.toBytecode();

        // 使用BASE64Encoder进行Base64编码
        String code = Base64.getEncoder().encodeToString(classBytes);
        System.out.println(code);
    }
    class EvilFilter{

    }
}

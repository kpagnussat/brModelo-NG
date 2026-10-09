package util;

import java.io.*;
import java.lang.reflect.*;

/** An allowed-package payload records whether any deserialization callback ran. */
public final class ReadWitness implements Serializable, InvocationHandler, java.util.Comparator<String> {
    private static final long serialVersionUID = 1L;
    public static int reads;
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        reads++;
        in.defaultReadObject();
    }
    @Override public Object invoke(Object proxy, Method method, Object[] args) { return null; }
    @Override public int compare(String a, String b) { return a.compareTo(b); }
}

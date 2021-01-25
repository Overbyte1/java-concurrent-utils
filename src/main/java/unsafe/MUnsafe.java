package unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class MUnsafe {
    private static final Unsafe unsafe;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }
    public static Unsafe getUnsafe() {
        return unsafe;
    }
}

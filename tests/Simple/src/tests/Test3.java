package tests;

import com.sun.squawk.*;

/**
 * Tests peek/poke mechanism from non-image class.
 */
public class Test3 {

    static void peekIntArray(Address raw, int offset, int length) {
        int end = offset + length;
        for (int i = offset; i != end; ++i) {
            System.out.println("Unsafe.getInt(raw, " + i + ") = " + Unsafe.getInt(raw, i));
        }
    }

    public static void main(String[] args) {
        int[] arr = new int[2];
        Address raw = Address.fromObject(arr);

        peekIntArray(raw, 0, 2);

        arr[0] = 555;
        Unsafe.setInt(raw, 1, 666);
        peekIntArray(raw, 0, 2);

        // Unpredictable values after GC as 'arr' may have been moved but 'raw'
        // will not have been updated (GC doesn't update Address variables).
        System.gc();
        peekIntArray(raw, 0, 2);

        try {
            Class.forName("tests.Test3_error");
            System.out.println("expected a LinkageError");
        } catch (ClassNotFoundException ex) {
            System.out.println("expected a LinkageError");
        } catch (Error le) {
            System.out.println("got expected LinkageError: " + le);
        }
    }

}

class Test3_error {
    static void f() {
        // Access a public native method that is not allowed
//        VM.isBigEndian();
    }
}

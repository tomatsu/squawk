//if[!FLASH_MEMORY]
/*
 * Copyright 2005-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package tests;

class Base {
    static int x = 0;
    static int y = 0;
    static int z = 0;
}

class Instance {
       int x = 0;
     int y = 0;
     int z = 0;  
     
     Instance() {
         x = 1;
         y = 2;
         z = 3;
     }
     
     public String toString() {
         return "<x: " + x + ", y: " + y + ", z: " + z + ">";
     }
}

public class TestApp extends Base {
    // primitives
    static byte b = 5;
    static short s = 2;
    static long l = -1;

    // objects:
    static String str = "ABC";
    // objects:
    static Object obj = new Object();
    int[] intarray = {1, 2, 3};
    static String[] strarray = {"foo", "bar", "baz"};

    // primitive objects
    static Boolean booleanObj = new Boolean(true);
    static Byte byteObj = new Byte((byte)111);
    static Short shortObj = new Short((short)2222);
    static Character charObj = new Character('Z');
    static Integer intObj = new Integer(4444);
    static Long longObj = new Long(8888);

    static Class thisClass = TestApp.class;
    static Thread thisThread = Thread.currentThread();
    
    static Object base = new Base();

    static void zorch1() {
        // zorchs should never appear on stack trace
    }

    static void f1(int i) {
        if (!(intObj instanceof Integer)) {
            throw new RuntimeException("inobj = " + intObj);
        }
        f2(i + 1);
    }

    static void zorch2() {
    }

    static void f2(int i) {
        x = 2; // create a different offset for each call.
        f3(i + 1);
    }

    static void zorch3() {
    }

    static void f3(int i) {
        x = 2;
        y = 2;
        f4(i + 1);
    }

    static void zorch4() {
    }

    static void f4(int i) {
        x = 2;
        y = 2;
        z = 2;
        f5(i + 1);
    }

    static void zorch5() {
    }

    static void f5(int i) {
        x += y;
        y += z;
        z += x+y;
        f6(i + 1);
        i = x;
        throw new RuntimeException("This is the expected exception in the TestApp.");
    }

    static void zorch6() {
    }

    static void f6(int i) {
        for (int j = 0; j < 2; j++) {
            int k = j;
        }

        int k = 2;
        while (k > 0) {
            k--;
        }

        int m = 2;
        do {
            m--;
        } while(m > 0);

        f7(i + 1);
    }

    static void f7(int i) {
        int j = 1;
        switch (j) {
            case 1:
                i = 1;
            case 2:
                i = 2;
                break;
            case 3:
                i = 3;
                break;

        }
        j = 4;

        f8(i + 1);
    }

    static void f8(int i) {
        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 2; k++) {
                i = i + 1;
            }
        }

        int j = 2;
        while (j > 0) {
            int k = 2;
            while (k > 0) {
                k--;
            }
            j--;
        }

        j = 2;
        while (j > 0) {
            for (int k = 0; k < 2; k++) {
                i = i + 1;
            }
            j--;
        }

        for (int l = 0; l < 2; l++) {
            int k = 2;
            while (k > 0) {
                k--;
            }
        }

        for (int l = 0; l < 2; l++) {
            int m = 2;
            do {
                m--;
            } while(m > 0);
        }

        f9(f9a(), f9b(), f9(f9a(), f9b(), 12));
        for (int p = 0; p < 2; p++) { }

    }

    static int f9(int i, int j, int k) {
        int l = i + j + k;
        return l;
    }

    static int f9a() {
        return 10;
    }

    static int f9b() {
        return 11;
    }

    static void mainLoop(boolean runExceptionThread) {
        int count = 0;
        System.err.println("Entering test loop...");

        while (true) {
            System.err.println("Loop iteration: " + count);
            try {
                f1(1);
            } catch (Exception e) {
                System.err.println("caught exception.");
            } finally {
                System.err.println("finally.");
            }
            count++;

            if (count % 5 == 0) {
                System.gc();

                if (runExceptionThread) {
                    new ExceptionThread().start();

                    System.out.println("for execution point in static initializer");
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
    }
    
    static void valCheck(long v) {
        System.out.println("v is " + v);
    }

    static void valCheck(Object v) {
        System.out.println("v is " + v);
    }
    
    static void testVals() {
        System.out.println("Check static fields");
        valCheck(b);
        valCheck(b);
        valCheck(b);
        
        valCheck(s);
        valCheck(s);
        valCheck(s);
        
        valCheck(l);
        valCheck(l);
        valCheck(l);
        
        valCheck(str);
        valCheck(str);
        valCheck(str);
        
        valCheck(obj);
        valCheck(obj);
        valCheck(obj);
        
        valCheck(strarray[1]);
        valCheck(strarray[1]);
        valCheck(strarray[1]);
        
        System.out.println("Check instance fields");
        Object foo = new Instance();
        System.out.println("foo: " + foo);
        System.out.println("foo: " + foo);
        System.out.println("foo: " + foo);
        
        System.out.println("booleanObj: " + booleanObj);
        System.out.println("booleanObj: " + booleanObj);
        System.out.println("booleanObj: " + booleanObj);
        
        System.out.println("byteObj: " + byteObj);
        System.out.println("byteObj: " + byteObj);
        System.out.println("byteObj: " + byteObj);
        
        System.out.println("shortObj: " + shortObj);
        System.out.println("shortObj: " + shortObj);
        System.out.println("shortObj: " + shortObj);
        
        System.out.println("charObj: " + charObj);
        System.out.println("charObj: " + charObj);
        System.out.println("charObj: " + charObj);
        
        System.out.println("intObj: " + intObj);
        System.out.println("intObj: " + intObj);
        System.out.println("intObj: " + intObj);
        
        System.out.println("longObj: " + longObj);
        System.out.println("longObj: " + longObj);
        System.out.println("longObj: " + longObj);
    }
    
    static class ExceptionThread extends Thread {
        static int counter;
        public void run() {
            int j = StaticInitializer.value;
            for (int i = 0; i != 10; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                }
                System.err.println("spin " + counter + ": " + i);
            }
            counter++;
            throw new RuntimeException("uncaught exception");
        }
    }
    
    static void testMT() {
        System.out.println("Hello World");
        Runnable r=new Runnable() {
            public void run() {
                while (true) {
                    Thread.yield();
                }
            }
        };
        Runnable r1=new Runnable() {
            public void run() {
                while (true) {
                    Thread.yield();
                }
            }
        };
        (new Thread(r)).start();
        (new Thread(r1)).start();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
    }
//    public void run() {
//        mainLoop();
//    }

    public static void main(String[] args) {
        testVals();
        testMT();
        mainLoop(args.length == 0);
    }
} // TestIsolate

class StaticInitializer {
    static int showInitializer() {
        System.out.println("in StaticInitializer.<clinit>");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {

        }
        return 5;
    }
    static int value = showInitializer();
}

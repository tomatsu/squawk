package tests;

import java.io.*;
import javax.microedition.io.*;
import com.sun.squawk.*;

public class Test0 {

    static int writingThreads = 0;

    public static void main (String[] args) {
        System.out.println("Dynamically loaded Hello World 0!");
/*if[FLOATS]*/
        float f = 4321.5678F;
        double d = 6789.4321D;
/*end[FLOATS]*/

        // Start some threads
        for (int i = 0; i != 3; ++i) {
            final int id = i;
            new Thread() {
                  public void run() {
//                      bench.cubes.Main.main(null);
                    try {
                        DataOutputStream os = Connector.openDataOutputStream("file://" + id + ".data");
                        writingThreads++;
                        for (int j = 0; j != 10000; ++j) {
                            os.writeInt(j);
                            os.writeUTF(" ");
                        }
                        os.close();
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();
                    }
                  }
            }.start();
        }

        while (writingThreads != 3) {
            System.out.println("writing threads: " + writingThreads);
            Thread.yield();
        }

        int wakeCount = 0;
        while (Thread.activeCount() > 1) {

            try {
                System.out.println("Hibernating " + Isolate.currentIsolate());
                Isolate.currentIsolate().hibernate();
                System.out.println("Reawoke " + Isolate.currentIsolate() + ": " + (++wakeCount));
                if (wakeCount > 10) {
                    break;
                }
            }
            catch (IOException ex) {
                System.err.println("Error hibernating isolate: " + ex);
//                ex.printStackTrace();
            }
        }
    }
}

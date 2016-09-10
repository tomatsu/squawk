package com.sun.squawk.hal;

public class AnalogIn {
   private static native int init(int pin);
   private static native int read0(int d);
   private static native float read1(int d);
   
   private int d;
   
   public AnalogIn(int pin) {
     d = init(pin);
   }

   public int read() {
     return read0(d);
   }

	public float readFloat() {
     return read1(d);
   }
}

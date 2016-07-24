import java.util.*;
public class Hello {
    /*
	static int fib(int n){
		if (n < 2) return n;
		return fib(n-1)+fib(n-2);
	}
    */

	static void test() throws InterruptedException {
		System.out.println("hello");
		/*
		for (int i = 0; i < 10; i++) {
		    System.out.println(new Date().getTime());
		    Thread.sleep(300);
		}
		*/
		throw new RuntimeException("message");
	}
    public static void main(String[] args)  {
//		System.out.println(fib(40));
	    try {
		    test();
	    } catch (Exception e){
		    e.printStackTrace();
	    }
    }
}

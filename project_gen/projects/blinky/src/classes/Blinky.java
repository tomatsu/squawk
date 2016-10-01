import com.sun.squawk.hal.*;

public class Blinky implements PinNames {
	static DigitalOut out = new DigitalOut(LED1);

	public static void main(String[] args) throws Exception {
		while (true) {
			out.write(1);
			Thread.sleep(400);
			out.write(0);
			Thread.sleep(400);
		}
	}
}

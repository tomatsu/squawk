import com.sun.squawk.hal.*;

public class Blinky {
	private static final int BUTTON_A = 17;
	private static final int BUTTON_B = 26;
	static DigitalOut out = new DigitalOut(13);
	static DigitalOut col = new DigitalOut(4);

	static void setupButtons() {
		GpioIRQ.init(BUTTON_A, true);
		GpioIRQ.init(BUTTON_B, true);
		GpioIRQ.startIRQ(new GpioIRQHandler(){
				public void signal(int pin, boolean fall) {
					System.out.println("pin="+pin + ", " + (fall ? "fall" : "rise"));
				}
			});
	}
	
	public static void main(String[] args) throws Exception {
		setupButtons();
		
		col.write(0);
		while (true) {
			out.write(1);
			Thread.sleep(400);
			out.write(0);
			Thread.sleep(400);
		}
	}
}

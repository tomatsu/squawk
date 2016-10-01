public class Hello {
	private static native void sayHello();
	private static native void passString(String arg);
	private static native void receive(Struct s);
	
	public static void main(String[] args) {
		sayHello();
		passString("Hello");
		
		Struct s = new Struct();
		System.out.println("input: " + s);
		receive(s);
		System.out.println("output: " + s);
	}

	static class Struct {
		int integer;
		byte singleByte;
		byte[] bytearray;
		
		Struct() {
			this.integer = 1234;
			this.singleByte = 1;
			this.bytearray = new byte[]{0x31, 0x32, 0x33, 0x34};
		}
		
		public String toString() {
			return "Struct [" + integer + ", " + singleByte + ", " + new String(bytearray) + "]";
		}
	}
}

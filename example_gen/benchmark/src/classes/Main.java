public class Main {
	public static void main(String[] args) {
		long end = System.currentTimeMillis() + 10000;
		int count = 0;
		while (System.currentTimeMillis() < end) {
			count++;
		}
		System.out.println(count);
	}
}

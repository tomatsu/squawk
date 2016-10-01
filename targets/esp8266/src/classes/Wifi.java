package esp8266;

public class Wifi {
	public static native byte get_opmode();
	public static native byte get_opmode_default();
	public static native boolean set_opmode(byte mode);
	public static native boolean set_opmode_current(byte mode);
	public static native boolean station_get_config(StationConfig c);
	public static native boolean station_get_config_default(StationConfig c);
	public static native boolean station_set_config(StationConfig c);
	public static native boolean station_set_config_current(StationConfig c);
	public static native int station_set_cert_key(byte[] client_cert, int client_cert_len, byte[] private_key, int private_key_len, String private_key_passwd);
	public static native void station_clear_cert_key();
	public static native int station_set_username(String name);
	public static native void station_clear_username();
	public static native boolean station_connect();
	public static native boolean station_disconnect();
	public static native byte station_get_connect_status();
	public static native void station_scan(ScanConfig c);
	public static native boolean station_ap_number_set(byte ap_number);
	public static native byte station_get_ap_info(StationConfig[] c);
	public static native byte station_ap_change(byte ap_id);

	public static native boolean get_ip_info(byte index, IPInfo info);


	public static class StationConfig {
		public byte ssid[];
		public byte passwd[];
		public byte bssid_set;
		public byte bssid[];
	}
	
	public static class ScanConfig {
		public byte[] ssid;
		public byte[] bssid;
		public byte channel;
		public byte show_hidden;
	}

	public static class IPInfo {
		public int ip;
		public int netmask;
		public int gw;
	}
	
}

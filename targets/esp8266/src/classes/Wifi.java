package esp8266;

import com.sun.squawk.*;

public class Wifi {
	public final static int WIFI_EVENT = 1;
	public final static int NULL_MODE = 0;
	public final static int STATION_MODE = 0x01;
	public final static int SOFTAP_MODE = 0x02;
	public final static int STATIONAP_MODE = 0x03;
	
    public final static int EVENT_STAMODE_CONNECTED = 0;
    public final static int EVENT_STAMODE_GOT_IP = 3;
    public final static int EVENT_SOFTAPMODE_STACONNECTED = 5;
	
	public static native byte get_opmode();
	public static native byte get_opmode_default();
	public static native boolean set_opmode(byte mode);
	public static native boolean set_opmode_current(byte mode);
	public static native boolean station_get_config(StationConfig c);
	public static native boolean station_get_config_default(StationConfig c);
	public static native boolean station_set_config(StationConfig c);
	public static native boolean station_set_config_current(StationConfig c);
	public static native int station_set_cert_key(byte[] client_cert, int client_cert_len,
												  byte[] private_key, int private_key_len, String private_key_passwd);
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
	public static native byte station_get_current_ap_id();
	public static native byte station_get_auto_connect();
	public static native boolean station_set_auto_connect(byte b);
	public static native boolean station_dhcpc_start();
	public static native boolean station_dhcpc_stop();
	public static native int station_dhcpc_status();
	public static native boolean station_dhcpc_set_maxtry(int max);
	public static native boolean station_set_reconnect_policy(boolean b);
	public static native int station_get_rssi();
	public static native boolean station_set_hostname(String name);
	public static native int station_get_hostname(byte[] name);
	public static native boolean softap_get_config(SoftApConfig c);
	public static native boolean softap_get_config_default(SoftApConfig c);
	public static native boolean softap_set_config(SoftApConfig c);
	public static native boolean softap_set_config_current(SoftApConfig c);
	public static native byte softap_get_station_num();
	public static native void softap_get_station_info(StationInfo info);
	public static native boolean softap_dhcps_start();
	public static native boolean softap_dhcps_stop();
	public static native boolean softap_set_dhcps_lease(DHCPLease lease);
	public static native boolean softap_get_dhcps_lease(DHCPLease lease);
	public static native boolean softap_set_dhcps_lease_time(int minute);
	public static native int softap_get_dhcps_lease_time();
	public static native int softap_dhcps_status();
	public static native boolean softap_set_dhcps_offer_option(byte level, int optarg);
	public static native boolean set_phy_mode(int mode);
	public static native int get_phy_mode();
	public static native boolean get_ip_info(int index, IPInfo info);
	public static native boolean set_ip_info(int if_index, IPInfo info);
	public static native boolean set_macaddr(int if_index, byte[] macaddr);
	public static native boolean get_macaddr(int if_index, byte[] macaddr);
	public static native boolean set_sleep_type(int type);
	public static native int get_sleep_type();
	public static native void status_led_install (int gpio_id, int gpio_name, int gpio_func);
	public static native void status_led_uninstall();
	public static native boolean set_broadcast_if(int intrf);
	public static native int get_broadcast_if();
	public static native boolean wps_enable(int wps_type);
	public static native boolean wps_disable();
	public static native boolean wps_start();
	public static native int send_pkt_freedom(byte[] buf, int len, boolean sys_seq);
	public static native int rfid_locp_recv_open();
	public static native void rfid_locp_recv_close();
	public static native void enable_gpio_wakeup(int i, int intr_status);
	public static native void disable_gpio_wakeup();
	static native void init_event_handler();
	static native int wifi_get_event_id(int addr);
	static native void receive_event(int addr, Event evt);
	
	static {
		init_event_handler();
		start();
	}
	
	public static class StationConfig {
		public byte ssid[];
		public byte passwd[];
		public byte bssid_set;
		public byte bssid[];
		
		public StationConfig() {
			ssid = new byte[32];
			passwd = new byte[64];
			bssid_set = 0;
			bssid = new byte[6];
		}
		
		public StationConfig(String ssid, String passwd, boolean bssid_set, String bssid) {
			this();
			byte[] b;
			int n;

			b = ssid.getBytes();
			n = b.length;
			if (n > 32) n = 32;
			System.arraycopy(b, 0, this.ssid, 0, n);

			b = passwd.getBytes();
			n = b.length;
			if (n > 64) n = 64;
			System.arraycopy(b, 0, this.passwd, 0, n);

			b = bssid.getBytes();
			n = b.length;
			if (n > 6) n = 6;
			System.arraycopy(b, 0, this.bssid, 0, n);
			this.bssid_set = bssid_set ? (byte)1 : (byte)0;
		}
	}

	public static class SoftApConfig {
		public byte[] ssid;
		public byte[] password;
		public int ssid_len;
		public int channel;
		public int authmode;
		public int ssid_hidden;
		public int max_connection;
		public int beacon_interval;
	}

	public static class StationInfo {
		public byte[] bssid;
		public int ip;
	}

	public static class DHCPLease {
		public boolean enable;
		public int start_ip;
		public int end_ip;
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

	public static interface Event {	}

	public static class StaMode_Connected_Event implements Event {
		public byte ssid[];
		public byte ssid_len;
		public byte bssid[];
		public byte channel;

		public StaMode_Connected_Event() {
			ssid = new byte[32];
			bssid = new byte[6];
		}
	}
	
	public static class StaMode_Got_IP_Event implements Event {
		public int ip;
		public int mask;
		public int gw;
	}
	
	public static class SoftAPMode_StaConnected_Event implements Event {
		public byte[] mac;
		public byte aid;
		
		public SoftAPMode_StaConnected_Event() {
			mac = new byte[6];
		}
	}
	
	static interface EventHandler {
		void handle(Event event);
	}

	static EventHandler eventHandler;
	
	public static void setEventHandler(EventHandler handler) {
		eventHandler = handler;
	}

	static void dispatchEvent(int addr) {
		int event_id = wifi_get_event_id(addr);
		Event evt = null;
		if (event_id == EVENT_STAMODE_CONNECTED) {
			evt = new StaMode_Connected_Event();
		} else if (event_id == EVENT_STAMODE_GOT_IP) {
			evt = new StaMode_Got_IP_Event();
		} else if (event_id == EVENT_SOFTAPMODE_STACONNECTED) {
			evt = new SoftAPMode_StaConnected_Event();
		}
		if (evt != null) {
			receive_event(addr, evt);
			if (eventHandler != null) {
				eventHandler.handle(evt);
			}
		}
	}

	static void start() {
		Thread th = new Thread(){
				public void run() {
					VMThread th = VMThread.currentThread();
					try {
						while (true) {
							VM.waitForInterrupt(WIFI_EVENT);
							dispatchEvent(th.event);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
		th.start();
	}
}

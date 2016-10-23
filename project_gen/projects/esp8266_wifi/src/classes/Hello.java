import esp8266.*;
import static esp8266.Wifi.*;
import javax.microedition.io.*;
import java.io.*;

public class Hello {
	static Class[] classes = {com.sun.squawk.io.j2me.socket.Protocol.class,
							  com.sun.squawk.io.j2me.serversocket.Protocol.class,
							  com.sun.squawk.io.j2me.http.Protocol.class,
	};
	
	public static void main(String[] args) throws Exception {
		
		System.out.println("get_opmode() " + Wifi.get_opmode());
		System.out.println("get_config() " + Wifi.Station.get_config());
		System.out.println("get_config_default() " + Wifi.Station.get_config(true));
		System.out.println("get_connect_status() " + Wifi.station_get_connect_status());
		StationConfig[] config = new StationConfig[5];
		for (int i = 0; i < config.length; i++) {
			config[i] = new StationConfig();
		}
		int n = Wifi.station_get_ap_info(config);
		System.out.println("get_ap_info() n="+ n);
		for (int i = 0; i < n; i++) {
			System.out.println(config[i]);
		}

		System.out.println("get_current_ap_id() " + Wifi.station_get_current_ap_id());
		System.out.println("get_auto_connect() " + Wifi.station_get_auto_connect());
		System.out.println("dhcpc_status() " + Wifi.station_dhcpc_status());
		System.out.println("get_rssi() " + Wifi.station_get_rssi());
		byte[] buf = new byte[32];
		n = Wifi.station_get_hostname(buf);
		System.out.println("get_hostname() " + new String(buf, 0, n));
		System.out.println("get_phy_mode() " + Wifi.get_phy_mode());
		IPInfo info = new IPInfo();
		if (Wifi.get_ip_info(0, info)) {
			System.out.println("get_ip_info STA "  + info);
		} else {
			System.out.println("get_ip_info STA failed");
		}
		byte[] addr = new byte[6];
		if (Wifi.get_macaddr(0, addr)) {
			System.out.println("get_macaddr() " + format_bytearray(addr));
		}
		System.out.println("get_sleep_type() " + Wifi.get_sleep_type());
		System.out.println("get_broadcast_if() " + Wifi.get_broadcast_if());

		/*
		System.out.println("scan() ");
		for (Wifi.BSS_Info bss : Wifi.Station.scan()) {
			System.out.println(bss);
		}

		Wifi.Station.set_config("ssid", "password", null);
		Wifi.Station.connect();
		*/
		System.out.println(Wifi.Station.getIP());

		/*
		{
			SocketConnection c = (SocketConnection)Connector.open("socket://192.168.1.7:9000");
			System.out.println("local address = " + c.getLocalAddress());
			System.out.println("local port = " + c.getLocalPort());
			System.out.println("address = " + c.getAddress());
			System.out.println("port = " + c.getPort());
			InputStream in = c.openInputStream();
			OutputStream out = c.openOutputStream();
			out.write(12);
			System.out.println(in.read());
		}
		*/
		/*
		{
			HttpConnection c = (HttpConnection)Connector.open("http://192.168.1.7/");
			InputStream in = c.openInputStream();
			int nn;
			byte[] b = new byte[512];
			while ((nn = in.read(b)) != -1) {
				System.out.write(b, 0, nn);
			}
		}
		*/
		{
			ServerSocketConnection c = (ServerSocketConnection)Connector.open("socket://:9000");
			SocketConnection sc = (SocketConnection)c.acceptAndOpen();
			InputStream in = sc.openInputStream();
			int nn;
			byte[] b = new byte[512];
			while ((nn = in.read(b)) != -1) {
				System.out.write(b, 0, nn);
			}
		}
		while (true) {
			System.out.println("Hello ");
			Thread.sleep(1000);
		}
	}
	
	static String format_bytearray(byte[] array) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		int len = array.length;
		if (len > 0) {
			sb.append(Integer.toHexString(array[0] & 0xff));
		}
		for (int i = 1; i < len; i++) {
			sb.append(':');
			sb.append(Integer.toHexString(array[i] & 0xff));
		}
		sb.append(']');
		return sb.toString();
	}
}

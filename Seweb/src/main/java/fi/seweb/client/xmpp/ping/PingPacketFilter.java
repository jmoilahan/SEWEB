package fi.seweb.client.xmpp.ping;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ping.packet.Ping;

import android.util.Log;

public class PingPacketFilter implements PacketFilter {
	private static final String TAG = "PacketFilter";
	@Override
	public boolean accept(Packet packet) {
		if (packet instanceof Ping) {
			Log.i(TAG, "Ping request received");
			return true;
		}
		return false;
	}
}

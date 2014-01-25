package fi.seweb.client.xmpp.ping;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;



import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.packet.Pong;

import android.util.Log;


public class PingPacketListener implements PacketListener {
	
	private static final String TAG = "PingPacketListener";
	private XMPPConnection mConnection;
	
	public PingPacketListener(XMPPConnection connection) {
		this.mConnection = connection;
	}
	
	@Override
	public void processPacket(Packet packet) {
		if (packet == null) {
			Log.e(TAG, "Ping packet == null");
			return;
		}
		
		if (packet instanceof Ping) {
			Ping ping = (Ping) packet;
			Pong pong = new Pong(ping);
			Log.i(TAG, "XMPP Pong packet");
			Log.i(TAG, pong.toXML());
			mConnection.sendPacket(pong);
		}
	}
}

package fi.seweb.client.xmpp.ping;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.xmlpull.v1.XmlPullParser;

public class PingProvider implements IQProvider {

	@Override
	public IQ parseIQ(XmlPullParser arg0) throws Exception {
		return new Ping();
	}
}

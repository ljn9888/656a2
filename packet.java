// common packet class used by both SENDER and RECEIVER

import java.nio.ByteBuffer;

public class packet  {

	private final int MAX_LENGTH = 500;
	private final int SEQ_NUMBER_MODULO = 32;

	private int length;
	private int type;
	private int seqnumber;
	private String data;
	public int getLength() {
		return length;
	}
	public int getType() {
		return type;
	}
	public int getSeqNumber() {
		return seqnumber;
	}
	public String getString() {
		return data;
	}

	packet(int type, int seqnumber, String data) throws Exception {
		this.type = type;
		this.seqnumber = seqnumber % SEQ_NUMBER_MODULO;
		this.data = data;
		this.length = data.length();
		if (length > MAX_LENGTH)
			throw new Exception("data too large (max 500 chars)");	////no more than 500

	}
	
	// construct packet in following way
	public static packet ACK(int seqnumber) throws Exception {
		return new packet(0, seqnumber, new String());
	}
	
	public static packet Packet(int seqnumber, String data) throws Exception {
		return new packet(1, seqnumber, data);
	}
	
	public static packet EOT(int seqnumber) throws Exception {
		return new packet(2, seqnumber, new String());
	}
	
	//////////////////////////// UDP HELPERS ///////////////////////////////////////
	
	public byte[] getUDPdata() {
		ByteBuffer buffer = ByteBuffer.allocate(512);
		buffer.putInt(type);
        buffer.putInt(seqnumber);
        buffer.putInt(data.length());
        buffer.put(data.getBytes(),0,data.length());
		return buffer.array();
	}
	
	public static packet parseUDPdata(byte[] UDPdata) throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
		int type = buffer.getInt();
		int seqnumber = buffer.getInt();
		int length = buffer.getInt();
		byte data[] = new byte[length];
		buffer.get(data, 0, length);
		return new packet(type, seqnumber, new String(data));
	}
}

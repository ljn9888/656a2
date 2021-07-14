// common packet class used by both SENDER and RECEIVER

import java.net.*;

class ACKReceiver extends Thread  {
	public void run() {
		try {
			byte[] receive = new byte[512];
			// Create socket to listen for ACKs from emulator at receive_port
			DatagramSocket socket0 = new DatagramSocket(sender.receiveport);
			DatagramPacket receiveAck = new DatagramPacket(receive, receive.length);
			while (true) {
				socket0.receive(receiveAck);
				// parse buffer into packet
				packet packet0 = packet.parseUDPdata(receive);
				int seqnumber = packet0.getSeqNumber();
				//////////////////Exit when find the eot///////////////
				if (packet0.getType() == 2) {
					sender.seqnumlog.close();
					sender.acklog.close();
					packet eot = packet.EOT(seqnumber+1);
					sender.udp_send(eot);
					sender.timer.cancel();
					sender.timer.purge();
					return;
				}
				// input ack sequence into ack.log
				sender.acklog.write("t=" + sender.timestamp++ + " " + seqnumber + "\n");
				sender.acklog.flush();
				// if seqNum overflows, adjust sender base accordingly
				sender.baseseqnumber = 32*((sender.nextSeqNum-seqnumber)/32) + (seqnumber+1)%32;
				System.out.print("Ack: " + sender.baseseqnumber);
				// Send EOT if all acks received
				if (seqnumber == (sender.send_string_all.size()-1)%32 && sender.nextSeqNum == sender.send_string_all.size()) {
					packet eot = packet.EOT(seqnumber+1);
					sender.udp_send(eot);
					sender.timer.cancel();
					sender.timer.purge();
					return;
				}
				// stop timer if window empty
				if (sender.baseseqnumber != sender.send_string_all.size()) {
					sender.timer.schedule(new sender.Timeout(), 100);
				}
			}

		} catch (Exception e) {
			System.out.println(e);
		}
	}
}

// common packet class used by both SENDER and RECEIVER

import java.net.*;
import java.util.HashMap;
import java.util.HashSet;

class ACKReceiver extends Thread  {
	public void run() {
		try {
			HashSet<Integer> judge_base = new HashSet<>();
			byte[] receive = new byte[512];
			// Create socket to listen for ACKs from emulator at receive_port
			DatagramSocket socket0 = new DatagramSocket(sender.receiveport);
			DatagramPacket receiveAck = new DatagramPacket(receive, receive.length);
			while (true) {
				socket0.receive(receiveAck);
				// parse buffer into packet
				packet packet0 = packet.parseUDPdata(receive);
				int seqnumber = packet0.getSeqNumber();
				judge_base.add(seqnumber);//add the already received value
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
				if(sender.windowsize < sender.WINDOW_N) {
					sender.windowsize++;
					sender.Nlog.write("t=" + (sender.timestamp - 1) + " " + sender.windowsize + "\r\n");
					sender.Nlog.flush();
				}
				// if seqNum overflows, adjust sender base accordingly
				System.out.print("seqnumber: " + seqnumber);
				//int receive_seq = 32*((sender.nextSeqNum-seqnumber)/32) + (seqnumber+1)%32;
				/////////////////////////////////serious things//////////////////
				int receive_seq = seqnumber + 1;
				if((sender.baseseqnumber + 1)%32 == receive_seq%32) {
					int i = 0;
					while(judge_base.contains((receive_seq + i + 1)%32)) {
						sender.baseseqnumber++;
						i++;
					}
					sender.baseseqnumber += (i + 1);
					//sender.baseseqnumber = 32*((sender.nextSeqNum-seqnumber)/32) + (seqnumber+1)%32;
					judge_base.clear();
				}
				//////////////////////////////////////////////////////////////////
				System.out.println(" BaseAck: " + sender.baseseqnumber);
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
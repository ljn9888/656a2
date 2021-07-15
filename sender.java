import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

public class sender {
    public static int windowsize = 1;
    public static int timestamp = 0;
    public static int timeinteval ;
    public static int baseseqnumber = 0;
    public static int nextSeqNum = 0;
    static String emulatoraddress;
    static int emulatorport;
    static final int WINDOW_N = 10;
    static int string_length = 75;
    public static int receiveport;
    static String filename;
    static ArrayList<packet> send_string_all = new ArrayList<>();
    static FileWriter seqnumlog;
    static FileWriter acklog;
    static FileWriter Nlog;
    public static Timer timer = new Timer();

    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("there should be 5 cml argument input");
            return;
        }
        // import cml arguments
        emulatoraddress = args[0];
        emulatorport = Integer.parseInt(args[1]);
        receiveport = Integer.parseInt(args[2]);
        timeinteval = Integer.parseInt(args[3]);
        filename = args[4];
        // Start thread to receive acks from emulator
        ACKReceiver ackreceiver = new ACKReceiver();
        ackreceiver.start();
        try {
            ////////////////////////////// Start read the file into send_string_all///////////////////////
            FileInputStream file_input = new FileInputStream(filename);
            int length_input;
            byte[] readBuffer = new byte[string_length];
            for(int i = 0; (length_input = file_input.read(readBuffer)) > 0; i++) {
                String send_string = new String(readBuffer, 0, length_input);
                packet send_packet = packet.Packet(i, send_string);
                send_string_all.add(send_packet);
            }
            int total_packets_num = send_string_all.size();
            file_input.close();
            System.out.println("Window's N: " + WINDOW_N);
            System.out.println("Total packets: " + total_packets_num);
            ///////////////////////all string are saved at send_string_all/////////////////////
            // initiate FileWriter objects
            seqnumlog = new FileWriter("seqnum.log");
            acklog = new FileWriter("ack.log");
            Nlog = new FileWriter("N.log");
            Nlog.write("t=" + timestamp++ + " " + windowsize + "\r\n");
            /////////////////////////////recycling for send packet//////////////////////
            while (true) {
                if (nextSeqNum == total_packets_num) {
                    Thread.sleep(2000);;
                    packet eot = packet.EOT(nextSeqNum + 1);
                    sender.seqnumlog.write("t=" + timestamp++ + " EOT");
                    sender.acklog.write("t=" + timestamp++ + " EOT");
                    sender.udp_send(eot);
                    sender.seqnumlog.close();
                    sender.acklog.close();
                    break;}        //end the loop when all packets send to the receiver
                if (nextSeqNum < baseseqnumber + windowsize) {
                    udp_send(send_string_all.get(nextSeqNum));
                    if (baseseqnumber == nextSeqNum) {
                        timer.schedule(new Timeout(), timeinteval);//start count on timeout
                    }
                    nextSeqNum++;
                }
                System.out.print("nextSeqNum: " + nextSeqNum);
                System.out.print("  base: " + baseseqnumber);
                System.out.println("  total_packets_num: " + total_packets_num);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    static class Timeout extends TimerTask {

        public void run()  {
            System.out.println("Timeout here"); //gengrate timeout in an different thread
            try{
                timer.schedule(new Timeout(), 100);}catch (Exception e) {System.out.println("timer already cancelled, its okay");}
            try{
                windowsize = 1;
                Nlog.write("t=" + timestamp + " " + windowsize + "\r\n");
                Nlog.flush();
            }catch (IOException e){
                e.printStackTrace();
            }
            udp_send(send_string_all.get(baseseqnumber));
        }
    }

    public static void udp_send(packet packet0) {
        byte[] sendBuffer;
        sendBuffer = packet0.getUDPdata();
        try {
            // input sequence numbers in seqnum.log
            if (packet0.getType() != 2) {
                seqnumlog.write("t=" + timestamp++ + " " + packet0.getSeqNumber() + "\r\n");
                seqnumlog.flush();}
            DatagramSocket socket0 = new DatagramSocket();
            DatagramPacket packet1 = new DatagramPacket(sendBuffer, packet0.getLength() + 12, InetAddress.getByName(emulatoraddress), emulatorport);
            socket0.send(packet1);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
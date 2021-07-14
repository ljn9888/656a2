import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class receiver {
  public static String emulatoraddress;
  public static int emulatorport;
  public static int receiveport;
  public static String filename;

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.out.println("Incorrect number of command line arguments");
      System.out.println("Proper usage: <emulator hostname> <emulator receiving UDP port> <receiver UDP port> <output file>");
      System.exit(0);
    }
    emulatoraddress = args[0];
    emulatorport = Integer.parseInt(args[1]);
    receiveport = Integer.parseInt(args[2]);
    filename = args[3];
    int seqnumber = 0;
    int expectSeqnumber = 0;
    ////////////////finish initiate/////////////
    try {
      /////////////////////////initiate udp link, receive port and arrival.log////////////////
      DatagramSocket socket0 = new DatagramSocket(receiveport);
      byte[] buffer = new byte[512];
      DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
      FileWriter fw = new FileWriter(filename);
      FileWriter arrivallog = new FileWriter("arrival.log");
      //////////////////////////start the loop of listen to the packet and comapare the seq number////
      while (true) {
        //listen the packet port
        socket0.receive(receivePacket);
        packet packet0 = packet.parseUDPdata(buffer);
        int type = packet0.getType();
        seqnumber = packet0.getSeqNumber();

        if (type == 2) { // End the program when have EOT
          packet eot = packet.EOT(seqnumber);
          arrivallog.write("EOT");
          udt_send(eot);
          // exit receiver
          fw.close();
          arrivallog.close();
          return;
        }
        arrivallog.write(seqnumber + "\n"); // input sequencenumber ro arrival.log
        arrivallog.flush();
        System.out.println(seqnumber);
        System.out.println("expectedSeqnumber" + expectSeqnumber);

        if (seqnumber == expectSeqnumber) {
          System.out.println(packet0.getString());   // extract data and send it to output file
          fw.write(packet0.getString());
          fw.flush();
          packet ack = packet.ACK(seqnumber);
          udt_send(ack);
          if (expectSeqnumber == 31) {
            expectSeqnumber = 0;
            continue;
          }
          expectSeqnumber++;
        } else if (expectSeqnumber != 0) { // if we receive an out of order packet that's not the first
          packet ack = packet.ACK(expectSeqnumber-1);
          udt_send(ack);
        }

      }
    } catch (Exception e) {
      System.err.println("error" + e);
    }
    packet eot = packet.EOT(seqnumber);
    udt_send(eot);
    System.out.println("hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh");
  }


  public static void udt_send(packet packet1) {
    byte[] sendstring;
    sendstring = packet1.getUDPdata();
    try {
      DatagramSocket socket0 = new DatagramSocket();
      DatagramPacket packet0 = new DatagramPacket(sendstring, 12, InetAddress.getByName(emulatoraddress), emulatorport);
      socket0.send(packet0);
    } catch (Exception e) {
       System.out.println(e);
    }
  }
}

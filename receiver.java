import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class receiver {
  public static String emulatoraddress;
  public static int emulatorport;
  public static int receiveport;
  public static String filename;
  public static HashMap<Integer, packet> receivebuffer = new HashMap<>();

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
        System.out.print(" received" + seqnumber);
        System.out.print(" expected" + expectSeqnumber);
        ///////////////handle packet here///////////////////
        if (seqnumber == expectSeqnumber) {
          ////System.out.println(packet0.getString());   // extract data and send it to output file
          fw.write(packet0.getString());
          fw.flush();
          packet ack = packet.ACK(seqnumber);
          udt_send(ack);
          System.out.println(" Ack:" + expectSeqnumber);
          expectSeqnumber = (expectSeqnumber + 1)%32;
          ////////////////////////////////////////clear buffer///////////
          {
            Boolean judge_buffer = false;
            while(receivebuffer.containsKey(expectSeqnumber))
            {
              judge_buffer = true;
              fw.write(receivebuffer.get(expectSeqnumber).getString());
              fw.flush();
              expectSeqnumber = (expectSeqnumber + 1)%32;
            }


            if(judge_buffer){packet ack0;
              if(expectSeqnumber == 0)
                ack0 = packet.ACK(31);
              else
                ack0 = packet.ACK(expectSeqnumber - 1);
              System.out.println(receivebuffer);
              receivebuffer.clear();
              System.out.println(" heheAck:" + (expectSeqnumber - 1));
              udt_send(ack0);}
          }
          /////////////////////////////////////////clear buffer////////////////
        } else if (expectSeqnumber != 0) { // if we receive an out of order packet that's not the first
          packet ack = packet.ACK(seqnumber);
          udt_send(ack);
          System.out.println(" wrongAck:" + seqnumber);
          if(expectSeqnumber<22 && seqnumber > expectSeqnumber && seqnumber < expectSeqnumber + 10) {
            receivebuffer.put(seqnumber, packet0);
          }
          if(expectSeqnumber>=22 && (seqnumber > expectSeqnumber || seqnumber <= expectSeqnumber - 21)) {
            receivebuffer.put(seqnumber, packet0);
          }
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
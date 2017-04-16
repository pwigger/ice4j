package test;

import sun.net.ftp.FtpClient;
import sun.net.ftp.FtpClientProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by pwg on 13.04.17.
 */
public class Start {

  public static final String COMPUTERNAME = "PwgMacbook";
  public static final String REMOTECOMPUTERNAME = "PwgRaspberryPie";
  public static final int PORT = 2020;

  public static void main(String[] args) throws Throwable {
    IceProcess iceProcess = new IceProcess(PORT);
    String localSDP = iceProcess.generateLocalSDP();
    iceProcess.printSDP();
    File file = new File("resources/IceSDP/sdp" + COMPUTERNAME + ".txt");
    saveToFile(localSDP, file);
    uploadFile(file);
    String remoteSDP = null;

    while (remoteSDP == null) {
      remoteSDP = downloadFile("http://www.pwigger.ch/rbp/sdp" + REMOTECOMPUTERNAME + ".txt");
      System.out.println("trying to get remote sdp...");
      Thread.sleep(5000);
    }
    System.out.println("Got remote SDP from server");

    iceProcess.tryConnect(remoteSDP);


    DatagramSocket socket = iceProcess.getSocket();
    if (socket == null) {
      throw new Exception("not sucessful connecting");
    }

    sendTestPackages(socket);

    //test.Start TCP Server - Person die Remote Support will
    IcePseudoTcp.LocalPseudoTcpJob server = new IcePseudoTcp.LocalPseudoTcpJob(socket);
    server.start();


    //test.Start TCP Client - Person die Support gibt
    IcePseudoTcp.RemotePseudoTcpJob client = new IcePseudoTcp.RemotePseudoTcpJob(socket, new InetSocketAddress(socket.getInetAddress(), PORT));
    client.start();


    Thread.sleep(10000);

  }


  private static void startListener(int port) {
    PortListener portListener = new PortListener(port);
    portListener.start();
  }

  private static void sendTestPackages(DatagramSocket socket) throws Throwable {
    for (int i = 0; i < 100; i++) {
      String message = "Testpackage" + i + " from " + COMPUTERNAME;
      DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length());
      socket.send(packet);
    }
  }

  private static void saveToFile(String localSDP, File file) throws Throwable {
    FileOutputStream fos = new FileOutputStream(file);
    fos.write(localSDP.getBytes());
  }

  static void uploadFile(File file) throws Exception {
    FtpClientProvider ftpClientProvider = FtpClientProvider.provider();
    FtpClient ftp = ftpClientProvider.createFtpClient();
    ftp.connect(new InetSocketAddress(InetAddress.getByName("94.126.16.19"), 21));
    ftp = ftp.login("rbp", "".toCharArray());
    ftp.putFile(file.getName(), new FileInputStream(file));
  }

  private static String downloadFile(String urlAsString) throws Throwable {
    URL url = new URL(urlAsString);
    Scanner s = new Scanner(url.openStream());
    StringBuilder remoteSDP = new StringBuilder("");
    while (s.hasNext()) {
      String line=s.nextLine();
        line = line.replace("[java]", "");
        line = line.trim();
        if (line.length() == 0) {
          break;}
        remoteSDP.append(line);
        remoteSDP.append("\r\n");
    }
    System.out.println(remoteSDP.toString());

    return remoteSDP.toString();
  }


}


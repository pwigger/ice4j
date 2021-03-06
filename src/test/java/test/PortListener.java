package test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by pwg on 18.03.17.
 */
public class PortListener extends Thread {
  int port;

  public PortListener(int port) {
    this.port = port;
  }

  @Override
  public void run() {

    System.out.println("I listen now on port " + port);


    try {
      DatagramSocket ds = new DatagramSocket(port);

      while (true) {
        DatagramPacket dp = new DatagramPacket(new byte[256], 256);
        ds.receive(dp);
        byte[] bytes = dp.getData();
        String message = new String(dp.getData()).replace(' ', ' ');
       System.out.println("Thread Listener got UPD-Package: " + message);
      }

    } catch (java.io.IOException e) {
      System.out.println(e);
    }
  }


}

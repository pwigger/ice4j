package test;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.NominationStrategy;
import org.ice4j.ice.harvest.CandidateHarvester;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.ice.harvest.UPNPHarvester;
import org.ice4j.security.LongTermCredential;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by pwg on 13.04.17.
 */
public class IceProcess {
  /**
   * The <tt>Logger</tt> used by the <tt>Ice</tt>
   * class and its instances for logging output.
   */
  private static final Logger logger
      = Logger.getLogger(IceProcess.class.getName());
  /**
   * test.Start time for debugging purposes.
   */
  static long startTime;


  private static InetAddress hostname;
  private static int port;
  private static int sdpCount = 0;
  private Agent localAgent;
  private StateListener stateListener;
  private String localSDP;
  private DatagramSocket socket;


  public IceProcess(int port) throws Throwable {
    this.localAgent = createAgent(port, false, null);
    this.stateListener = new StateListener(this);
    localAgent.setNominationStrategy(NominationStrategy.NOMINATE_HIGHEST_PRIO);
    localAgent.addStateChangeListener(stateListener);
  }

  public String generateLocalSDP() throws Throwable {
    //let them fight ... fights forge character.
    localAgent.setControlling(false);
    localSDP = SdpUtils.createSDPDescription(localAgent);
    //wait a bit so that the logger can stop dumping stuff:
    Thread.sleep(500);
    return localSDP;
  }

  public void printSDP() {
    System.out.println("=================== feed the following"
        + " to the remote agent ===================");

    System.out.println(localSDP);

    System.out.println("======================================"
        + "========================================\n");
    System.out.println();
  }

  public void startConnectivityEstablishment() {
    localAgent.startConnectivityEstablishment();
  }

  public void tryConnect(String sdp) throws Throwable {
    startTime = System.currentTimeMillis();
    SdpUtils.parseSDP(localAgent, sdp);
    localAgent.startConnectivityEstablishment();
   //Does not work! Evt weil Multithreading??
//    if (socket != null) {
//      System.out.println("We Have a socket");
//      return socket;
//    }

    //Give processing enough time to finish. We'll System.exit() anyway
    //as soon as localAgent enters a final state.
    Thread.sleep(10000);
  }



  /**
   * Creates an ICE <tt>Agent</tt> (vanilla or trickle, depending on the
   * value of <tt>isTrickling</tt>) and adds to it an audio and a video stream
   * with RTP and RTCP components.
   *
   * @param rtpPort     the port that we should try to bind the RTP component on
   *                    (the RTCP one would automatically go to rtpPort + 1)
   * @param isTrickling indicates whether the newly created agent should be
   *                    performing trickle ICE.
   * @param harvesters  the list of {@link CandidateHarvester}s that the new
   *                    agent should use or <tt>null</tt> if it should include the default ones.
   * @return an ICE <tt>Agent</tt> with an audio stream with RTP and RTCP
   * components.
   * @throws Throwable if anything goes wrong.
   */
  private static Agent createAgent(int rtpPort, boolean isTrickling, List<CandidateHarvester> harvesters) throws Throwable {

    long startTime = System.currentTimeMillis();
    Agent agent = new Agent();
    agent.setTrickling(isTrickling);

    if (harvesters == null) {
      // STUN
      StunCandidateHarvester stunHarv = new StunCandidateHarvester(
          new TransportAddress("stun.jitsi.net", 3478, Transport.UDP));
      StunCandidateHarvester stun6Harv = new StunCandidateHarvester(
          new TransportAddress("stun6.jitsi.net", 3478, Transport.UDP));

      agent.addCandidateHarvester(stunHarv);
      agent.addCandidateHarvester(stun6Harv);

      // TURN
      String[] hostnames = new String[]
          {
              "stun.jitsi.net",
              "stun6.jitsi.net"
          };
      int port = 3478;
      LongTermCredential longTermCredential
          = new LongTermCredential("guest", "anonymouspower!!");

      for (String hostname : hostnames) {
        agent.addCandidateHarvester(
            new TurnCandidateHarvester(
                new TransportAddress(
                    hostname, port, Transport.UDP),
                longTermCredential));
      }

      //UPnP: adding an UPnP harvester because they are generally slow
      //which makes it more convenient to test things like trickle.
      agent.addCandidateHarvester(new UPNPHarvester());
    } else {
      for (CandidateHarvester harvester : harvesters) {
        agent.addCandidateHarvester(harvester);
      }
    }

    //STREAMS
    createStream(rtpPort, "audio", agent);
    createStream(rtpPort + 2, "video", agent);


    long endTime = System.currentTimeMillis();
    long total = endTime - startTime;

    logger.info("Total harvesting time: " + total + "ms.");

    return agent;
  }

  /**
   * Creates an <tt>IceMediaStream</tt> and adds to it an RTP and and RTCP
   * component.
   *
   * @param rtpPort    the port that we should try to bind the RTP component on
   *                   (the RTCP one would automatically go to rtpPort + 1)
   * @param streamName the name of the stream to create
   * @param agent      the <tt>Agent</tt> that should create the stream.
   * @return the newly created <tt>IceMediaStream</tt>.
   * @throws Throwable if anything goes wrong.
   */
  private static IceMediaStream createStream(int rtpPort, String streamName, Agent agent) throws Throwable {
    IceMediaStream stream = agent.createMediaStream(streamName);

    long startTime = System.currentTimeMillis();

    //TODO: component creation should probably be part of the library. it
    //should also be started after we've defined all components to be
    //created so that we could run the harvesting for everyone of them
    //simultaneously with the others.

    //rtp
    agent.createComponent(
        stream, Transport.UDP, rtpPort, rtpPort, rtpPort + 100);

    long endTime = System.currentTimeMillis();
    logger.info("RTP Component created in "
        + (endTime - startTime) + " ms");
    startTime = endTime;
    //rtcpComp
    agent.createComponent(
        stream, Transport.UDP, rtpPort + 1, rtpPort + 1, rtpPort + 101);

    endTime = System.currentTimeMillis();
    logger.info("RTCP Component created in "
        + (endTime - startTime) + " ms");

    return stream;
  }


  public DatagramSocket getSocket() {
    return socket;
  }

  public void setSocket(DatagramSocket socket) {
    this.socket = socket;
  }
}















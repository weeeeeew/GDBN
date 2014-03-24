package network;

import net.tomp2p.futures.BaseFuture;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.futures.FutureResponse;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.RequestP2PConfiguration;
import net.tomp2p.p2p.builder.SendBuilder;
import net.tomp2p.p2p.builder.SendDirectBuilder;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by Leif on 2014-03-19.
 */
public class Passer {

    private final Peer peer;

    private final Random random = new Random();

    //TODO <Long, Command> instead of <Long, String>...
    private final HashMap<Long, String> pendingRequests = new HashMap<>();

    public Passer(final Peer peer) {
        this.peer = peer;
        peer.setObjectDataReply(new ObjectDataReply() {
            @Override
            public Object reply(PeerAddress sender, Object request) throws Exception {

                if(!(request instanceof NetworkMessage)){
                    System.out.println("in Passer: ERROR! some request was not a NetworkMessage");
                    return null;
                }
                if(peer.getPeerAddress().equals(sender)){
                    System.out.println("in Passer: ERROR! sender is myself!!!");
                }

                NetworkMessage message = (NetworkMessage) request;
                System.out.println("ObjectDataReply:" + message.toString());

                switch (message.getType()){
                    case OK:
                        String resolved = pendingRequests.remove(message.getRef());
                        if(resolved==null){
                            System.out.println("OK received for unknown! Ref "+message.getRef());
                        }else{
                            System.out.println("OK received for "+resolved);
                        }
                        break;
                    case REQUEST:
                        System.out.println("REQUEST received: "+message.getObject());
                        sendMessage(sender, new NetworkMessage(null, NetworkMessage.Type.OK, message.getRef()));
                        break;
                    case NO_REPLY:
                        System.out.println("NO_REPLY received: "+message.getObject());
                        break;
                }
                return null;
            }
        });
    }

    public void sendRequest(PeerAddress receiver, String data){
        Long ref = random.nextLong();
        pendingRequests.put(ref, data);
        sendMessage(receiver, new NetworkMessage(data, NetworkMessage.Type.REQUEST, ref));
    }

    public void sendReply(PeerAddress receiver, Object data, Long ref){
        sendMessage(receiver, new NetworkMessage(data, NetworkMessage.Type.NO_REPLY, ref));
    }

    /**
     * In testing, the message gets through but the Future says not successful...
     * Perhaps has something to do with the reply... TODO check if return "OK" changes that
     * TODO make good message passing protocol for Tasks
     * @param receiver other peer
     * @param networkMessage Any object to send
     */
    private void sendMessage(PeerAddress receiver, final NetworkMessage networkMessage){
        RequestP2PConfiguration requestP2PConfiguration = new RequestP2PConfiguration(1, 10, 0);
        SendBuilder sendBuilder = peer.send(receiver.getID());

        FutureDHT futureDHT = sendBuilder.setObject( networkMessage ).setRequestP2PConfiguration(requestP2PConfiguration).start();
        futureDHT.addListener(new BaseFutureAdapter<BaseFuture>() {
            @Override
            public void operationComplete(BaseFuture future) throws Exception {
                if(!future.isSuccess()){
                    System.out.println("Error sending " + networkMessage.toString());
                    return;
                }
                System.out.println("Success sending " + networkMessage.toString());
            }
        });

    }

    /**
     * SendDirect. Seems to work just as well or worse
     * @param receiver r
     * @param message m
     *
     * @deprecated
     */
    public void sendd(PeerAddress receiver, final Object message){
        SendDirectBuilder sendDirectBuilder = peer.sendDirect(receiver);
        sendDirectBuilder.setObject(message);
        FutureResponse futureResponse = sendDirectBuilder.start();
        futureResponse.addListener(new BaseFutureAdapter<BaseFuture>() {
            @Override
            public void operationComplete(BaseFuture future) throws Exception {
                if(!future.isSuccess()){
                    System.out.println("Error sendDing "+message.toString());
                    return;
                }
                System.out.println("Success sendDing "+message.toString());
            }
        });
    }
}

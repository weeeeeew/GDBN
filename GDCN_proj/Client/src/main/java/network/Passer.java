package network;

import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.RequestP2PConfiguration;
import net.tomp2p.p2p.builder.SendBuilder;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;

import java.io.Serializable;

/**
 * Created by Leif on 2014-03-19.
 *
 * There must only be ONE Passer for each Peer!
 */
abstract class Passer {

    private final Peer peer;

    private final static RequestP2PConfiguration requestConfiguration = new RequestP2PConfiguration(1, 10, 0);
    private final static RequestP2PConfiguration noReplyConfiguration = new RequestP2PConfiguration(0, 10, 0);

    public Passer(final Peer peer) {
        this.peer = peer;
        peer.setObjectDataReply(new ObjectDataReply() {
            @Override
            public Object reply(PeerAddress sender, Object request) throws Exception {

                if(peer.getPeerAddress().equals(sender)){
                    System.out.println("in Passer: ERROR! sender is myself!!!");
                }

                NetworkMessage message = NetworkMessage.decrypt( request);
                if(message == null){
                    //Error has occured in decrypt
                    System.out.println("Decrypt returned NULL!");
                    return "Decrypt was NULL";
                }
                System.out.println("ObjectDataReply: " + message.getType().name());

                switch (message.getType()){
                    case REQUEST:
                        //TODO remove these outputs
//                        System.out.println("REQUEST received: "+message.getObject());
                        Serializable reply = handleRequest(sender, message.getObject());
//                        sendMessage(sender, new NetworkMessage(reply, NetworkMessage.Type.OK, message.getRef()));
                        return reply;
                    case NO_REPLY:
//                        System.out.println("NO_REPLY received: "+message.getObject());
                        handleNoReply(sender, message.getObject());
                        return "Message was Handled in some way...";
                }
                return "Message was read but not Handled! Type: "+message.getType().name();
            }
        });
    }

    protected abstract Serializable handleRequest(PeerAddress sender, Object messageContent);

    protected abstract void handleNoReply(PeerAddress sender, Object messageContent);

    protected void sendRequest(PeerAddress receiver, Serializable data, final OnReplyCommand onReturn){
        SendBuilder sendBuilder = peer.send(receiver.getID());

        final NetworkMessage networkMessage = new NetworkMessage(data, NetworkMessage.Type.REQUEST);

        FutureDHT futureDHT = sendBuilder.setObject( networkMessage.encrypt() ).setRequestP2PConfiguration(requestConfiguration).start();
        futureDHT.addListener(new BaseFutureAdapter<FutureDHT>() {
            @Override
            public void operationComplete(FutureDHT future) throws Exception {
                if(!future.isSuccess()){
                    System.out.println("Error sending " + networkMessage.toString());
                    System.out.println("WHY: "+future.getFailedReason());
                    return;
                }
                System.out.println("Success sending " + networkMessage.toString());
                for(PeerAddress address : future.getRawDirectData2().keySet()){
                    Object answer = future.getRawDirectData2().get(address);
                    onReturn.execute(answer);
                    System.out.println(""+address+" answered with "+answer);
                }
            }
        });
    }

    protected void sendNoReplyMessage(PeerAddress receiver, Serializable data){
        SendBuilder sendBuilder = peer.send(receiver.getID());

        final NetworkMessage networkMessage = new NetworkMessage(data, NetworkMessage.Type.NO_REPLY);

        FutureDHT futureDHT = sendBuilder.setObject( networkMessage.encrypt() ).setRequestP2PConfiguration(noReplyConfiguration).start();
        futureDHT.addListener(new BaseFutureAdapter<FutureDHT>() {
            @Override
            public void operationComplete(FutureDHT future) throws Exception {
                if(!future.isSuccess()){
                    System.out.println("Error sending " + networkMessage.toString());
                    System.out.println("WHY: "+future.getFailedReason());
                    return;
                }
                System.out.println("Success sending " + networkMessage.toString());
                for(PeerAddress address : future.getRawDirectData2().keySet()){
                    Object answer = future.getRawDirectData2().get(address);
                    System.out.println(""+address+" answered with "+answer);
                }
            }
        });
    }
}

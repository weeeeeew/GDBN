package network;

import java.io.Serializable;

/**
 * Created by Leif on 2014-03-24.
 */
public class NetworkMessage implements Serializable {

    private final Serializable object;
    private final Type type;
    private final Long ref;

    public NetworkMessage(Serializable object, Type type, Long ref) {
        this.object = object;
        this.type = type;
        this.ref = ref;
    }

    public Object getObject() {
        return object;
    }

    public Type getType() {
        return type;
    }

    public Long getRef() {
        return ref;
    }

    /**
     * Encrypt message using receiving peer's public key
     * @return encrypted message
     */
    public Object encrypt(){
        //TODO encrypt and sign message
//        try {
//            return new Data(this);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return this;
    }

    /**
     * Decrypt message using private key
     * @param data Encrypted message
     * @return Decrypted message
     */
    public static NetworkMessage decrypt(Object data){
//        //TODO decrypt message
//        try {
//            return (NetworkMessage) data.getObject();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return (NetworkMessage) data;
    }

    @Override
    public String toString() {
        return "NetworkMessage{" +
                "object=" + object +
                ", type=" + type +
                '}';
    }

    public static enum Type {
        OK,
        REQUEST,
        NO_REPLY
    }


}

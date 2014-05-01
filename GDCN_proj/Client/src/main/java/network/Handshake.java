package network;

import javax.crypto.interfaces.DHPublicKey;
import java.io.Serializable;
import java.security.interfaces.RSAPublicKey;

/**
 * Created by weeeeeew on 2014-04-10.
 */
public class Handshake implements Serializable {
    public final DHPublicKey dhKey;
    public final RSAPublicKey rsaKey;
    public final Stage stage;

    public Handshake(DHPublicKey dhKey, RSAPublicKey rsaKey) {
        this(dhKey,rsaKey,Stage.INIT);
    }

    private Handshake(DHPublicKey dhKey, RSAPublicKey rsaKey, Stage stage) {
        this.dhKey = dhKey;
        this.rsaKey = rsaKey;
        this.stage = stage;
    }

    public Handshake reply(DHPublicKey dhKey, RSAPublicKey rsaKey) {
        return stage == Stage.INIT ? new Handshake(dhKey,rsaKey,Stage.REPLY) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Handshake) {
            Handshake hs = (Handshake) o;
            return this.dhKey.equals(hs.dhKey) && this.rsaKey.equals(hs.rsaKey) && this.stage == hs.stage;
        }

        return false;
    }

    public enum Stage {
        INIT,
        REPLY
    }
}
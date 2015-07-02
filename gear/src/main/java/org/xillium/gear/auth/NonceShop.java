package org.xillium.gear.auth;

import java.util.*;
import java.security.SecureRandom;
import org.xillium.base.Perishable;
import org.xillium.base.beans.Strings;


/**
 * NonceShop is a producer of nonce values used in authentication/verification.
 */
public class NonceShop {

    /**
     * Constructs a NonceShop running on a JVM instance with a TTL and of a given byte size.
     *
     * @param instance the instance number of this JVM
     * @param ttl the default time-to-live in milliseconds for each nonce produced
     * @param size the byte size of nonces
     */
    public NonceShop(byte instance, long ttl, byte size) {
        INSTANCE = instance;
        TTL = ttl;
        SIZE = size;
    }

    /**
     * Reports nonce TTL.
     *
     * @return the default nonce TTL
     */
    public long getTimeToLive() {
        return TTL;
    }

    /**
     * Produces a nonce of an arbitrary size in bytes, which is not to be proved later.
     *
     * @param size the size of the nonce in bytes
     * @return the new nonce as a hexadecimal string
     */
    public String produce(int size) {
        byte bytes[] = new byte[size];
        _random.nextBytes(bytes);
        return Strings.toHexString(bytes);
    }

    /**
     * Produces a nonce that is associated with the given state and is to be proved within a time limit.
     *
     * @param state an arbitrary state to be associated with the new nonce.
     * @param time the TTL of the new nonce, in milliseconds. If this parameter is 0 or negative, the default TTL is used instead.
     * @return the new nonce as a hexadecimal string
     */
    public String produce(String state, long time) {
        Nonce nonce = new Nonce(INSTANCE, time > 0 ? time : TTL, SIZE, _random);
        _map.put(state + ':' + nonce.value, nonce);
        return nonce.value;
    }

    /**
     * Renews a nonce. The nonce must be valid and must have not expired.
     */
    public boolean renew(String value, String state, long time) {
        Nonce nonce = _map.get(state + ':' + value);
        if (nonce != null && !nonce.hasExpired()) {
            nonce.renew(time > 0 ? time : TTL);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Proves a nonce and its associated state. A nonce can only be successfully proved once.
     *
     * @param value the nonce value
     * @param state the state associated with the nonce.
     * @return whether the nonce is successfully proved
     */
    public boolean prove(String value, String state) {
        Nonce nonce = _map.remove(state + ':' + value);
        return nonce != null && !nonce.hasExpired();
    }

    private static class Nonce implements Perishable {
        final String value;
        long expiration;

        Nonce(byte instance, long time, int size, SecureRandom random) {
            byte bytes[] = new byte[size];
            random.nextBytes(bytes);
            bytes[0] = instance;
            value = Strings.toHexString(bytes);
            expiration = System.currentTimeMillis() + time;
        }

        void renew(long time) {
            expiration = System.currentTimeMillis() + time;
        }

        @Override
        public boolean hasExpired() {
            return expiration < System.currentTimeMillis();
        }
    }

    private final Map<String, Nonce> _map = Collections.synchronizedMap(new Perishable.Map<String, Nonce>());
    private final SecureRandom _random = new SecureRandom();
    private final byte INSTANCE;
    private final byte SIZE;
    private final long TTL;
}

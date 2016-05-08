package id.ac.ui.clab.dchronochat;


import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.sync.ChronoSync2013;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by yudiandreanp on 03/05/16.
 */


public class DChronoChat implements ChronoSync2013.OnInitialized, ChronoSync2013.OnReceivedSyncState, OnData, OnInterestCallback {

    private final String screenName; //the screen name of the user
    private final String userName;
    private final String hubPrefix; //the prefix of the hub
    private final Name chatPrefix_;
    private final String chatRoom; //chatroom name
    private int maxMsgCacheLength;
    private boolean isRecoverySyncState;
    private float syncLifetime;
    private final Face face;
    private final KeyChain keyChain;
    private final Name certificateName;
    private final double syncLifetime = 5000.0; // milliseconds
    private ChronoSync2013 sync;
    private final OnTimeout heartbeat;

    // Use a non-template ArrayList so it works with older Java compilers.
    private final ArrayList messageCache_ = new ArrayList(); // of CachedMessage
    private final ArrayList roster = new ArrayList(); // of String
    private final int maxMessageCacheLength = 100;
    private boolean isRecoverySyncState = true;

    public DChronoChat(String screenName, String userName, String chatRoom,
                       String hubPrefix, Face face, KeyChain keyChain, Name certificateName)
    {
        this.screenName = screenName;
        this.userName = userName;
        this.chatRoom = chatRoom;
        this.hubPrefix = hubPrefix;
        this.face = face;
        this.keyChain = keyChain;
        this.certificateName = certificateName;

    }

    /**
     * Append a new CachedMessage to messageCache_, using given messageType and message,
     * the sequence number from sync_.getSequenceNo() and the current time. Also
     * remove elements from the front of the cache as needed to keep
     * the size to maxMessageCacheLength_.
     */
    private void messageCacheAppend(ChatMessage.ChatMessageType messageType, String message)
    {
        messageCache.add(new CachedMessage
                (sync.getSequenceNo(), messageType, message, getNowMilliseconds()));
        while (messageCache.size() > maxMessageCacheLength)
            messageCache.remove(0);
    }

    // Generate a random name for ChronoSync.
    private static String
    getRandomString()
    {
        String seed = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789";
        String result = "";
        Random random = new Random();
        for (int i = 0; i < 10; ++i) {
            // Using % means the distribution isn't uniform, but that's OK.
            int position = random.nextInt(256) % seed.length();
            result += seed.charAt(position);
        }

        return result;
    }


}

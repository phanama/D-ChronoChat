package id.ac.ui.clab.dchronochat;

import id.ac.ui.clab.dchronochat.ChatbufProto.ChatMessage;

import com.google.protobuf.InvalidProtocolBufferException;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.sync.ChronoSync2013;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by yudiandreanp on 03/05/16.
 */


public class DChronoChat implements ChronoSync2013.OnInitialized, ChronoSync2013.OnReceivedSyncState, OnData, OnInterestCallback {

    private final String screenName; //the screen name of the user
    private final String userName;
    //private final String hubPrefix; //the prefix of the hub
    private final Name chatPrefix;
    private final String chatRoom; //chatroom name
    private int maxMsgCacheLength;
    private final Face face;
    private final KeyChain keyChain;
    private final Name certificateName;
    private final double syncLifetime = 5000.0; // milliseconds
    private ChronoSync2013 sync;
    private final OnTimeout heartbeat;

    // Use a non-template ArrayList so it works with older Java compilers.
    private ArrayList messageCache = new ArrayList(); // of CachedMessage
    private ArrayList roster = new ArrayList(); // of String
    private final int maxMessageCacheLength = 100;
    private boolean isRecoverySyncState = true;

    public DChronoChat(String screenName, String chatRoom, Name hubPrefix,
                       Face face, KeyChain keyChain, Name certificateName)
    {
        this.screenName = screenName;
        this.chatRoom = chatRoom;
        this.face = face;
        this.keyChain = keyChain;
        this.certificateName = certificateName;
        heartbeat = new Heartbeat();

        // This should only be called once, so get the random string here.
        chatPrefix = new Name(hubPrefix).append(chatRoom).append(getRandomString());
        int session = (int)Math.round(getNowMilliseconds() / 1000.0);
        userName = screenName + session;
        try {
            sync = new ChronoSync2013
                    (this, this, chatPrefix,
                            new Name("/ndn/broadcast/ChronoChat").append(chatRoom), session,
                            face, keyChain, certificateName, syncLifetime, RegisterFailed.onRegisterFailed);
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        try {
            face.registerPrefix(chatPrefix, this, RegisterFailed.onRegisterFailed);
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Send a chat message.
    public final void
    sendMessage(String chatMessage) throws IOException, SecurityException
    {
        if (messageCache.size() == 0)
            messageCacheAppend(ChatMessage.ChatMessageType.JOIN, "xxx");

        // Ignore an empty message.
        // forming Sync Data Packet.
        if (!chatMessage.equals("")) {
            sync.publishNextSequenceNo();
            messageCacheAppend(ChatMessage.ChatMessageType.CHAT, chatMessage);
            System.out.println(screenName + ": " + chatMessage);
        }
    }

    // Send leave message and leave.
    public final void leave() throws IOException, SecurityException
    {
        sync.publishNextSequenceNo();
        messageCacheAppend(ChatMessage.ChatMessageType.LEAVE, "xxx");
    }


    /**
     * Append a new CachedMessage to messageCache, using given messageType and message,
     * the sequence number from sync.getSequenceNo() and the current time. Also
     * remove elements from the front of the cache as needed to keep
     * the size to maxMessageCacheLength.
     */
    public void messageCacheAppend(ChatMessage.ChatMessageType messageType, String message)
    {
        messageCache.add(new CachedMessage
                (sync.getSequenceNo(), messageType, message, getNowMilliseconds()));
        while (messageCache.size() > maxMessageCacheLength)
            messageCache.remove(0);
    }

    // Generate a random name for ChronoSync.
    private static String getRandomString()
    {
        String seed = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM012345678a9";
        String result = "";
        Random random = new Random();
        for (int i = 0; i < 10; ++i) {
            // Using % means the distribution isn't uniform, but that's OK.
            int position = random.nextInt(256) % seed.length();
            result += seed.charAt(position);
        }

        return result;
    }

    /**
     * Get the current time in milliseconds.
     * @return  The current time in milliseconds since 1/1/1970, including
     * fractions of a millisecond.
     */
    public static double
    getNowMilliseconds() { return (double)System.currentTimeMillis(); }

    // initial: push the JOIN message in to the messageCache_, update roster and
    // start the heartbeat.
    // (Do not call this. It is only public to implement the interface.)
    public final void
    onInitialized()
    {
        // Set the heartbeat timeout using the Interest timeout mechanism. The
        // heartbeat() function will call itself again after a timeout.
        // TODO: Are we sure using a "/local/timeout" interest is the best future call approach?
        Interest timeout = new Interest(new Name("/local/timeout"));
        timeout.setInterestLifetimeMilliseconds(60000);
        try {
            face.expressInterest(timeout, DummyOnData.onData, heartbeat);
        } catch (IOException ex) {
            Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        if (roster.indexOf(userName) < 0) {
            roster.add(userName);
            System.out.println("Member: " + screenName);
            System.out.println(screenName + ": Join");
            messageCacheAppend(ChatMessage.ChatMessageType.JOIN, "xxx");
        }
    }

    // sendInterest: Send a Chat Interest to fetch chat messages after the
    // user gets the Sync data packet back but will not send interest.
    // (Do not call this. It is only public to implement the interface.)
    public final void
    onReceivedSyncState(List syncStates, boolean isRecovery)
    {
        // This is used by onData to decide whether to display the chat messages.
        isRecoverySyncState = isRecovery;

        ArrayList sendList = new ArrayList(); // of String
        ArrayList sessionNoList = new ArrayList(); // of long
        ArrayList sequenceNoList = new ArrayList(); // of long
        for (int j = 0; j < syncStates.size(); ++j) {
            ChronoSync2013.SyncState syncState = (ChronoSync2013.SyncState)syncStates.get(j);
            Name nameComponents = new Name(syncState.getDataPrefix());
            String tempName = nameComponents.get(-1).toEscapedString();
            long sessionNo = syncState.getSessionNo();
            if (!tempName.equals(screenName)) {
                int index = -1;
                for (int k = 0; k < sendList.size(); ++k) {
                    if (((String)sendList.get(k)).equals(syncState.getDataPrefix())) {
                        index = k;
                        break;
                    }
                }
                if (index != -1) {
                    sessionNoList.set(index, sessionNo);
                    sequenceNoList.set(index, syncState.getSequenceNo());
                }
                else{
                    sendList.add(syncState.getDataPrefix());
                    sessionNoList.add(sessionNo);
                    sequenceNoList.add(syncState.getSequenceNo());
                }
            }
        }

        for (int i = 0; i < sendList.size(); ++i) {
            String uri = (String)sendList.get(i) + "/" + (long)sessionNoList.get(i) +
                    "/" + (long)sequenceNoList.get(i);
            Interest interest = new Interest(new Name(uri));
            interest.setInterestLifetimeMilliseconds(syncLifetime);
            try {
                face.expressInterest(interest, this, ChatTimeout.onTimeout);
            } catch (IOException ex) {
                Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
    }

    // Send back a Chat Data Packet which contains the user's message.
    // (Do not call this. It is only public to implement the interface.)
    public final void
    onInterest
    (Name prefix, Interest interest, Face face, long interestFilterId,
     InterestFilter filter)
    {
        ChatMessage.Builder builder = ChatMessage.newBuilder();
        long sequenceNo = Long.parseLong(interest.getName().get(chatPrefix.size() + 1).toEscapedString());
        boolean gotContent = false;
        for (int i = messageCache.size() - 1; i >= 0; --i) {
            CachedMessage message = (CachedMessage)messageCache.get(i);
            if (message.getSequenceNo() == sequenceNo) {
                if (!message.getMessageType().equals(ChatMessage.ChatMessageType.CHAT)) {
                    builder.setFrom(screenName);
                    builder.setTo(chatRoom);
                    builder.setType(message.getMessageType());
                    builder.setTimestamp((int)Math.round(message.getTime() / 1000.0));
                }
                else {
                    builder.setFrom(screenName);
                    builder.setTo(chatRoom);
                    builder.setType(message.getMessageType());
                    builder.setData(message.getMessage());
                    builder.setTimestamp((int)Math.round(message.getTime() / 1000.0));
                }
                gotContent = true;
                break;
            }
        }

        if (gotContent) {
            ChatMessage content = builder.build();
            byte[] array = content.toByteArray();
            Data data = new Data(interest.getName());
            data.setContent(new Blob(array, false));
            try {
                keyChain.sign(data, certificateName);
            } catch (SecurityException ex) {
                Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            try {
                face.putData(data);
            } catch (IOException ex) {
                Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // Process the incoming Chat data.
    // (Do not call this. It is only public to implement the interface.)
    public final void
    onData(Interest interest, Data data)
    {
        ChatMessage content;
        try {
            content = ChatMessage.parseFrom(data.getContent().getImmutableArray());
        } catch (InvalidProtocolBufferException ex) {
            Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        if (getNowMilliseconds() - content.getTimestamp() * 1000.0 < 120000.0) {
            String name = content.getFrom();
            String prefix = data.getName().getPrefix(-2).toUri();
            long sessionNo = Long.parseLong(data.getName().get(-2).toEscapedString());
            long sequenceNo = Long.parseLong(data.getName().get(-1).toEscapedString());
            String nameAndSession = name + sessionNo;

            int l = 0;
            //update roster
            while (l < roster.size()) {
                String entry = (String)roster.get(l);
                String tempName = entry.substring(0, entry.length() - 10);
                long tempSessionNo = Long.parseLong(entry.substring(entry.length() - 10));
                if (!name.equals(tempName) && !content.getType().equals(ChatMessage.ChatMessageType.LEAVE))
                    ++l;
                else {
                    if (name.equals(tempName) && sessionNo > tempSessionNo)
                        roster.set(l, nameAndSession);
                    break;
                }
            }

            if (l == roster.size()) {
                roster.add(nameAndSession);
                System.out.println(name + ": Join");
            }

            // Set the alive timeout using the Interest timeout mechanism.
            // TODO: Are we sure using a "/local/timeout" interest is the best future call approach?
            Interest timeout = new Interest(new Name("/local/timeout"));
            timeout.setInterestLifetimeMilliseconds(120000);
            try {
                face.expressInterest
                        (timeout, DummyOnData.onData,
                                this.new Alive(sequenceNo, name, sessionNo, prefix));
            } catch (IOException ex) {
                Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

            // isRecoverySyncState_ was set by sendInterest.
            // TODO: If isRecoverySyncState_ changed, this assumes that we won't get
            //   data from an interest sent before it changed.
            if (content.getType().equals(ChatMessage.ChatMessageType.CHAT) &&
                    !isRecoverySyncState && !content.getFrom().equals(screenName))
                System.out.println(content.getFrom() + ": " + content.getData());
            else if (content.getType().equals(ChatMessage.ChatMessageType.LEAVE)) {
                // leave message
                int n = roster.indexOf(nameAndSession);
                if (n >= 0 && !name.equals(screenName)) {
                    roster.remove(n);
                    System.out.println(name + ": Leave");
                }
            }
        }

    }


    /**
     * This repeatedly calls itself after a timeout to send a heartbeat message
     * (chat message type HELLO).
     * This method has an "interest" argument because we use it as the onTimeout
     * for Face.expressInterest.
     */
    private class Heartbeat implements OnTimeout {
        public final void onTimeout(Interest interest) {
            if (messageCache.size() == 0)
                messageCacheAppend(ChatMessage.ChatMessageType.JOIN, "xxx");
            try {
                sync.publishNextSequenceNo();
            } catch (IOException | SecurityException ex) {
                Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            messageCacheAppend(ChatMessage.ChatMessageType.HELLO, "xxx");

            // Call again.
            // TODO: Are we sure using a "/local/timeout" interest is the best future call approach?
            Interest timeout = new Interest(new Name("/local/timeout"));
            timeout.setInterestLifetimeMilliseconds(60000);
            try {
                face.expressInterest(timeout, DummyOnData.onData, heartbeat);
            } catch (IOException ex) {
                Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }


    /**
     * This is called after a timeout to check if the user with prefix has a newer
     * sequence number than the given temp_seq. If not, assume the user is idle and
     * remove from the roster and print a leave message.
     * This is used as the onTimeout for Face.expressInterest.
     */
    private class Alive implements OnTimeout {
        private final long tempSequenceNo;
        private final String name;
        private final long sessionNo;
        private final String prefix;

        public Alive(long tempSequenceNo, String name, long sessionNo, String prefix)
        {
            this.tempSequenceNo = tempSequenceNo;
            this.name = name;
            this.sessionNo = sessionNo;
            this.prefix = prefix;
        }

        public final void
        onTimeout(Interest interest)
        {
            long sequenceNo = sync.getProducerSequenceNo(prefix, sessionNo);
            String nameAndSession = name + sessionNo;
            int n = roster.indexOf(nameAndSession);
            if (sequenceNo != -1 && n >= 0) {
                if (tempSequenceNo == sequenceNo) {
                    roster.remove(n);
                    System.out.println(name + ": Leave");
                }
            }
        }


    }

}

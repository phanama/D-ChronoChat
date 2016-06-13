package id.ac.ui.clab.dchronochat;

import android.media.UnsupportedSchemeException;
import android.util.Log;

import id.ac.ui.clab.dchronochat.ChatbufProto.ChatMessage;

import com.google.protobuf.InvalidProtocolBufferException;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.sync.ChronoSync2013;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by yudiandreanp on 03/05/16.
 */


public class DChronoChat implements ChronoSync2013.OnInitialized, ChronoSync2013.OnReceivedSyncState, OnData, OnInterestCallback {

    private static final String LOG_TAG = "DChronoChat";
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
    private final boolean requireVerification;
    private Name identityName;
    private final ArrayList<HashMap<String, String>> mSequence = new ArrayList<HashMap<String, String>>();
    private ArrayList messageCache = new ArrayList(); // of CachedMessage
    private ArrayList<ChatMessage> chatMessageList = new ArrayList(); // of All ChatMessage
    private ArrayList roster = new ArrayList(); //TODO change from arraylist to map
    private final int maxMessageCacheLength = 250;
    private boolean isRecoverySyncState = true;
    private String tempName;
    private int session;
    private String mNameAndSession;

    private long prefixID;


    public DChronoChat(String screenName, String userName, String chatRoom, Name hubPrefix,
                       Face face, KeyChain keyChain, Name certificateName, boolean requireVerification)
    {
        Log.i(LOG_TAG, "DChronoChat Createed!");
        this.screenName = screenName; //The name on screen
        this.chatRoom = chatRoom; //chatroom name
        this.face = face;
        this.keyChain = keyChain;
        this.certificateName = certificateName;
        heartbeat = this.new Heartbeat();
        this.requireVerification = requireVerification;
        //this.userName = userName; //the email

        // This should only be called once, so get the random string here.

        session = (int)Math.round(getNowMilliseconds() / 1000.0);
        this.userName = screenName; // + session;
        this.mNameAndSession = this.userName + session;
        identityName = new Name(hubPrefix).append(this.userName); //identity to append to chatprefix
        // TODO see the effect of adding CHATCHANNEL and SESSION to chatPrefix
        chatPrefix = new Name(identityName).append(chatRoom).append(String.valueOf(session)); //the prefix of this chat

        try {
            sync = new ChronoSync2013
                    (this, this, chatPrefix,
                            new Name("/ndn/broadcast/ChronoChat").append(chatRoom), session,
                            face, keyChain, certificateName, syncLifetime, RegisterFailed.onRegisterFailed);
        } catch (IOException | SecurityException ex) {
            Log.e(LOG_TAG, "Exception : " + ex + " when creating new ChronoSync!");
            //Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        try {
            prefixID = face.registerPrefix(chatPrefix, this, RegisterFailed.onRegisterFailed);
            Log.i("Register:", chatPrefix.toString());
        } catch (IOException | SecurityException ex) {
            Log.e(LOG_TAG, "Exception : " + ex + " when registering prefix to face!");
            //Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
        }

        // TODO : Add persistent storage for chats
    }

    public void chatEnd() {
        face.removeRegisteredPrefix(prefixID);
        face.shutdown();
    }

    // Send a chat message by appending it to messagecache
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

            //add the chat message to view list
            ChatMessage.Builder builder = ChatMessage.newBuilder(); //builder to build chatmessage instance
            CachedMessage cachedMessage = (CachedMessage)messageCache.get(messageCache.size() - 1);
            builder.setFrom(screenName);
            builder.setTo(chatRoom);
            builder.setType(cachedMessage.getMessageType());
            builder.setData(cachedMessage.getMessage());
            builder.setTimestamp((int)Math.round(cachedMessage.getTime() / 1000.0));

            ChatMessage content = builder.build();
            chatMessageList.add(content);

            Log.i(LOG_TAG,screenName + ": " + chatMessage);
        }
    }

    // Send leave message and leave.
    public final void leave() throws IOException, SecurityException
    {
        sync.publishNextSequenceNo();
        messageCacheAppend(ChatMessage.ChatMessageType.LEAVE, "xxx");
        Log.i(LOG_TAG, "I am Leaving!");
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
        String seed = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM012345678avb9" + String.valueOf(getNowMilliseconds());
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

    // initial: push the JOIN message in to the messageCache, update roster and
    // start the heartbeat.
    // (Do not call this. It is only public to implement the interface.)
    public final void
    onInitialized()
    {
        // Set the heartbeat timeout using the Interest timeout mechanism. The
        // heartbeat() function will call itself again after a timeout.
        // TODO: Using a "/local/timeout" interest ,is this the best approach?
        Interest interestTimeout = new Interest(new Name("/local/timeout"));
        interestTimeout.setInterestLifetimeMilliseconds(60000);
        try 
        {
            face.expressInterest(interestTimeout, DummyOnData.onData, heartbeat);
        } 
        catch (IOException ex) 
        {
            Log.e(LOG_TAG, "Exception : " + ex + " when expressing Interest in onInitialized!");
            //Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        if (roster.indexOf(this.mNameAndSession) < 0) {
            join();
        }
    }

    //Send join message
    public void join()
    {
        roster.add(this.mNameAndSession);
        Log.i(LOG_TAG, "I am: " + screenName);
        Log.i(LOG_TAG, screenName + ": Join");
        messageCacheAppend(ChatMessage.ChatMessageType.JOIN, "xxx");

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(this.mNameAndSession, String.valueOf(0));
        mSequence.add(map);
    }

    // sendInterest: Send a Chat Interest to fetch chat messages after the
    // user gets the Sync data packet back but will not send interest.
    // (Do not call this. It is only public to implement the interface.)
    public final void
    onReceivedSyncState(List syncStates, boolean isRecovery)
    {
        // This is used by onData to decide whether to display the chat messages.
        isRecoverySyncState = isRecovery;
        Log.i(LOG_TAG, "Got sync state! | isRecovery state: " + String.valueOf(isRecovery));

        ArrayList sendList = new ArrayList(); // of String
        ArrayList<Long> sessionNoList = new ArrayList<>(); // of long
        ArrayList<Long> sequenceNoList = new ArrayList<>(); // of long
        for (int j = 0; j < syncStates.size(); ++j)
        {
            //see history
            ChronoSync2013.SyncState syncState = (ChronoSync2013.SyncState)syncStates.get(j);
            Name nameComponents = new Name(syncState.getDataPrefix());
            Log.i(LOG_TAG, "Full data name for index " + j + " : " + syncState.getDataPrefix() );
            //String tempSession = nameComponents.get(-1).toEscapedString();
            try {
                tempName = URLDecoder.decode(nameComponents.get(-3).toEscapedString(), "UTF-8");
                Log.i(LOG_TAG, "Sync state for data name: " + tempName);
                Log.i(LOG_TAG, "Current screenName: " + screenName);
            }
            catch (UnsupportedEncodingException e)
            {
                Log.e(LOG_TAG, "Encoding not supported");
            }
            //String tempFullName = tempName + tempSession;

            long sessionNo = syncState.getSessionNo();
            if (!tempName.equals(screenName))
            {
                int index = -1;
                for (int k = 0; k < sendList.size(); ++k) {
                    if ((sendList.get(k)).equals(syncState.getDataPrefix()))
                    {
                        index = k;
                        break;
                    }
                }
                if (index != -1)
                {
                    sessionNoList.set(index, sessionNo);
                    sequenceNoList.set(index, syncState.getSequenceNo());
                }
                else
                {
                    sendList.add(syncState.getDataPrefix());
                    sessionNoList.add(sessionNo);
                    sequenceNoList.add(syncState.getSequenceNo());
                }
            }
        }

        for (int i = 0; i < sendList.size(); ++i)
        {
            String uri = (String)sendList.get(i) + "/" +  (long)sequenceNoList.get(i);

            Log.i("Send URI :", "Send URI : " + uri + " | Sendlist uri : " + sendList.get(i) + " | "
                    + "Sequence no: " + sequenceNoList.get(i));

            //String uri = (String)sendList.get(i) + "/" + (long)sessionNoList.get(i) +
            //        "/" + (long)sequenceNoList.get(i);
            //Log.i("Send URI :", "Send URI : " + uri + " | Sendlist uri : " + sendList.get(i) + " | " + "Session : " +
            //        sessionNoList.get(i) + " | Sequence no: " + sequenceNoList.get(i));
            Interest interest = new Interest(new Name(uri));
            interest.setInterestLifetimeMilliseconds(syncLifetime);
            try {
                face.expressInterest(interest, this, ChatTimeout.onTimeout);
                Log.i(LOG_TAG, "Sent Interest : " + interest.getName());
            }
            catch (IOException ex)
            {
                Log.e(LOG_TAG, "Exception : " + ex + " when expressing Interest on Sync!");
                //Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
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
        Log.i("OnInterest", "Got New Interest!");
        Log.i("OnInterest", interest.toUri());
        ChatMessage.Builder builder = ChatMessage.newBuilder(); //builder to build chatmessage instance
        Log.i(LOG_TAG, "Name : " + interest.getName() + " | Chatprefix : " + chatPrefix);
        long sequenceNo = Long.parseLong(interest.getName().get(chatPrefix.size()).toEscapedString());

        Log.i(LOG_TAG, "Sequence Number : " + sequenceNo);

        boolean gotContent = false;

        //Find the right chat message in messageCache
        for (int i = messageCache.size() - 1; i >= 0; --i)
        {
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

        //forge the Chat into Data
        if (gotContent) {
            ChatMessage content = builder.build();
            byte[] array = content.toByteArray();
            Data data = new Data(interest.getName());
            data.setContent(new Blob(array, false));
            try {
                keyChain.sign(data, certificateName);
            } catch (SecurityException ex) {
                Log.e(LOG_TAG, "Security Exception : " + ex + " when signing Chat Data!");
                //Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            try {
                face.putData(data);
                Log.i(LOG_TAG, "Sent Data : " + data.getName());
            } catch (IOException ex) {
                Log.e(LOG_TAG, "IOException : " + ex + " when putting Chat Data to Face!");
                //Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // Process the incoming Chat data.
    // (Do not call this. It is only public to implement the interface.)
    public final void
    onData(Interest interest, Data data)
    {
        Log.i(LOG_TAG, "onData:" + data.getContent().toString());

        ChatMessage content;
        //parse data into Chat Message
        try
        {
            content = ChatMessage.parseFrom(data.getContent().getImmutableArray());
            Log.i("Content:", content.getData());
        }
        catch (InvalidProtocolBufferException ex)
        {
            Log.e(LOG_TAG, "Exception : " + ex + " when parsing ChatMessage from Data!");
            //Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        //using 180secs difference
        if (getNowMilliseconds() - content.getTimestamp() * 1000.0 < 180000.0) {
            String name = content.getFrom(); //get sender's screenName
            String prefix = data.getName().getPrefix(-2).toUri(); //get sender's prefix
            long sessionNo = Long.parseLong(data.getName().get(-2).toEscapedString());
            long sequenceNo = Long.parseLong(data.getName().get(-1).toEscapedString());
            Log.i("Content details: " , "name: " + name
                    + " | prefix : " + prefix + " | sessionNo: " + sessionNo +
                    " | sequenceNo: " + sequenceNo + " | MessageType: " + content.getType().toString() +
                     " | Data: " + content.getData());
            String nameAndSession = name + sessionNo;
            int length = 0;

            //check roster
            while (length < roster.size())
            {
                String entry = (String)roster.get(length);
                String tempName = entry.substring(0, entry.length()-10);
                long tempSessionNo = Long.parseLong(entry.substring(entry.length() - 10));
                Log.i("Check Rosterr:", "roster[" + length + "] | tempname:" + tempName + " | tempsession:" + tempSessionNo);
                if (!name.equals(tempName) && !content.getType().equals(ChatMessage.ChatMessageType.LEAVE))
                {
                    ++length;
                }

                //bugfix for broadcasted HELLO message from self
                else if (name.equals(tempName) && content.getType().equals(ChatMessage.ChatMessageType.HELLO))
                {
                    Log.i(LOG_TAG, "Roster | " + tempName + " | " + content.getType().toString());
                    break;
                }

                else
                {
                    if (name.equals(tempName) && sessionNo > tempSessionNo)
                        {
                            roster.set(length, nameAndSession);
                            Log.i("Roster", "Updated roster!" + tempName+sessionNo);
                        }
                    break;
                }
            }

            if (length == roster.size()) {
                roster.add(nameAndSession);
                Log.i(LOG_TAG, name + ": Join");
            }

            //update the sequence_
            boolean isPrint = true;
            int i;
            for (i=0; i < mSequence.size(); ++i)
            {

                HashMap<String, String> map = mSequence.get(i);
                if (map.containsKey(nameAndSession))
                {

                    String mapSequence = map.get(nameAndSession);
                    Log.i("Map Debug:", "Map Debug: " + map.toString() + " contains key: " + nameAndSession + " | Map Sequence: "
                    + mapSequence);

                    //if (mapSequence.compareTo(String.valueOf(sequenceNo)) >= 0)
                    //fix bug if sequence reaches 10
                    if (Long.valueOf(mapSequence) >
                            sequenceNo)
                    {

                        isPrint = false;
                        Log.i(LOG_TAG, "isPrint : is false!" );
                        break;
                    }

                    HashMap<String, String> tmpMap = new HashMap<String, String>();
                    tmpMap.put(nameAndSession, String.valueOf(sequenceNo));
                    mSequence.set(i, tmpMap);
                        Log.i(LOG_TAG, "tmpMap: " + tmpMap.toString());
                }
            }
            if (i >= mSequence.size()) {
                HashMap<String, String> tmpMap = new HashMap<String, String>();
                tmpMap.put(nameAndSession, String.valueOf(sequenceNo));
                mSequence.add(tmpMap);
                Log.i(LOG_TAG, "sequence_ add new item: " + tmpMap);
            }


            // Set the alive timeout using the Interest timeout mechanism.
            // TODO: Using a "/local/timeout" interest ,is this the best approach?
            Interest timeout = new Interest(new Name("/local/timeout"));
            timeout.setInterestLifetimeMilliseconds(120000);
            try {
                face.expressInterest
                        (timeout, DummyOnData.onData,
                                this.new Alive(sequenceNo, name, sessionNo, prefix));
            } catch (IOException ex) {
                Log.e(LOG_TAG, "IOException when onData!");
                //Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }

            // isRecoverySyncState_ was set by sendInterest.
            // TODO: If isRecoverySyncState_ changed, this assumes that we won't get data from an interest sent before it changed.
            if (isPrint && content.getType().equals(ChatbufProto.ChatMessage.ChatMessageType.CHAT) &&
                    sessionNo != session) {
                Log.i("Content from...", content.getFrom() + ": " + content.getData());
                chatMessageList.add(content);
            }

            else if (content.getType().equals(ChatMessage.ChatMessageType.LEAVE)) {
                // leave message
                int n = roster.indexOf(nameAndSession);
                if (n >= 0 && !name.equals(screenName))
                {
                    roster.remove(n);
                    Log.i(LOG_TAG, name + ": Leave");

                }

                for (int j = 0; j < mSequence.size(); ++ j) {
                    HashMap<String, String> map = mSequence.get(j);
                    if (map.containsKey(nameAndSession) && !nameAndSession.contains(screenName)) {
                        mSequence.remove(i);
                        Log.i(LOG_TAG, "remove sequence_ item: " + map.toString());
                        break;
                    }
                }
            }
        }
    }

    private static class RegisterFailed implements OnRegisterFailed {
        public final void
        onRegisterFailed(Name prefix)
        {
            Log.i(LOG_TAG, "Register failed for prefix " + prefix.toUri());
        }

        public final static OnRegisterFailed onRegisterFailed = new RegisterFailed();
    }

    // This is a do-nothing onData for using expressInterest for timeouts.
    // This should never be called.
    private static class DummyOnData implements OnData {
        public final void
        onData(Interest interest, Data data) {}

        public final static OnData onData = new DummyOnData();
    }

    private static class ChatTimeout implements OnTimeout {
        public final void
        onTimeout(Interest interest) {

            System.out.println("Timeout waiting for chat data");
        }

        public final static OnTimeout onTimeout = new ChatTimeout();
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
                Log.v("Hearbeat", "Published new sequence");
            } catch (IOException | SecurityException ex) {
                Log.e(LOG_TAG, "Exception : " + ex + " when publishing new sequence!");
                //Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            messageCacheAppend(ChatMessage.ChatMessageType.HELLO, "xxx");


            // TODO: Are we sure using a "/local/timeout" interest is the best future call approach?
            Interest timeout = new Interest(new Name("/local/timeout"));
            timeout.setInterestLifetimeMilliseconds(60000);
            try {
                face.expressInterest(timeout, DummyOnData.onData, heartbeat);
                Log.i("Hearbeat", "Create heartbeat again");
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Exception : " + ex + " when expressing Heartbeat!");
                //Logger.getLogger(DChronoChat.class.getName()).log(Level.SEVERE, null, ex);
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
                    Log.i(LOG_TAG, name + ": Leave");
                }
            }

            for (int i = 0; i < mSequence.size(); ++ i) {
                HashMap<String, String> map = mSequence.get(i);
                if (map.containsKey(nameAndSession) && map.get(nameAndSession).equals(sequenceNo)) {
                    mSequence.remove(i);
                    //sendDebugMsg("  remove the sequence_ item: " + map.toString());
                    break;
                }
            }
        }


    }

    public ArrayList getMessageCache()
    {
        return messageCache;
    }

    public ArrayList<ChatMessage> getChatList()
    {
        return chatMessageList;
    }

}

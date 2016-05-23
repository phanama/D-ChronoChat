package id.ac.ui.clab.dchronochat;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import id.ac.ui.clab.dchronochat.ChatbufProto.ChatMessage;

/**
 * Created by yudiandreanp on 21/05/16.
 */
public class ChatListFragment extends Fragment {
    private List<ChatMessage> values;
    private String screenName;
    private String hubPrefix;
    private String userName;
    private String chatRoom;


    public static ChatListFragment newInstance(String screenName, String userName, String hubPrefix, String chatRoom) {
        ChatListFragment fragment = new ChatListFragment();

        //Get the argument from MainActivity calling this fragment
        Bundle args = new Bundle();
        args.putString("screenName", screenName);
        args.putString("userName", userName);
        args.putString("hubPrefix", hubPrefix);
        args.putString("chatRoom", chatRoom);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Get rage face names and descriptions.
        final Resources resources = context.getResources();
        mNames = resources.getStringArray(R.array.names);
        mDescriptions = resources.getStringArray(R.array.descriptions);
        mUrls = resources.getStringArray(R.array.urls);

        // Get rage face images.
        final TypedArray typedArray = resources.obtainTypedArray(R.array.images);
        final int imageCount = mNames.length;
        mImageResIds = new int[imageCount];
        for (int i = 0; i < imageCount; i++) {
            mImageResIds[i] = typedArray.getResourceId(i, 0);
        }
        typedArray.recycle();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        final Activity activity = getActivity();
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.listItemChat);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(new ChatAdapter(this.getActivity(), values));
        return view;
    }

    private class ChatHolder extends ViewHolder {
        // Views
        private View mUserPresence;
        private TextView mScreenName;
        private TextView mChatTime;
        private TextView mMessage;
        public ChatMessage mChatMessage;

        public ChatHolder(View itemView) {
            super(itemView);

            // Get references to image and name.
            mUserPresence = (View) itemView.findViewById(R.id.user_presence);
            mScreenName = (TextView) itemView.findViewById(R.id.screenName);
            mChatTime = (TextView) itemView.findViewById(R.id.chat_time);
            mMessage = (TextView) itemView.findViewById(R.id.chat_message);
        }

        public void bindData(ChatMessage chatMessage) {
            mChatMessage = chatMessage;
            mScreenName.setText(chatMessage.getFrom());
            mChatTime.setText(chatMessage.getTimestamp());
            mMessage.setText(chatMessage.getData());
        }
    }

    public class ChatAdapter extends RecyclerView.Adapter<ChatHolder> {
        private final Context context;
        private LayoutInflater inflater;
        private List<ChatMessage> values;
        private Set<String> onlineNow = new HashSet<String>();

        public ChatAdapter(Context context, List<ChatMessage> values) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
            this.values=values;
        }

        public ChatHolder onCreateViewHolder(ViewGroup parent, int pos) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_chat, parent, false);
            return new ChatHolder(view);
        }

        @Override
        public void onBindViewHolder(ChatHolder holder, int pos) {
            ChatMessage chat = values.get(pos);
            holder.bindData(chat);
        }

        @Override
        public int getItemCount() {
            return values.size();
        }


        //TODO Clean up these mess
        class ViewHolder {
            TextView user;
            TextView message;
            TextView timeStamp;
            View userPresence;
            ChatMessage chatMsg;
        }


        public View getView(final int position, View convertView, ViewGroup parent) {
            ChatMessage chatMsg = this.values.get(position);
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.list_item_chat, parent, false);
                holder.user = (TextView) convertView.findViewById(R.id.screenName);
                holder.message = (TextView) convertView.findViewById(R.id.chat_message);
                holder.timeStamp = (TextView) convertView.findViewById(R.id.chat_time);
                holder.userPresence = convertView.findViewById(R.id.user_presence);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.user.setText(chatMsg.getFrom());
            holder.message.setText(chatMsg.getData());
            holder.timeStamp.setText(formatTimeStamp(chatMsg.getTimestamp()));
            holder.chatMsg=chatMsg;
            holder.userPresence.setBackgroundDrawable( // If online show the green presence dot
                    this.onlineNow.contains(chatMsg.getFrom())
                            ? context.getResources().getDrawable(R.drawable.online_circle)
                            : null);
            return convertView;
        }


        /**
         * Method to add a single message and update the listview.
         * @param chatMsg Message to be added
         */
        public void addMessage(ChatMessage chatMsg){
            this.values.add(chatMsg);
            notifyDataSetChanged();
        }

        /**
         * Method to add a list of messages and update the listview.
         * @param chatMsgs Messages to be added
         */
        public void setMessages(List<ChatMessage> chatMsgs){
            this.values.clear();
            this.values.addAll(chatMsgs);
            notifyDataSetChanged();
        }

        /**
         * Handle users. Fill the onlineNow set with current users. Data is used to display a green dot
         *   next to users who are currently online.
         * @param user UUID of the user online.
         * @param action The presence action
         */
        public void userPresence(String user, String action){
            boolean isOnline = action.equals("join") || action.equals("state-change");
            if (!isOnline && this.onlineNow.contains(user))
                this.onlineNow.remove(user);
            else if (isOnline && !this.onlineNow.contains(user))
                this.onlineNow.add(user);

            notifyDataSetChanged();
        }

        /**
         * Overwrite the onlineNow array with all the values attained from a call to hereNow().
         * @param onlineNow
         */
        public void setOnlineNow(Set<String> onlineNow){
            this.onlineNow = onlineNow;
            notifyDataSetChanged();
        }

        /**
         * Format the long System.currentTimeMillis() to a better looking timestamp. Uses a calendar
         *   object to format with the user's current time zone.
         * @param timeStamp
         * @return
         */
        public String formatTimeStamp(long timeStamp){
            // Create a DateFormatter object for displaying date in specified format.
            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");

            // Create a calendar object that will convert the date and time value in milliseconds to date.
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeStamp);
            return formatter.format(calendar.getTime());
        }

        /**
         * Clear all values from the values array and update the listview. Used when changing rooms.
         */
        public void clearMessages(){
            this.values.clear();
            notifyDataSetChanged();
        }

    }

}


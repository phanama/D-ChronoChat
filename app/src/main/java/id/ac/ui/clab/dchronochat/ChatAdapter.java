package id.ac.ui.clab.dchronochat;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import id.ac.ui.clab.dchronochat.ChatListFragment.ChatHolder;
/**
 * Created by yudiandreanp on 25/05/16.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatHolder> {
    private final Context context;
    private LayoutInflater inflater;
    private List<ChatbufProto.ChatMessage> mMessageList;
    private Set<String> onlineNow = new HashSet<String>();

    public ChatAdapter(Context context, List<ChatbufProto.ChatMessage> messageList) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.mMessageList = messageList;
    }

    public ChatHolder onCreateViewHolder(ViewGroup parent, int pos) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View chatView = inflater.inflate(R.layout.list_item_chat, parent, false);
        ChatHolder chatHolder = new ChatHolder(chatView);

        return chatHolder;
    }

    @Override
    public void onBindViewHolder(ChatHolder holder, int pos) {
        ChatbufProto.ChatMessage chat = mMessageList.get(pos);
        holder.bindData(chat);
    }

    @Override
    public int getItemCount()
    {
        return mMessageList.size();
    }

    public void refreshList(List<ChatbufProto.ChatMessage> messageList){
        this.mMessageList = messageList;
        notifyDataSetChanged();
    }

}
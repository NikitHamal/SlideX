package com.slides.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private EditText chatInput;
    private ImageButton sendButton;
    
    private ChatInteractionListener chatInteractionListener;

    public interface ChatInteractionListener {
        void onChatPromptSent(String prompt);
    }

    public void setChatInteractionListener(ChatInteractionListener listener) {
        this.chatInteractionListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        chatInput = view.findViewById(R.id.chat_input);
        sendButton = view.findViewById(R.id.send_button);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        sendButton.setOnClickListener(v -> {
            String message = chatInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                chatInput.setText("");
            }
        });

        // Add welcome message
        addWelcomeMessage();

        return view;
    }

    private void addWelcomeMessage() {
        addAiMessage("Hello! I'm here to help you create amazing presentations. Just describe what kind of slide you want and I'll generate it for you!\n\nFor example, try:\n• \"Create a slide about renewable energy\"\n• \"Make a presentation slide for our company overview\"\n• \"Generate a slide about machine learning basics\"");
    }

    private void sendMessage(String message) {
        // Add user message to chat
        addUserMessage(message);
        
        // Show typing indicator
        addAiMessage("Creating your slide...");
        
        // Send to parent activity for processing
        if (chatInteractionListener != null) {
            chatInteractionListener.onChatPromptSent(message);
        }
    }

    private void addUserMessage(String message) {
        chatMessages.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    public void addAiMessage(String message) {
        // Remove the last "Creating your slide..." message if it exists
        if (!chatMessages.isEmpty()) {
            ChatMessage lastMessage = chatMessages.get(chatMessages.size() - 1);
            if (!lastMessage.isUser() && lastMessage.getText().equals("Creating your slide...")) {
                chatMessages.remove(chatMessages.size() - 1);
                chatAdapter.notifyItemRemoved(chatMessages.size());
            }
        }
        
        chatMessages.add(new ChatMessage(message, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    public void addAiResponse(String response) {
        addAiMessage(response);
    }
}

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
                addUserMessage(message);
                chatInput.setText("");
                // TODO: Add logic to send the message to the AI
            }
        });

        return view;
    }

    private void addUserMessage(String message) {
        chatMessages.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }

    private void addAiMessage(String message) {
        chatMessages.add(new ChatMessage(message, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
}

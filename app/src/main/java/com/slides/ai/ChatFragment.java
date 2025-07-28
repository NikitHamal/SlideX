package com.slides.ai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.slides.ai.qwen.QwenManager;
import java.util.concurrent.Executors;
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
    private Button modelSelectorButton;
    private String selectedModel = "Gemini-2.0-Flash";
    private String selectedQwenModel = "qwen3-235b-a22b";
    private QwenManager qwenManager;
    // Qwen models (hardcoded from qwen_thinking_search.txt)
    private static final String[] QWEN_MODELS = {
        "qwen3-235b-a22b",
        "qwen3-coder-plus",
        "qwen3-30b-a3b",
        "qwen3-32b",
        "qwen-max-latest",
        "qwen-plus-2025-01-25",
        "qwq-32b",
        "qwen-turbo-2025-02-11",
        "qwen2.5-omni-7b",
        "qvq-72b-preview-0310",
        "qwen2.5-vl-32b-instruct",
        "qwen2.5-14b-instruct-1m",
        "qwen2.5-coder-32b-instruct",
        "qwen2.5-72b-instruct"
    };
    
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
        modelSelectorButton = view.findViewById(R.id.model_selector_button);

        modelSelectorButton.setOnClickListener(v -> showModelSelectionDialog());

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

        qwenManager = new QwenManager(new ApiKeyManager(getContext()), new android.os.Handler(), Executors.newSingleThreadExecutor());

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
        if (selectedModel.startsWith("Qwen")) {
            qwenManager.createNewChat(new QwenManager.QwenCallback<com.slides.ai.qwen.QwenNewChatResponse>() {
                @Override
                public void onSuccess(com.slides.ai.qwen.QwenNewChatResponse response) {
                    if (response.success) {
                        qwenManager.getCompletionStreaming(response.data.id, message, selectedQwenModel, new QwenManager.QwenStreamingCallback() {
                            @Override
                            public void onStream(String partial) {
                                // Append streaming response
                                updateLastAiMessage(partial);
                            }
                            @Override
                            public void onComplete(String full) {
                                updateLastAiMessage(full);
                            }
                            @Override
                            public void onError(String error) {
                                addAiResponse("Error: " + error);
                            }
                        });
                    } else {
                        addAiResponse("Error creating new chat.");
                    }
                }
                @Override
                public void onError(String error) {
                    addAiResponse("Error: " + error);
                }
            }, selectedQwenModel);
        } else {
            // Send to parent activity for processing
            if (chatInteractionListener != null) {
                chatInteractionListener.onChatPromptSent(message);
            }
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

    private void showModelSelectionDialog() {
        final String[] models = new String[QWEN_MODELS.length + 1];
        models[0] = "Gemini-2.0-Flash";
        for (int i = 0; i < QWEN_MODELS.length; i++) {
            models[i + 1] = "Qwen: " + QWEN_MODELS[i];
        }
        int checkedItem = 0;
        if (!selectedModel.equals("Gemini-2.0-Flash")) {
            for (int i = 0; i < QWEN_MODELS.length; i++) {
                if (selectedQwenModel.equals(QWEN_MODELS[i])) {
                    checkedItem = i + 1;
                    break;
                }
            }
        }
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Select Model")
                .setSingleChoiceItems(models, checkedItem, (dialog, which) -> {
                    if (which == 0) {
                        selectedModel = "Gemini-2.0-Flash";
                        modelSelectorButton.setText("Gemini-2.0-Flash");
                    } else {
                        selectedModel = "Qwen: " + QWEN_MODELS[which - 1];
                        selectedQwenModel = QWEN_MODELS[which - 1];
                        modelSelectorButton.setText(selectedQwenModel);
                    }
                    dialog.dismiss();
                })
                .show();
    }
    // Add this method to update the last AI message (for streaming)
    private void updateLastAiMessage(String message) {
        if (!chatMessages.isEmpty()) {
            ChatMessage lastMessage = chatMessages.get(chatMessages.size() - 1);
            if (!lastMessage.isUser()) {
                lastMessage.setText(message);
                chatAdapter.notifyItemChanged(chatMessages.size() - 1);
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        }
    }
}

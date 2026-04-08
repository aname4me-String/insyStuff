package at.insystuff.rag.websocket;

import at.insystuff.rag.conversation.ConversationService;
import at.insystuff.rag.query.QueryService;
import at.insystuff.rag.statistics.BenchmarkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ConversationService conversationService;
    private final QueryService queryService;
    private final ObjectMapper objectMapper;
    private final BenchmarkService benchmarkService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Map<?, ?> payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (IOException e) {
            sendError(session, "Invalid JSON payload");
            return;
        }

        Object chatIdRaw = payload.get("chatId");
        Object questionRaw = payload.get("question");
        Object modelRaw = payload.get("model");

        if (chatIdRaw == null || questionRaw == null) {
            sendError(session, "chatId and question are required");
            return;
        }

        long chatId;
        try {
            chatId = ((Number) chatIdRaw).longValue();
        } catch (ClassCastException e) {
            sendError(session, "chatId must be a number");
            return;
        }

        String question = questionRaw.toString().trim();
        String model = modelRaw != null ? modelRaw.toString() : null;

        if (question.isBlank()) {
            sendError(session, "question must not be blank");
            return;
        }

        // Persist the user message and update chat title
        conversationService.saveUserMessage(chatId, question);

        // Retrieve context + token stream
        long requestStart = System.currentTimeMillis();
        QueryService.StreamContext ctx = queryService.streamAnswer(question, model);

        // Stream tokens to the client.
        // We intentionally use toIterable() (blocking iteration) because this handler
        // runs on a Tomcat servlet thread where blocking is the correct pattern.
        // Each token is forwarded immediately, giving the client live streaming output.
        StringBuilder fullAnswer = new StringBuilder();
        int tokenCount = 0;
        try {
            for (String token : ctx.tokenStream().toIterable()) {
                fullAnswer.append(token);
                tokenCount++;
                sendJson(session, Map.of("type", "token", "content", token));
            }
        } catch (Exception e) {
            log.error("Error while streaming tokens for chat {}", chatId, e);
            sendError(session, "Streaming failed: " + e.getMessage());
            return;
        }
        long totalResponseMs = System.currentTimeMillis() - requestStart;

        // Persist the assistant message
        String modelLabel = (model != null && !model.isBlank()) ? model : "default";
        ConversationService.ChatMessageDto saved = conversationService.saveAssistantMessage(
                chatId, fullAnswer.toString(), modelLabel, ctx.sources());

        // Record benchmark
        benchmarkService.record(question, modelLabel,
                ctx.vectorSearchMs(), totalResponseMs,
                tokenCount, ctx.sources().size());

        // Send the completion event with sources and persisted message id
        sendJson(session, Map.of(
                "type", "done",
                "messageId", saved.id(),
                "sources", ctx.sources()
        ));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.debug("WebSocket connection closed: {} status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
    }

    private void sendJson(WebSocketSession session, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.error("Failed to send WebSocket message", e);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        sendJson(session, Map.of("type", "error", "message", message));
    }
}

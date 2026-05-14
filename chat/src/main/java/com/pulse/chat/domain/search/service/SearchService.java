package com.pulse.chat.domain.search.service;

import com.pulse.chat.domain.chat_core.repository.MessageRepository;
import com.pulse.chat.domain.search.dto.MessageSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class SearchService {
    private final MessageRepository messageRepository;

    public Page<MessageSearchResult> searchMessages(UUID userId,
                                                    String keyword,
                                                    UUID conversationId,
                                                    UUID senderId,
                                                    Instant fromTime,
                                                    Instant toTime,
                                                    int page,
                                                    int size) {
        return messageRepository.searchByKeyword(
                        userId,
                        keyword,
                        conversationId,
                        senderId,
                        fromTime,
                        toTime,
                        PageRequest.of(page, size)
                )
                .map(m -> new MessageSearchResult(m.getId(), m.getConversationId(), m.getSenderId(), m.getContent(), m.getCreatedAt()));
    }
}

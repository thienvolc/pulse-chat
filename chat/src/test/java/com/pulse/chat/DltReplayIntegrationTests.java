package com.pulse.chat;

import com.pulse.chat.domain.events.dlt.DltCaptureService;
import com.pulse.chat.domain.events.dlt.DltEventRepository;
import com.pulse.chat.domain.events.dlt.DltEventStatus;
import com.pulse.chat.domain.events.dlt.DltReplayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class DltReplayIntegrationTests {

    @Autowired
    private DltCaptureService dltCaptureService;

    @Autowired
    private DltEventRepository dltEventRepository;

    @Autowired
    private DltReplayService dltReplayService;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void dltArrival_thenReplay_success() {
        dltCaptureService.capture("chat.messages.created.dlt", "conv-1", "{\"messageId\":\"m-1\"}");

        var summary = dltReplayService.replayPending(10);

        assertThat(summary.processed()).isEqualTo(1);
        assertThat(summary.replayed()).isEqualTo(1);
        assertThat(summary.failed()).isEqualTo(0);
        assertThat(dltEventRepository.countByStatus(DltEventStatus.REPLAYED)).isEqualTo(1);
        verify(kafkaTemplate).send(anyString(), anyString(), anyString());
    }

    @Test
    void replayFailure_marksFailed_then_secondReplay_doesNotReprocessFailed() {
        dltCaptureService.capture("chat.messages.created.dlt", "conv-2", "{\"messageId\":\"m-2\"}");
        doThrow(new IllegalStateException("kafka-down")).when(kafkaTemplate).send(anyString(), anyString(), anyString());

        var first = dltReplayService.replayPending(10);
        assertThat(first.processed()).isEqualTo(1);
        assertThat(first.failed()).isEqualTo(1);
        assertThat(dltEventRepository.countByStatus(DltEventStatus.FAILED)).isEqualTo(1);

        reset(kafkaTemplate);
        var second = dltReplayService.replayPending(10);
        assertThat(second.processed()).isEqualTo(0);
        assertThat(second.replayed()).isEqualTo(0);
    }
}

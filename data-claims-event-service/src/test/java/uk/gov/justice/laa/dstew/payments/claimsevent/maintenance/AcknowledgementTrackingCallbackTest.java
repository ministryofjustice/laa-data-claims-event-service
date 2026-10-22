package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.GenericMessage;

@ExtendWith(MockitoExtension.class)
@DisplayName("AcknowledgementTrackingCallback")
class AcknowledgementTrackingCallbackTest {

  @Mock ListenerActivityTracker activityTracker;

  @Test
  @DisplayName("Decrements the in-flight count by the batch size on acknowledgement success")
  void onSuccessDecrementsByBatchSize() {
    AcknowledgementTrackingCallback callback = new AcknowledgementTrackingCallback(activityTracker);

    callback.onSuccess(List.of(new GenericMessage<>("a"), new GenericMessage<>("b")));

    verify(activityTracker).decrement(2);
    verifyNoMoreInteractions(activityTracker);
  }

  @Test
  @DisplayName("Still decrements the in-flight count by the batch size on acknowledgement failure")
  void onFailureDecrementsByBatchSize() {
    AcknowledgementTrackingCallback callback = new AcknowledgementTrackingCallback(activityTracker);

    callback.onFailure(List.of(new GenericMessage<>("a")), new RuntimeException("boom"));

    verify(activityTracker).decrement(1);
    verifyNoMoreInteractions(activityTracker);
  }
}

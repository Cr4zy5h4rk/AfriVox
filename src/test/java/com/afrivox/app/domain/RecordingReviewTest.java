package com.afrivox.app.domain;

import static com.afrivox.app.domain.RecordingReviewTestSamples.*;
import static com.afrivox.app.domain.RecordingTestSamples.*;
import static com.afrivox.app.domain.UserProfileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.afrivox.app.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RecordingReviewTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RecordingReview.class);
        RecordingReview recordingReview1 = getRecordingReviewSample1();
        RecordingReview recordingReview2 = new RecordingReview();
        assertThat(recordingReview1).isNotEqualTo(recordingReview2);

        recordingReview2.setId(recordingReview1.getId());
        assertThat(recordingReview1).isEqualTo(recordingReview2);

        recordingReview2 = getRecordingReviewSample2();
        assertThat(recordingReview1).isNotEqualTo(recordingReview2);
    }

    @Test
    void recordingTest() {
        RecordingReview recordingReview = getRecordingReviewRandomSampleGenerator();
        Recording recordingBack = getRecordingRandomSampleGenerator();

        recordingReview.setRecording(recordingBack);
        assertThat(recordingReview.getRecording()).isEqualTo(recordingBack);

        recordingReview.recording(null);
        assertThat(recordingReview.getRecording()).isNull();
    }

    @Test
    void reviewerTest() {
        RecordingReview recordingReview = getRecordingReviewRandomSampleGenerator();
        UserProfile userProfileBack = getUserProfileRandomSampleGenerator();

        recordingReview.setReviewer(userProfileBack);
        assertThat(recordingReview.getReviewer()).isEqualTo(userProfileBack);

        recordingReview.reviewer(null);
        assertThat(recordingReview.getReviewer()).isNull();
    }
}

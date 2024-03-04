package com.artur.youtback.listener;

import com.artur.youtback.mediator.ProcessingEventMediator;
import com.artur.youtback.utils.AppConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;

@Component
public class ProcessingEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProcessingEventHandler.class);

    @Autowired
    ProcessingEventMediator processingEventMediator;

    @KafkaListener(
            topics = AppConstants.VIDEO_OUTPUT_TOPIC,
            groupId = "yout-back.video:consumer",
            containerFactory = "resultListenerFactory"
    )
    public void listenVideoEvent(ConsumerRecord<String, Boolean> record) throws BrokenBarrierException, InterruptedException, TimeoutException {
        logger.trace("Received message from leader [key: " + record.key() + "; value " + record.value() + "], topic: " + AppConstants.VIDEO_OUTPUT_TOPIC);
       processingEventMediator.videoProcessingResultNotice(record.key(), record.value());
    }

    @KafkaListener(
            topics = AppConstants.THUMBNAIL_OUTPUT_TOPIC,
            groupId = "yout-back.video:consumer",
            containerFactory = "resultListenerFactory"
    )
    public void listenThumbnailEvent(ConsumerRecord<String, Boolean> record) throws BrokenBarrierException, InterruptedException, TimeoutException {
        logger.trace("Received message from leader [key: " + record.key() + "; value " + record.value() + "], topic: " + AppConstants.VIDEO_OUTPUT_TOPIC);
        processingEventMediator.thumbnailProcessingResultNotice(record.key(), record.value());
    }

    @KafkaListener(
            topics = AppConstants.USER_PICTURE_OUTPUT_TOPIC,
            groupId = "yout-back.video:consumer",
            containerFactory = "resultListenerFactory"
    )
    public void listenUserPictureEvent(ConsumerRecord<String, Boolean> record) throws BrokenBarrierException, InterruptedException, TimeoutException {
        logger.trace("Received message from leader [key: " + record.key() + "; value " + record.value() + "], topic: " + AppConstants.VIDEO_OUTPUT_TOPIC);
        processingEventMediator.userPictureProcessingResultNotice(record.key(), record.value());
    }
}

package com.artur.youtback.mediator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class ProcessingEventMediator {
    private static final Logger logger = LoggerFactory.getLogger(ProcessingEventMediator.class);

    private boolean videoEventResult = false;
    private boolean thumbnailEventResult = false;
    private boolean userPictureEventResult = false;

    private final Map<String, CyclicBarrier> videoCyclicBarrier = new ConcurrentHashMap<>();
    private final Map<String, CyclicBarrier> thumbnailCyclicBarrier = new ConcurrentHashMap<>();
    private final Map<String, CyclicBarrier> userPictureCyclicBarrier = new ConcurrentHashMap<>();


    public boolean videoProcessingWait(String id) throws BrokenBarrierException, InterruptedException, TimeoutException {
        videoCyclicBarrier.put(id, new CyclicBarrier(2));
        videoCyclicBarrier.get(id).await(90, TimeUnit.SECONDS);
        return videoEventResult;
    }

    public void videoProcessingResultNotice(String id, boolean value) throws BrokenBarrierException, InterruptedException, TimeoutException {
        videoEventResult = value;
        videoCyclicBarrier.get(id).await(90, TimeUnit.SECONDS);
        videoCyclicBarrier.remove(id);
    }

    public boolean thumbnailProcessingWait(String id) throws BrokenBarrierException, InterruptedException, TimeoutException {
        thumbnailCyclicBarrier.put(id, new CyclicBarrier(2));
        thumbnailCyclicBarrier.get(id).await(90, TimeUnit.SECONDS);
        return thumbnailEventResult;
    }

    public void thumbnailProcessingResultNotice(String id, boolean value) throws BrokenBarrierException, InterruptedException, TimeoutException {
        thumbnailEventResult = value;
        thumbnailCyclicBarrier.get(id).await(90, TimeUnit.SECONDS);
        thumbnailCyclicBarrier.remove(id);
    }

    public boolean userPictureProcessingWait(String id) throws BrokenBarrierException, InterruptedException, TimeoutException {
        userPictureCyclicBarrier.put(id, new CyclicBarrier(2));
        userPictureCyclicBarrier.get(id).await(90, TimeUnit.SECONDS);
        return userPictureEventResult;
    }

    public void userPictureProcessingResultNotice(String id, boolean value) throws BrokenBarrierException, InterruptedException, TimeoutException {
        userPictureEventResult = value;
        userPictureCyclicBarrier.get(id).await(90, TimeUnit.SECONDS);
        thumbnailCyclicBarrier.remove(id);

    }
}

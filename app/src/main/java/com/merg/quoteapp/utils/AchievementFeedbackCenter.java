package com.merg.quoteapp.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.merg.quoteapp.model.AchievementFeedbackEvent;

import java.util.LinkedList;
import java.util.Queue;

public class AchievementFeedbackCenter {

    private static volatile AchievementFeedbackCenter instance;

    private final Queue<AchievementFeedbackEvent> pendingEvents = new LinkedList<>();
    private final MutableLiveData<Integer> eventSignal = new MutableLiveData<>(0);

    private AchievementFeedbackCenter() {
    }

    public static AchievementFeedbackCenter getInstance() {
        if (instance == null) {
            synchronized (AchievementFeedbackCenter.class) {
                if (instance == null) {
                    instance = new AchievementFeedbackCenter();
                }
            }
        }
        return instance;
    }

    public LiveData<Integer> getEventSignal() {
        return eventSignal;
    }

    public synchronized void publish(AchievementFeedbackEvent event) {
        if (event == null) {
            return;
        }
        pendingEvents.add(event);
        Integer currentSignal = eventSignal.getValue();
        eventSignal.postValue(currentSignal == null ? 1 : currentSignal + 1);
    }

    public synchronized AchievementFeedbackEvent poll() {
        return pendingEvents.poll();
    }

    public synchronized boolean hasPendingEvents() {
        return !pendingEvents.isEmpty();
    }
}

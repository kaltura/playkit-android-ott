/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.ott;

import android.content.Context;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.BuildConfig;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.api.phoenix.services.BookmarkService;
import com.kaltura.playkit.utils.Consts;

import java.util.Timer;
import java.util.TimerTask;

public class PhoenixAnalyticsPlugin extends PKPlugin {
    private static final PKLog log = PKLog.get("PhoenixAnalyticsPlugin");
    private static final double MEDIA_ENDED_THRESHOLD = 0.98;

    // Fields shared with TVPAPIAnalyticsPlugin
    int mediaHitInterval;
    Timer timer;
    Player player;
    Context context;
    MessageBus messageBus;
    PKMediaConfig mediaConfig;
    RequestQueue requestsExecutor;

    String fileId;
    String currentMediaId = "UnKnown";
    String baseUrl;
    long lastKnownPlayerPosition = 0;
    boolean isAdPlaying;

    private String ks;
    private int partnerId;
    private boolean playEventWasFired;
    private boolean intervalOn = false;
    private boolean isFirstPlay = true;
    private boolean isMediaFinished = false;
    private String kalturaAssetType = "media";

    enum PhoenixActionType {
        HIT,
        PLAY,
        STOP,
        PAUSE,
        FIRST_PLAY,
        SWOOSH,
        LOAD,
        FULL_SCREEN,
        SEND_TO_FRIEND,
        FULL_SCREEN_EXIT,
        FINISH,
        BITRATE_CHANGE,
        ERROR
    }

    enum PositionOwner {
        HOUSEHOLD,
        USER
    }

    public static final Factory factory = new Factory() {
        @Override
        public String getName() {
            return "PhoenixAnalytics";
        }

        @Override
        public PKPlugin newInstance() {
            return new PhoenixAnalyticsPlugin();
        }

        @Override
        public String getVersion() {
            return BuildConfig.VERSION_NAME;
        }

        @Override
        public void warmUp(Context context) {

        }
    };

    @Override
    protected void onLoad(Player player, Object config, final MessageBus messageBus, Context context) {
        log.d("onLoad");

        this.requestsExecutor = APIOkRequestsExecutor.getSingleton();
        this.player = player;
        this.context = context;
        this.messageBus = messageBus;
        this.timer = new Timer();
        setConfigMembers(config);
        if (baseUrl != null && !baseUrl.isEmpty() && partnerId > 0) {
            messageBus.listen(mEventListener, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.PLAYING, PlayerEvent.Type.PLAYHEAD_UPDATED, PlayerEvent.Type.ENDED, PlayerEvent.Type.ERROR, PlayerEvent.Type.STOPPED, PlayerEvent.Type.REPLAY, PlayerEvent.Type.SEEKED, PlayerEvent.Type.SOURCE_SELECTED, AdEvent.Type.CONTENT_PAUSE_REQUESTED, AdEvent.Type.CONTENT_RESUME_REQUESTED);
        } else {
            log.e("Error, base url/partner - incorrect");
        }
    }

    private void setConfigMembers(Object config) {
        PhoenixAnalyticsConfig pluginConfig = parseConfig(config);
        this.baseUrl = pluginConfig.getBaseUrl();
        this.partnerId = pluginConfig.getPartnerId();
        this.ks = pluginConfig.getKS();
        this.mediaHitInterval = (pluginConfig.getTimerInterval() > 0) ? pluginConfig.getTimerInterval() * (int) Consts.MILLISECONDS_MULTIPLIER : Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH;
        this.kalturaAssetType = pluginConfig.getKalturaAssetType();
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        this.mediaConfig = mediaConfig;
        isFirstPlay = true;
        playEventWasFired = false;
        isMediaFinished = false;
    }

    @Override
    protected void onUpdateConfig(Object config) {
        setConfigMembers(config);
        if (baseUrl == null || baseUrl.isEmpty() || partnerId <= 0) {
            cancelTimer();
            messageBus.remove(mEventListener, (Enum[]) PlayerEvent.Type.values());
        }
    }

    @Override
    protected void onApplicationPaused() {
        log.d("PhoenixAnalyticsPlugin onApplicationPaused");
        if (player != null) {
            long playerPosOnPause = player.getCurrentPosition();
            if (playerPosOnPause > 0 && !isAdPlaying) {
                lastKnownPlayerPosition = playerPosOnPause / Consts.MILLISECONDS_MULTIPLIER;
            }
        }
        cancelTimer();
    }

    @Override
    protected void onApplicationResumed() {
        log.d("PhoenixAnalyticsPlugin onApplicationResumed");
        if (!isAdPlaying) {
            startMediaHitInterval();
        }
    }

    @Override
    public void onDestroy() {
        log.d("onDestroy");
        cancelTimer();
    }

    private PKEvent.Listener mEventListener = new PKEvent.Listener() {
        @Override
        public void onEvent(PKEvent event) {
            if (event instanceof AdEvent) {
                log.d("Ad Event = " + ((AdEvent) event).type.name() + ", lastKnownPlayerPosition = " + lastKnownPlayerPosition);
                switch (((AdEvent) event).type) {
                    case CONTENT_PAUSE_REQUESTED:
                        isAdPlaying = true;
                        break;
                    case CONTENT_RESUME_REQUESTED:
                        isAdPlaying = false;
                        break;
                    default:
                        break;
                }
            }
            if (event instanceof PlayerEvent) {
                if (event.eventType() != PlayerEvent.Type.PLAYHEAD_UPDATED) {
                    log.d("Player Event = " + ((PlayerEvent) event).type.name() + ", lastKnownPlayerPosition = " + lastKnownPlayerPosition);
                }
                switch (((PlayerEvent) event).type) {
                    case PLAYHEAD_UPDATED:
                        if (!isAdPlaying) {
                            PlayerEvent.PlayheadUpdated playheadUpdated = (PlayerEvent.PlayheadUpdated) event;
                            if (playheadUpdated != null && playheadUpdated.position > 0) {
                                lastKnownPlayerPosition = playheadUpdated.position / Consts.MILLISECONDS_MULTIPLIER;
                            }
                        }
                        break;
                    case STOPPED:
                        if (isMediaFinished) {
                            return;
                        }
                        isAdPlaying = false;
                        sendAnalyticsEvent(PhoenixActionType.STOP);
                        resetTimer();
                        break;
                    case ENDED:
                        resetTimer();
                        sendAnalyticsEvent(PhoenixActionType.FINISH);
                        playEventWasFired = false;
                        isMediaFinished = true;
                        break;
                    case ERROR:
                        resetTimer();
                        sendAnalyticsEvent(PhoenixActionType.ERROR);
                        break;
                    case SOURCE_SELECTED:
                        PlayerEvent.SourceSelected sourceSelected = (PlayerEvent.SourceSelected) event;
                        fileId = sourceSelected.source.getId();

                        if (mediaConfig != null && mediaConfig.getMediaEntry() != null) {
                            currentMediaId = mediaConfig.getMediaEntry().getId();
                        }
                        lastKnownPlayerPosition = 0;
                        if (mediaConfig != null && mediaConfig.getStartPosition() != null) {
                            lastKnownPlayerPosition = mediaConfig.getStartPosition();
                        }
                        sendAnalyticsEvent(PhoenixActionType.LOAD);
                        break;
                    case PAUSE:
                        if (isMediaFinished) {
                            return;
                        }
                        if (playEventWasFired) {
                            sendAnalyticsEvent(PhoenixActionType.PAUSE);
                            playEventWasFired = false;
                        }
                        resetTimer();
                        break;
                    case PLAY:
                        if (isMediaFinished) {
                            return;
                        }
                        if (isFirstPlay) {
                            playEventWasFired = true;
                            sendAnalyticsEvent(PhoenixActionType.FIRST_PLAY);
                            sendAnalyticsEvent(PhoenixActionType.HIT);
                        }
                        if (!intervalOn) {
                            startMediaHitInterval();
                        }
                        break;
                    case PLAYING:
                        isMediaFinished = false;
                        if (!isFirstPlay && !playEventWasFired) {
                            sendAnalyticsEvent(PhoenixActionType.PLAY);
                            playEventWasFired = true;
                        } else {
                            isFirstPlay = false;
                        }
                        isAdPlaying = false;
                        break;
                    case SEEKED:
                    case REPLAY:
                        //Receiving one of this events, mean that media position was reset.
                        isMediaFinished = false;
                        break;
                    default:
                        break;
                }
            }
        }
    };

    void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        intervalOn = false;
    }

    private void resetTimer() {
        cancelTimer();
        timer = new Timer();
    }

    /**
     * Media Hit analytics event
     */
    private void startMediaHitInterval() {
        log.d("startMediaHitInterval - Timer");
        if (timer == null) {
            timer = new Timer();
        }
        intervalOn = true;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendAnalyticsEvent(PhoenixActionType.HIT);
                if (player.getCurrentPosition() > 0 && !isAdPlaying) {
                    lastKnownPlayerPosition = player.getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER;
                }
                if (player.getDuration() > 0 && ((float) lastKnownPlayerPosition / player.getDuration() > MEDIA_ENDED_THRESHOLD)) {
                    sendAnalyticsEvent(PhoenixActionType.FINISH);
                    playEventWasFired = false;
                    isMediaFinished = true;
                }
            }
        }, mediaHitInterval, mediaHitInterval); // Get media hit interval from plugin config
    }

    /**
     * Send Bookmark/add event using Kaltura Phoenix Rest API
     *
     * @param eventType - Enum stating the event type to send
     */
    protected void sendAnalyticsEvent(final PhoenixActionType eventType) {
        if (isAdPlaying) {
            return;
        }
        if (eventType != PhoenixActionType.STOP) {
            if (player.getCurrentPosition() > 0) {
                lastKnownPlayerPosition = player.getCurrentPosition() / Consts.MILLISECONDS_MULTIPLIER;
            }
        }
        if (mediaConfig == null || mediaConfig.getMediaEntry() == null || mediaConfig.getMediaEntry().getId() == null) {
            log.e("Error mediaConfig is not valid");
            return;
        }
        if (eventType == PhoenixActionType.FINISH) {
            lastKnownPlayerPosition = player.getDuration();
        }
        log.d("PhoenixAnalyticsPlugin sendAnalyticsEvent " + eventType + " isAdPlaying " + isAdPlaying + " position = " + lastKnownPlayerPosition);

        RequestBuilder requestBuilder = BookmarkService.actionAdd(baseUrl, partnerId, ks,
                kalturaAssetType, currentMediaId, eventType.name(), lastKnownPlayerPosition, fileId, PositionOwner.HOUSEHOLD.name(), "", isFinishedWatching(eventType));

        requestBuilder.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(ResponseElement response) {
                if (response.isSuccess() && response.getError() != null && response.getError().getCode().equals("4001")) {
                    messageBus.post(new OttEvent(OttEvent.OttEventType.Concurrency));
                    messageBus.post(new PhoenixAnalyticsEvent.PhoenixAnalyticsReport(eventType.toString()));
                    log.d("onComplete send event: " + eventType);
                }
            }
        });
        requestsExecutor.queue(requestBuilder.build());
    }

    private boolean isFinishedWatching(PhoenixActionType actionType) {
        switch(actionType) {
            case FINISH:
                return true;
            case STOP:
                if (player != null && player.getCurrentPosition() >= player.getDuration()) {
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    PKEvent.Listener getEventListener() {
        return mEventListener;
    }


    private static PhoenixAnalyticsConfig parseConfig(Object config) {
        if (config instanceof PhoenixAnalyticsConfig) {
            return ((PhoenixAnalyticsConfig) config);

        } else if (config instanceof JsonObject) {
            JsonObject params = (JsonObject) config;
            String baseUrl = params.get("baseUrl").getAsString();
            int partnerId = params.get("partnerId").getAsInt();
            int timerInterval = params.get("timerInterval").getAsInt();
            String ks = params.get("ks").getAsString();
            String kalturaAssetType = (params.has("kalturaAssetType")) ? params.get("kalturaAssetType").getAsString() : "media";

            return new PhoenixAnalyticsConfig(partnerId, baseUrl, ks, timerInterval, APIDefines.KalturaAssetType.valueOf(kalturaAssetType));
        }
        return null;
    }
}

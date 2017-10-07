package com.mixpanel.android.mpmetrics;

import android.os.Handler;

import com.mixpanel.android.util.MPLog;

import org.json.JSONArray;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

class ConnectIntegrations implements OnMixpanelUpdatesReceivedListener {
    public ConnectIntegrations(MixpanelAPI mixpanel) {
        mMixpanel = mixpanel;
        mSavedUrbanAirshipChannelID = null;
    }

    public void onMixpanelUpdatesReceived() {
        setupIntegrations(mMixpanel.getPeople().getConnectIntegrationIds());
    }

    private synchronized void setupIntegrations(Set<Integer> integrationIds) {
        if (integrationIds.contains(4)) {
            setUrbanAirshipPeopleProp();
        }
    }

    private synchronized void setUrbanAirshipPeopleProp() {
        String urbanAirshipClassName = "com.urbanairship.UAirship";
        try {
            Class urbanAirshipClass = Class.forName(urbanAirshipClassName);
            Object sharedUAirship = urbanAirshipClass.getMethod("shared", null).invoke(null);
            Object pushManager = sharedUAirship.getClass().getMethod("getPushManager", null).invoke(sharedUAirship);
            String channelID = (String)pushManager.getClass().getMethod("getChannelId", null).invoke(pushManager);
            if (channelID != null && !channelID.isEmpty()) {
                if (mSavedUrbanAirshipChannelID == null || !mSavedUrbanAirshipChannelID.equals(channelID)) {
                    mMixpanel.getPeople().set("$urban_airship_channel_id", channelID);
                    mSavedUrbanAirshipChannelID = channelID;
                }
            } else {
                final Handler delayedHandler = new Handler();
                delayedHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setUrbanAirshipPeopleProp();
                    }
                }, 2000);
            }
        } catch (ClassNotFoundException e) {
            MPLog.w(LOGTAG, "Urban Airship SDK not found but Urban Airship is integrated on Mixpanel");
        } catch (NoSuchMethodException e) {
            MPLog.e(LOGTAG, "Urban Airship SDK class exists but methods do not");
        } catch (InvocationTargetException e) {
            MPLog.e(LOGTAG, "method invocation failed");
        } catch (IllegalAccessException e) {
            MPLog.e(LOGTAG, "method invocation failed");
        }
    }

    private final MixpanelAPI mMixpanel;
    private String mSavedUrbanAirshipChannelID;

    private static final String LOGTAG = "MixpanelAPI.ConnectIntegrations";
}
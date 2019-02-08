// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.analytics;

import android.app.Application;
import android.content.Context;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.AppsFlyerTrackingRequestListener;
import java.util.HashMap;
import java.util.Map;

/** Wraps {@link AppsFlyerLib} */
public class AppsFlyerLibWrapper {
  static final String AF_DEFAULT_CURRENCY = "USD";

  @GuardedBy("AppsFlyerLibWrapper.class")
  private static AppsFlyerLibWrapper instance;

  @GuardedBy("this")
  private String currencyCode;

  public static AppsFlyerLibWrapper getInstance() {
    if (instance == null) {
      synchronized (AppsFlyerLibWrapper.class) {
        if (instance == null) {
          instance = new AppsFlyerLibWrapper();
        }
      }
    }
    return instance;
  }

  AppsFlyerLibWrapper() {
  }

  /**
   * Wraps calls to {@link AppsFlyerLib#setAppUserId(String)}
   *
   * @deprecated use {@link #setCustomerUserId(String)} instead
   */
  @Deprecated
  public void setAppUserId(String id) {
    getGoogleAnalytics().setUserId(id);
    getAppsFlyerLib().setAppUserId(id);
  }

  /** Wraps calls to {@link AppsFlyerLib#setCustomerUserId(String)} */
  public void setCustomerUserId(String id) {
    getGoogleAnalytics().setUserId(id);
    getAppsFlyerLib().setCustomerUserId(id);
  }

  /** Wraps calls to {@link AppsFlyerLib#setCustomerIdAndTrack(String, Context)} */
  public void setCustomerIdAndTrack(String id, @NonNull Context context) {
    getGoogleAnalytics().registerContext(context).setUserId(id);
    getAppsFlyerLib().setCustomerIdAndTrack(id, context);
  }

  /** Wraps calls to {@link AppsFlyerLib#init(String, AppsFlyerConversionListener, Context)} */
  public AppsFlyerLib init(
      String key, AppsFlyerConversionListener appsFlyerConversionListener, Context context) {
    getGoogleAnalytics().registerContext(context);
    return getAppsFlyerLib().init(key, appsFlyerConversionListener, context);
  }

  /** Wraps calls to {@link AppsFlyerLib#startTracking(Application)} */
  public void startTracking(Application application) {
    getGoogleAnalytics().registerContext(application);
    getAppsFlyerLib().startTracking(application);
  }

  /** Wraps calls to {@link AppsFlyerLib#startTracking(Application, String)} */
  public void startTracking(Application application, String key) {
    getGoogleAnalytics().registerContext(application);
    getAppsFlyerLib().startTracking(application, key);
  }

  /**
   * Wraps calls to {@link AppsFlyerLib#startTracking(Application, String,
   * AppsFlyerTrackingRequestListener)}
   */
  public void startTracking(
      Application application, String key, AppsFlyerTrackingRequestListener listener) {
    getGoogleAnalytics().registerContext(application);
    getAppsFlyerLib().startTracking(application, key, listener);
  }

  /** Wraps calls to {@link AppsFlyerLib#setCurrencyCode(String)} */
  public void setCurrencyCode(String currencyCode) {
    synchronized (this) {
      this.currencyCode = currencyCode;
    }
    getAppsFlyerLib().setCurrencyCode(currencyCode);
  }

  /** Wraps calls to {@link AppsFlyerLib#trackEvent(Context, String, Map)} */
  public void trackEvent(Context context, String eventName, Map<String, Object> eventValues) {
    Map<String, Object> googleAnalyticsParams;
    synchronized (this) {
      if (eventValues != null
          && !eventValues.containsKey(AFInAppEventParameterName.CURRENCY)
          && eventValues.containsKey(AFInAppEventParameterName.REVENUE)) {
        googleAnalyticsParams = new HashMap<>(eventValues);
        googleAnalyticsParams.put(
            AFInAppEventParameterName.CURRENCY,
            currencyCode != null ? currencyCode : AF_DEFAULT_CURRENCY);
      } else {
        googleAnalyticsParams = eventValues;
      }
    }
    getGoogleAnalytics().registerContext(context).logEvent(eventName, googleAnalyticsParams);
    getAppsFlyerLib().trackEvent(context, eventName, eventValues);
  }

  /** Wraps calls to {@link AppsFlyerLib#setDeviceTrackingDisabled(boolean)} */
  public void setDeviceTrackingDisabled(boolean isDisabled) {
    getGoogleAnalytics().setAnalyticsCollectionEnabled(!isDisabled);
    getAppsFlyerLib().setDeviceTrackingDisabled(isDisabled);
  }

  /** Wraps calls to {@link AppsFlyerLib#setPreinstallAttribution(String, String, String)} */
  public void setPreinstallAttribution(String mediaSource, String campaign, String siteId) {
    Map<String, Object> params = new HashMap<>(3);
    if (mediaSource != null) {
      params.put(FirebaseAnalytics.Param.SOURCE, mediaSource);
    }
    if (campaign != null) {
      params.put(FirebaseAnalytics.Param.CAMPAIGN, campaign);
    }
    if (siteId != null) {
      params.put(FirebaseAnalytics.Param.MEDIUM, siteId);
    }
    getGoogleAnalytics().logEvent(FirebaseAnalytics.Event.CAMPAIGN_DETAILS, params);
    getAppsFlyerLib().setPreinstallAttribution(mediaSource, campaign, siteId);
  }

  /** Wraps calls to {@link AppsFlyerLib#setMinTimeBetweenSessions(int)} */
  public void setMinTimeBetweenSessions(int seconds) {
    getGoogleAnalytics().setSessionTimeoutDuration(seconds * 1000);
    getAppsFlyerLib().setMinTimeBetweenSessions(seconds);
  }

  /**
   * Wraps calls to {@link AppsFlyerGoogleAnalyticsAdapterProvider#getGoogleAnalyticsAdapter()} to
   * allow the call to be mocked in tests.
   *
   * @hide
   */
  GoogleAnalyticsAdapter getGoogleAnalytics() {
    return AppsFlyerGoogleAnalyticsAdapterProvider.getGoogleAnalyticsAdapter();
  }

  /**
   * Wraps calls to {@link AppsFlyerLib#getInstance()} to allow the call to be mocked in tests.
   *
   * @hide
   */
  AppsFlyerLib getAppsFlyerLib() {
    return AppsFlyerLib.getInstance();
  }
}

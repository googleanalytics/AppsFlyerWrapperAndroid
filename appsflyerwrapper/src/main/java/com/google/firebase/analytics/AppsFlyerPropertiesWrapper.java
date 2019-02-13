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

import android.support.annotation.GuardedBy;
import com.appsflyer.AppsFlyerProperties;
import java.util.Arrays;

/** Wraps {@link AppsFlyerPropertiesWrapper} */
public class AppsFlyerPropertiesWrapper {

  @GuardedBy("AppsFlyerPropertiesWrapper.class")
  private static AppsFlyerPropertiesWrapper instance;

  public static AppsFlyerPropertiesWrapper getInstance() {
    if (instance == null) {
      synchronized (AppsFlyerPropertiesWrapper.class) {
        if (instance == null) {
          instance = new AppsFlyerPropertiesWrapper();
        }
      }
    }
    return instance;
  }

  AppsFlyerPropertiesWrapper() {
  }

  /** Wraps calls to {@link AppsFlyerProperties#set(String, String)} */
  public void set(String key, String value) {
    setUserProperty(key, value);
    getAppsFlyerProperties().set(key, value);
  }

  /** Wraps calls to {@link AppsFlyerProperties#set(String, String[])} */
  public void set(String key, String[] value) {
    setUserProperty(key, value != null ? Arrays.toString(value) : null);
    getAppsFlyerProperties().set(key, value);
  }

  /** Wraps calls to {@link AppsFlyerProperties#set(String, int)} */
  public void set(String key, int value) {
    setUserProperty(key, Integer.toString(value));
    getAppsFlyerProperties().set(key, value);
  }

  /** Wraps calls to {@link AppsFlyerProperties#set(String, long)} */
  public void set(String key, long value) {
    setUserProperty(key, Long.toString(value));
    getAppsFlyerProperties().set(key, value);
  }

  /** Wraps calls to {@link AppsFlyerProperties#set(String, boolean)} */
  public void set(String key, boolean value) {
    setUserProperty(key, Boolean.toString(value));
    getAppsFlyerProperties().set(key, value);
  }

  /** Wraps call to {@link GoogleAnalyticsAdapter#setUserProperty(String, String)}. */
  private void setUserProperty(String name, String value) {
    if (AppsFlyerProperties.APP_USER_ID.equals(name)) {
      getGoogleAnalytics().setUserId(value);
      return;
    }
    getGoogleAnalytics().setUserProperty(name, value);
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
   * Wraps calls to {@link AppsFlyerProperties#getInstance()} to allow the call to be mocked in
   * tests.
   *
   * @hide
   */
  AppsFlyerProperties getAppsFlyerProperties() {
    return AppsFlyerProperties.getInstance();
  }
}

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
import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFInAppEventType;
import com.appsflyer.AppsFlyerProperties;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initializes and provides {@link GoogleAnalyticsAdapter} singleton for {@link AppsFlyerLibWrapper}
 * and {@link AppsFlyerPropertiesWrapper}.
 */
class AppsFlyerGoogleAnalyticsAdapterProvider {
  private static final String AF_PREFIX = "af_";

  private static final String EMPTY_EVENT_NAME = "af_unnamed_event";
  private static final String EMPTY_PARAM_NAME = "af_unnamed_parameter";
  private static final String EMPTY_USER_PROPERTY_NAME = "af_unnamed_user_property";

  private static final String WRAPPER_PARAM_VALUE = "af";

  /**
   * Maps predefined AppsFlyer event names ({@link AFInAppEventType}) to their Google Analytics for
   * Firebase equivalent ({@link FirebaseAnalytics.Event}).
   */
  private static final Map<String, String> EVENT_MAP = new HashMap<>();

  static {
    EVENT_MAP.put(AFInAppEventType.LEVEL_ACHIEVED, FirebaseAnalytics.Event.LEVEL_UP);
    EVENT_MAP.put(AFInAppEventType.ADD_PAYMENT_INFO, FirebaseAnalytics.Event.ADD_PAYMENT_INFO);
    EVENT_MAP.put(AFInAppEventType.ADD_TO_CART, FirebaseAnalytics.Event.ADD_TO_CART);
    EVENT_MAP.put(AFInAppEventType.ADD_TO_WISH_LIST, FirebaseAnalytics.Event.ADD_TO_WISHLIST);
    EVENT_MAP.put(AFInAppEventType.TUTORIAL_COMPLETION, FirebaseAnalytics.Event.TUTORIAL_COMPLETE);
    EVENT_MAP.put(AFInAppEventType.INITIATED_CHECKOUT, FirebaseAnalytics.Event.BEGIN_CHECKOUT);
    EVENT_MAP.put(AFInAppEventType.PURCHASE, FirebaseAnalytics.Event.ECOMMERCE_PURCHASE);
    EVENT_MAP.put(AFInAppEventType.SEARCH, FirebaseAnalytics.Event.SEARCH);
    EVENT_MAP.put(AFInAppEventType.SPENT_CREDIT, FirebaseAnalytics.Event.SPEND_VIRTUAL_CURRENCY);
    EVENT_MAP.put(
        AFInAppEventType.ACHIEVEMENT_UNLOCKED, FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT);
    EVENT_MAP.put(AFInAppEventType.CONTENT_VIEW, FirebaseAnalytics.Event.VIEW_ITEM);
    EVENT_MAP.put(AFInAppEventType.SHARE, FirebaseAnalytics.Event.SHARE);
    EVENT_MAP.put(AFInAppEventType.LOGIN, FirebaseAnalytics.Event.LOGIN);
  }

  /**
   * Maps predefined AppsFlyer event parameter names ({@link AFInAppEventParameterName}) to their
   * Google Analytics for Firebase equivalent ({@link FirebaseAnalytics.Param}).
   */
  private static final Map<String, String> PARAM_MAP = new HashMap<>();

  static {
    PARAM_MAP.put(AFInAppEventParameterName.LEVEL, FirebaseAnalytics.Param.LEVEL);
    PARAM_MAP.put(AFInAppEventParameterName.SCORE, FirebaseAnalytics.Param.SCORE);
    PARAM_MAP.put(AFInAppEventParameterName.SUCCESS, FirebaseAnalytics.Param.SUCCESS);
    PARAM_MAP.put(AFInAppEventParameterName.PRICE, FirebaseAnalytics.Param.PRICE);
    PARAM_MAP.put(AFInAppEventParameterName.CONTENT_TYPE, FirebaseAnalytics.Param.CONTENT_TYPE);
    PARAM_MAP.put(AFInAppEventParameterName.CONTENT_ID, FirebaseAnalytics.Param.ITEM_ID);
    PARAM_MAP.put(AFInAppEventParameterName.CURRENCY, FirebaseAnalytics.Param.CURRENCY);
    PARAM_MAP.put(AFInAppEventParameterName.QUANTITY, FirebaseAnalytics.Param.QUANTITY);
    PARAM_MAP.put(AFInAppEventParameterName.REGSITRATION_METHOD, FirebaseAnalytics.Param.METHOD);
    PARAM_MAP.put(AFInAppEventParameterName.SEARCH_STRING, FirebaseAnalytics.Param.SEARCH_TERM);
    PARAM_MAP.put(AFInAppEventParameterName.DATE_A, FirebaseAnalytics.Param.START_DATE);
    PARAM_MAP.put(AFInAppEventParameterName.DATE_B, FirebaseAnalytics.Param.END_DATE);
    PARAM_MAP.put(AFInAppEventParameterName.DESTINATION_A, FirebaseAnalytics.Param.ORIGIN);
    PARAM_MAP.put(AFInAppEventParameterName.DESTINATION_B, FirebaseAnalytics.Param.DESTINATION);
    PARAM_MAP.put(AFInAppEventParameterName.EVENT_START, FirebaseAnalytics.Param.START_DATE);
    PARAM_MAP.put(AFInAppEventParameterName.EVENT_END, FirebaseAnalytics.Param.END_DATE);
    PARAM_MAP.put(AFInAppEventParameterName.REVENUE, FirebaseAnalytics.Param.VALUE);
  }

  /**
   * Blacklisted predefined AppsFlyer user properties ({@link AppsFlyerProperties}) that Google
   * Analytics for Firebase will not log because they either contain PII or are not applicable.
   */
  private static final List<String> BLACKLISTED_USER_PROPERTIES =
      Arrays.asList(
          AppsFlyerProperties.COLLECT_ANDROID_ID_FORCE_BY_USER,
          AppsFlyerProperties.COLLECT_FACEBOOK_ATTR_ID,
          AppsFlyerProperties.COLLECT_FINGER_PRINT,
          AppsFlyerProperties.COLLECT_IMEI,
          AppsFlyerProperties.COLLECT_IMEI_FORCE_BY_USER,
          AppsFlyerProperties.COLLECT_MAC,
          AppsFlyerProperties.USER_EMAIL,
          AppsFlyerProperties.USER_EMAILS);

  @GuardedBy("AppsFlyerGoogleAnalyticsAdapterProvider.class")
  private static GoogleAnalyticsAdapter adapter;

  static GoogleAnalyticsAdapter getGoogleAnalyticsAdapter() {
    synchronized (AppsFlyerGoogleAnalyticsAdapterProvider.class) {
      if (adapter == null) {
        adapter =
            new GoogleAnalyticsAdapter.Builder()
                .setWrappedSdkName(WRAPPER_PARAM_VALUE)
                .setSanitizedNamePrefix(AF_PREFIX)
                .setEmptyEventName(EMPTY_EVENT_NAME)
                .setEmptyParamName(EMPTY_PARAM_NAME)
                .setEmptyUserPropertyName(EMPTY_USER_PROPERTY_NAME)
                .setEventNameMap(EVENT_MAP)
                .setParamNameMap(PARAM_MAP)
                .setBlacklistedUserPropertyNames(BLACKLISTED_USER_PROPERTIES)
                .build();
      }
    }
    return adapter;
  }

  AppsFlyerGoogleAnalyticsAdapterProvider() {
  }
}

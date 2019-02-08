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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Adapter that formats Google Analytics for Firebase call arguments so that they adhere to
 * formatting rules.
 */
public class GoogleAnalyticsAdapter {

  /** The builder for {@link GoogleAnalyticsAdapter}. */
  public static class Builder {
    private Map<String, String> eventNameMap;
    private Map<String, String> paramNameMap;
    private Map<String, String> userPropertyNameMap;
    private List<String> blacklistedEventNames;
    private List<String> blacklistedParamNames;
    private List<String> blacklistedUserPropertyNames;
    private String wrappedSdkName;
    private String sanitizedNamePrefix;
    private String emptyEventName;
    private String emptyParamName;
    private String emptyUserPropertyName;

    Builder setEventNameMap(@Nullable Map<String, String> eventNameMap) {
      this.eventNameMap = eventNameMap;
      return this;
    }

    Builder setParamNameMap(@Nullable Map<String, String> paramNameMap) {
      this.paramNameMap = paramNameMap;
      return this;
    }

    Builder setUserPropertyNameMap(@Nullable Map<String, String> userPropertyNameMap) {
      this.userPropertyNameMap = userPropertyNameMap;
      return this;
    }

    Builder setBlacklistedEventNames(@Nullable List<String> blacklistedEventNames) {
      this.blacklistedEventNames = blacklistedEventNames;
      return this;
    }

    Builder setBlacklistedParamNames(@Nullable List<String> blacklistedParamNames) {
      this.blacklistedParamNames = blacklistedParamNames;
      return this;
    }

    Builder setBlacklistedUserPropertyNames(@Nullable List<String> blacklistedUserPropertyNames) {
      this.blacklistedUserPropertyNames = blacklistedUserPropertyNames;
      return this;
    }

    Builder setWrappedSdkName(@NonNull String wrappedSdkName) {
      if (wrappedSdkName == null
          || isStringTooLong(wrappedSdkName, MAX_PARAM_STRING_VALUE_LENGTH)) {
        throw new IllegalArgumentException(
            String.format(
                "Wrapped SDK Name must be non-null and not exceed %d characters",
                MAX_PARAM_STRING_VALUE_LENGTH));
      }
      this.wrappedSdkName = wrappedSdkName;
      return this;
    }

    Builder setSanitizedNamePrefix(@NonNull String sanitizedNamePrefix) {
      if (!isValidPublicNameFormat(sanitizedNamePrefix)) {
        throw new IllegalArgumentException(
            "Sanitized Name Prefix must be non-null, may only contain alphanumeric characters"
                + "and underscores (\"_\"), and must start with an alphabetic character");
      }
      this.sanitizedNamePrefix = sanitizedNamePrefix;
      return this;
    }

    Builder setEmptyEventName(@NonNull String emptyEventName) {
      if (!isValidPublicName(emptyEventName, MAX_EVENT_NAME_LENGTH, RESTRICTED_EVENT_NAMES)) {
        throw new IllegalArgumentException(
            "Empty Event Name must conform to formatting rules outlined at "
                + "https://firebase.google.com/docs/reference/android/com/google/firebase/"
                + "analytics/FirebaseAnalytics.Event");
      }
      this.emptyEventName = emptyEventName;
      return this;
    }

    Builder setEmptyParamName(@NonNull String emptyParamName) {
      if (!isValidPublicName(emptyParamName, MAX_PARAM_NAME_LENGTH, RESTRICTED_PARAM_NAMES)) {
        throw new IllegalArgumentException(
            "Empty Param Name must conform to formatting rules outlined at "
                + "https://firebase.google.com/docs/reference/android/com/google/firebase/"
                + "analytics/FirebaseAnalytics.Param");
      }
      this.emptyParamName = emptyParamName;
      return this;
    }

    Builder setEmptyUserPropertyName(@NonNull String emptyUserPropertyName) {
      if (!isValidPublicName(
          emptyUserPropertyName, MAX_USER_PROPERTY_NAME_LENGTH, RESTRICTED_USER_PROPERTY_NAMES)) {
        throw new IllegalArgumentException(
            "Empty User Property name must conform to formatting rules outlined at "
                + "https://firebase.google.com/docs/reference/android/com/google/firebase/"
                + "analytics/FirebaseAnalytics.UserProperty");
      }
      this.emptyUserPropertyName = emptyUserPropertyName;
      return this;
    }

    public GoogleAnalyticsAdapter build() {
      if (wrappedSdkName == null) {
        throw new IllegalArgumentException("wrappedSdkName must not be empty");
      }
      if (sanitizedNamePrefix == null) {
        throw new IllegalArgumentException("sanitizedNamePrefix must not be empty");
      }
      if (emptyEventName == null) {
        emptyEventName = "unnamed_event";
      }
      if (emptyParamName == null) {
        emptyParamName = "unnamed_param";
      }
      if (emptyUserPropertyName == null) {
        emptyUserPropertyName = "unnamed_user_property";
      }
      return new GoogleAnalyticsAdapter(
          eventNameMap != null ? eventNameMap : Collections.<String, String>emptyMap(),
          paramNameMap != null ? paramNameMap : Collections.<String, String>emptyMap(),
          userPropertyNameMap != null
              ? userPropertyNameMap
              : Collections.<String, String>emptyMap(),
          blacklistedEventNames != null ? blacklistedEventNames : Collections.<String>emptyList(),
          blacklistedParamNames != null ? blacklistedParamNames : Collections.<String>emptyList(),
          blacklistedUserPropertyNames != null
              ? blacklistedUserPropertyNames
              : Collections.<String>emptyList(),
          wrappedSdkName,
          sanitizedNamePrefix,
          emptyEventName,
          emptyParamName,
          emptyUserPropertyName);
    }
  }

  private static final int MAX_PARAM_COUNT = 25;
  private static final int MAX_PARAM_NAME_LENGTH = 40;
  private static final int MAX_PARAM_STRING_VALUE_LENGTH = 100;
  private static final int MAX_EVENT_NAME_LENGTH = 40;
  private static final int MAX_USER_PROPERTY_NAME_LENGTH = 24;
  private static final int MAX_USER_PROPERTY_VALUE_LENGTH = 36;
  private static final int MAX_USER_ID_VALUE_LENGTH = 256;

  /**
   * Maximum number of {@link FirebaseAnalytics} API calls queued until a {@link Context} is
   * registered using {@link GoogleAnalyticsAdapter#registerContext}.
   */
  private static final int MAX_QUEUE_LENGTH = 1000;

  /** Idle number of worker threads. */
  private static final int CORE_QUEUE_POOL_SIZE = 0;

  /** Maximum number of worker threads. */
  private static final int MAX_QUEUE_POOL_SIZE = 1;

  /** Time to keep thread pool worker thread alive when idle */
  private static final long QUEUE_THREAD_KEEP_ALIVE_TIME_SECONDS = 30L;

  private static final String LOG_TAG = "FA-W";

  private static final String WRAPPER_PARAM_NAME = "api_wrapper";

  private static final List<String> RESERVED_NAME_PREFIXES =
      Arrays.asList("firebase_", "ga_", "google_");

  /**
   * Event names disallowed by Google Analytics. A prefix will be added if a developer logs an event
   * with a restricted name to disambiguate wrapped SDK events from the events automatically
   * collected by Google Analytics for Firebase.
   */
  private static final List<String> RESTRICTED_EVENT_NAMES =
      Arrays.asList(
          "first_open",
          "in_app_purchase",
          "error",
          "user_engagement",
          "session_start",
          "app_update",
          "app_remove",
          "os_update",
          "app_clear_data",
          "notification_foreground",
          "notification_receive",
          "notification_open",
          "notification_dismiss",
          "notification_send",
          "app_exception",
          "dynamic_link_first_open",
          "dynamic_link_app_open",
          "dynamic_link_app_update",
          "app_install",
          "ad_exposure",
          "adunit_exposure",
          "ad_query",
          "ad_activeview",
          "ad_impression",
          "ad_click",
          "app_upgrade",
          "screen_view",
          "first_visit");

  /**
   * Event param names disallowed by Google Analytics Adapter. A prefix will be added if a developer
   * includes event params with restricted names.
   */
  private static final List<String> RESTRICTED_PARAM_NAMES = Arrays.asList(WRAPPER_PARAM_NAME);

  /**
   * User property names disallowed by Google Analytics. A prefix will be added if a developer sets
   * a user property with a restricted name.
   */
  private static final List<String> RESTRICTED_USER_PROPERTY_NAMES =
      Arrays.asList(
          "first_open_time",
          "last_deep_link_referrer",
          "user_id",
          "first_open_after_install",
          "first_visit_time",
          "lifetime_user_engagement",
          "session_number",
          "session_id");

  private AtomicReference<FirebaseAnalytics> firebaseReference = new AtomicReference<>();

  /**
   * All non-static {@link FirebaseAnalytics} API calls ({@link FirebaseAnalytics#logEvent(String,
   * Bundle)}, {@link FirebaseAnalytics#setUserProperty(String, String)}, etc) should to be made on
   * this executor. Calls will be blocked until a {@link Context} is registered and a {@link
   * FirebaseAnalytics} instance can be obtained.
   */
  protected final ThreadPoolExecutor executor;

  @NonNull private final Map<String, String> eventNameMap;
  @NonNull private final Map<String, String> paramNameMap;
  @NonNull private final Map<String, String> userPropertyNameMap;

  @NonNull private final List<String> blacklistedEventNames;
  @NonNull private final List<String> blacklistedParamNames;
  @NonNull private final List<String> blacklistedUserPropertyNames;

  private final String wrappedSdkName;
  private final String sanitizedNamePrefix;

  private final String emptyEventName;
  private final String emptyParamName;
  private final String emptyUserPropertyName;

  GoogleAnalyticsAdapter(
      @NonNull Map<String, String> eventNameMap,
      @NonNull Map<String, String> paramNameMap,
      @NonNull Map<String, String> userPropertyNameMap,
      @NonNull List<String> blacklistedEventNames,
      @NonNull List<String> blacklistedParamNames,
      @NonNull List<String> blacklistedUserPropertyNames,
      @NonNull String wrappedSdkName,
      @NonNull String sanitizedNamePrefix,
      @NonNull String emptyEventName,
      @NonNull String emptyParamName,
      @NonNull String emptyUserPropertyName) {
    this.eventNameMap = eventNameMap;
    this.paramNameMap = paramNameMap;
    this.userPropertyNameMap = userPropertyNameMap;
    this.blacklistedEventNames = blacklistedEventNames;
    this.blacklistedParamNames = blacklistedParamNames;
    this.blacklistedUserPropertyNames = blacklistedUserPropertyNames;
    this.wrappedSdkName = wrappedSdkName;
    this.sanitizedNamePrefix = sanitizedNamePrefix;
    this.emptyEventName = emptyEventName;
    this.emptyParamName = emptyParamName;
    this.emptyUserPropertyName = emptyUserPropertyName;

    this.executor =
        new ThreadPoolExecutor(
            CORE_QUEUE_POOL_SIZE,
            MAX_QUEUE_POOL_SIZE,
            QUEUE_THREAD_KEEP_ALIVE_TIME_SECONDS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(MAX_QUEUE_LENGTH),
            new RejectedExecutionHandler() {
              @Override
              public void rejectedExecution(
                  Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
                if (executor.isShutdown()) {
                  Log.e(LOG_TAG, "Data loss. Executor service is shut down.");
                } else {
                  Log.e(LOG_TAG, "Data loss. Max task queueOnWorker size exceeded.");
                }
              }
            });
    // Initial executor runnable blocks Google Analytics for Firebase API call execution until an
    // instance of FirebaseAnalytics can be obtained.
    this.executor.execute(
        new Runnable() {
          @Override
          public void run() {
            synchronized (firebaseReference) {
              // Block executor until a FirebaseAnalytics instance can be obtained.
              while (firebaseReference.get() == null) {
                try {
                  firebaseReference.wait();
                } catch (InterruptedException ex) {
                  Log.e(LOG_TAG, "Error waiting for a FirebaseAnalytics instance.", ex);
                }
              }
            }
          }
        });
  }

  /**
   * Registers application {@link Context} required to obtain an instance of {@link
   * FirebaseAnalytics}.
   *
   * @param context the {@link Context} used to initialize Firebase Analytics. Call is a no-op if
   *     {@code null} or if an instance of {@link FirebaseAnalytics} has been already obtained.
   */
  public GoogleAnalyticsAdapter registerContext(@Nullable Context context) {
    if (context == null) {
      return this;
    }
    if (firebaseReference.get() == null) {
      synchronized (firebaseReference) {
        if (firebaseReference.compareAndSet(null, FirebaseAnalytics.getInstance(context))) {
          firebaseReference.notifyAll();
        }
      }
    }
    return this;
  }

  /**
   * Wraps call to {@link FirebaseAnalytics#logEvent(String, Bundle)}.
   *
   * <p>Note: Call will only be executed once an application {@link Context} is registered using
   * {@link GoogleAnalyticsAdapter#registerContext(Context)}.
   */
  public void logEvent(@Nullable String rawName, @Nullable Map<String, Object> rawParams) {
    if (blacklistedEventNames.contains(rawName)) {
      return;
    }

    final String name =
        sanitizeName(
            mapName(eventNameMap, rawName),
            MAX_EVENT_NAME_LENGTH,
            emptyEventName,
            RESTRICTED_EVENT_NAMES);
    if (name == null) {
      Log.w(LOG_TAG, String.format("Event %s sanitized to \'_\'. Dropping event.", rawName));
      return;
    }

    final Bundle params = new Bundle();
    params.putString(WRAPPER_PARAM_NAME, wrappedSdkName);

    if (rawParams != null) {
      for (Entry<String, Object> rawParam : rawParams.entrySet()) {
        if (rawParam.getValue() == null) {
          continue;
        }

        String paramName = rawParam.getKey();
        if (blacklistedParamNames.contains(paramName)) {
          continue;
        }

        String sanitizedParamName =
            sanitizeName(
                mapName(paramNameMap, paramName),
                MAX_PARAM_NAME_LENGTH,
                emptyParamName,
                RESTRICTED_PARAM_NAMES);
        if (sanitizedParamName == null) {
          Log.w(
              LOG_TAG,
              String.format("Parameter %s sanitized to \'_\'. Dropping param.", paramName));
          continue;
        }

        addParamToBundle(
            params, sanitizedParamName, rawParam.getValue(), MAX_PARAM_STRING_VALUE_LENGTH);
        if (params.size() >= MAX_PARAM_COUNT) {
          break;
        }
      }
    }

    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            getFirebaseAnalytics().logEvent(name, params);
          }
        });
  }

  /**
   * Wraps call to {@link FirebaseAnalytics#setUserProperty(String, String)}.
   *
   * <p>Note: Call will only be executed once an application {@link Context} is registered using
   * {@link GoogleAnalyticsAdapter#registerContext(Context)}.
   */
  public void setUserProperty(@Nullable String rawName, @Nullable String rawValue) {
    if (blacklistedEventNames != null && blacklistedUserPropertyNames.contains(rawName)) {
      return;
    }

    if (rawName != null && rawName.startsWith("firebase_exp_")) {
      rawName = sanitizedNamePrefix + rawName;
    }
    final String name =
        sanitizeName(
            mapName(userPropertyNameMap, rawName),
            MAX_USER_PROPERTY_NAME_LENGTH,
            emptyUserPropertyName,
            RESTRICTED_USER_PROPERTY_NAMES);
    if (name == null) {
      Log.w(
          LOG_TAG,
          String.format("User Property %s sanitized to \'_\'. Dropping user property.", rawName));
      return;
    }
    final String value = trimString(rawValue, MAX_USER_PROPERTY_VALUE_LENGTH);

    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            getFirebaseAnalytics().setUserProperty(name, value);
          }
        });
  }

  /**
   * Wraps call to {@link FirebaseAnalytics#setAnalyticsCollectionEnabled(boolean)}.
   *
   * <p>Note: Call will only be executed once an application {@link Context} is registered using
   * {@link GoogleAnalyticsAdapter#registerContext(Context)}.
   */
  public void setAnalyticsCollectionEnabled(final boolean enabled) {
    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            getFirebaseAnalytics().setAnalyticsCollectionEnabled(enabled);
          }
        });
  }

  /**
   * Wraps call to {@link FirebaseAnalytics#setUserId(String)}.
   *
   * <p>Note: Call will only be executed once an application {@link Context} is registered using
   * {@link GoogleAnalyticsAdapter#registerContext(Context)}.
   */
  public void setUserId(@Nullable String rawUserId) {
    final String userId = trimString(rawUserId, MAX_USER_ID_VALUE_LENGTH);
    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            getFirebaseAnalytics().setUserId(userId);
          }
        });
  }

  /**
   * Wraps call to {@link FirebaseAnalytics#setSessionTimeoutDuration(long)}.
   *
   * <p>Note: Call will only be executed once an application {@link Context} is registered using
   * {@link GoogleAnalyticsAdapter#registerContext(Context)}.
   */
  public void setSessionTimeoutDuration(@IntRange(from = 1) final long milliseconds) {
    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            getFirebaseAnalytics().setSessionTimeoutDuration(milliseconds);
          }
        });
  }

  private FirebaseAnalytics getFirebaseAnalytics() {
    synchronized (firebaseReference) {
      return firebaseReference.get();
    }
  }

  /**
   * Returns event, param, or user property name formatted to conform to Google Analytics for
   * Firebase public name rules.
   *
   * <p>A valid public name:
   *
   * <p>- is non-null
   *
   * <p>- does not start with a reserved prefix ({@link
   * GoogleAnalyticsAdapter#RESERVED_NAME_PREFIXES})
   *
   * <p>- starts with an alphabetic character
   *
   * <p>- only contains alphanumeric characters and underscores ("_")
   *
   * <p>- is not longer than the maximum length
   *
   * <p>- is not contained in the restricted internal names list
   *
   * @param name name to be sanitized
   * @param maxLength maximum character length of the output name
   * @param defaultValue name returned if name is null or empty
   * @param restrictedNames list of names reserved by Google Analytics for Firebase
   * @return formatted public name or null
   */
  @Nullable
  String sanitizeName(
      @Nullable String name,
      @IntRange(from = 1) int maxLength,
      @Nullable String defaultValue,
      @NonNull List<String> restrictedNames) {
    if (name == null || name.isEmpty()) {
      return defaultValue;
    }

    StringBuilder builder = new StringBuilder();

    // If name doesn't start with an alphabetic character, prepend prefix to name (additional
    // characters may cause string to exceed the length limit and cause the name to be truncated)
    boolean prependPrefix = !isValidNameCharacter(name.codePointAt(0), true);

    // Remove invalid characters (only letters, digits, and _ are allowed)
    int offset = 0;
    boolean lastAddedValidCharacter = true;
    boolean hasAlphaNumericChar = false;
    while (offset < name.length()) {
      int codepoint = name.codePointAt(offset);
      if (isValidNameCharacter(codepoint, false)) {
        builder.appendCodePoint(codepoint);
        hasAlphaNumericChar |= codepoint != (int) '_';
        lastAddedValidCharacter = true;
      } else if (lastAddedValidCharacter) {
        // Add a single underscore for an invalid subsequence
        builder.append('_');
        lastAddedValidCharacter = false;
      }
      offset += Character.charCount(codepoint);
    }

    if (!hasAlphaNumericChar) {
      // Drop names that collapse to a series of underscores
      return null;
    }

    if (prependPrefix
        || nameStartsWithReservedPrefix(builder.toString())
        || restrictedNames.contains(builder.toString())) {
      // If name starts with "google_", "firebase_", or "ga_", or is a restricted internal name,
      // prepend prefix to name (additional characters may cause string to exceed the length limit
      // and cause the name to be truncated)
      builder.insert(0, sanitizedNamePrefix);
    }

    return trimString(builder.toString(), maxLength);
  }

  /**
   * Returns {@code true} if name is a properly formatted Google Analytics for Firebase public name.
   *
   * <p>A valid public name:
   *
   * <p>- is non-null
   *
   * <p>- does not start with a reserved prefix ({@link
   * GoogleAnalyticsAdapter#RESERVED_NAME_PREFIXES})
   *
   * <p>- starts with an alphabetic character
   *
   * <p>- only contains alphanumeric characters and underscores ("_")
   *
   * @param name name to be checked
   * @return if name is properly formatted
   */
  static boolean isValidPublicNameFormat(String name) {
    if (name == null || name.length() == 0 || nameStartsWithReservedPrefix(name)) {
      return false;
    }
    for (int offset = 0; offset < name.length(); offset++) {
      if (!isValidNameCharacter(name.codePointAt(offset), offset == 0)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if name is a properly formatted Google Analytics for Firebase public name.
   *
   * <p>A valid public name:
   *
   * <p>- is non-null
   *
   * <p>- does not start with a reserved prefix ({@link
   * GoogleAnalyticsAdapter#RESERVED_NAME_PREFIXES})
   *
   * <p>- starts with an alphabetic character
   *
   * <p>- only contains alphanumeric characters and underscores ("_")
   *
   * <p>- is not longer than the maximum length
   *
   * <p>- is not contained in the restricted internal names list
   *
   * @param name name to be checked
   * @param maxLength maximum character length of the output name
   * @param restrictedNames list of names reserved by Google Analytics for Firebase
   * @return if name is properly formatted
   */
  static boolean isValidPublicName(
      String name, @IntRange(from = 1) int maxLength, @NonNull List<String> restrictedNames) {
    return isValidPublicNameFormat(name)
        && !isStringTooLong(name, maxLength)
        && !restrictedNames.contains(name);
  }

  /** Returns {@code true} if the {@code String} value exceeds the maximum character length. */
  static boolean isStringTooLong(@NonNull String string, @IntRange(from = 1) int maxLength) {
    return string.codePointCount(0, string.length()) > maxLength;
  }

  /**
   * If {@code String} value exceeds maximum character length, returns the {@code String} value
   * truncated to the maximum character length, otherwise returns the original value.
   */
  static String trimString(@Nullable String string, @IntRange(from = 1) int maxLength) {
    if (string == null) {
      return null;
    }

    if (maxLength <= 0 || string.isEmpty()) {
      return "";
    } else if (isStringTooLong(string, maxLength)) {
      return string.substring(0, string.offsetByCodePoints(0, maxLength));
    } else {
      return string;
    }
  }

  /**
   * Maps predefined event, param, and user property constants of a wrapped SDK to their Google
   * Analytics for Firebase equivalent.
   *
   * @param map map of other wrapped SDK event, param, or user property name (key) to the GA4F
   *     equivalent (value)
   * @param name wrapped SDK event, param, or user property name
   * @return GA4F equivalent name (if present in map), otherwise the supplied name
   */
  static String mapName(@NonNull Map<String, String> map, @Nullable String name) {
    if (map.containsKey(name)) {
      return map.get(name);
    }
    return name;
  }

  /**
   * Returns {@code true} if codepoint can be used in an event, param, user property name, otherwise
   * returns {@code false}. All event, param, user property names may only contain alphanumeric
   * characters and underscores ("_"), and must start with an alphabetic character.
   */
  static boolean isValidNameCharacter(int codepoint, boolean isFirstCharacter) {
    if (isFirstCharacter) {
      return Character.isLetter(codepoint);
    } else {
      return Character.isLetterOrDigit(codepoint) || codepoint == (int) '_';
    }
  }

  /**
   * Returns {@code true} if name starts with a reserved prefix ({@link
   * GoogleAnalyticsAdapter#RESERVED_NAME_PREFIXES}), otherwise returns {@code false}.
   */
  static boolean nameStartsWithReservedPrefix(@Nullable String name) {
    if (name == null) {
      return false;
    }
    for (String prefix : RESERVED_NAME_PREFIXES) {
      if (name.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Converts param value to supported type ({@code String}, {@code long}, {@code double}) and adds
   * key-value pair to the supplied bundle. If value is converted to a {@code String}, it will be
   * truncated if it exceeds the maximum length.
   */
  static Bundle addParamToBundle(
      @NonNull Bundle bundle,
      @NonNull String key,
      @NonNull Object value,
      @IntRange(from = 1) int maxStringLength) {
    String stringValueToAdd = null;
    if (value instanceof Float) {
      bundle.putDouble(key, ((Float) value).doubleValue());
    } else if (value instanceof Double) {
      bundle.putDouble(key, ((Double) value));
    } else if (value instanceof Byte) {
      bundle.putLong(key, ((Byte) value).longValue());
    } else if (value instanceof Short) {
      bundle.putLong(key, ((Short) value).longValue());
    } else if (value instanceof Integer) {
      bundle.putLong(key, ((Integer) value).longValue());
    } else if (value instanceof Long) {
      bundle.putLong(key, (Long) value);
    } else if (value instanceof Character
        || value instanceof String
        || value instanceof CharSequence) {
      stringValueToAdd = String.valueOf(value);
    } else if (value instanceof float[]) {
      stringValueToAdd = Arrays.toString((float[]) value);
    } else if (value instanceof double[]) {
      stringValueToAdd = Arrays.toString((double[]) value);
    } else if (value instanceof byte[]) {
      stringValueToAdd = Arrays.toString((byte[]) value);
    } else if (value instanceof short[]) {
      stringValueToAdd = Arrays.toString((short[]) value);
    } else if (value instanceof int[]) {
      stringValueToAdd = Arrays.toString((int[]) value);
    } else if (value instanceof long[]) {
      stringValueToAdd = Arrays.toString((long[]) value);
    } else if (value instanceof char[]) {
      stringValueToAdd = Arrays.toString((char[]) value);
    } else if (value instanceof Object[]) {
      stringValueToAdd = Arrays.toString((Object[]) value);
    } else {
      stringValueToAdd = value.toString();
    }

    if (stringValueToAdd != null) {
      bundle.putString(key, trimString(stringValueToAdd, maxStringLength));
    }

    return bundle;
  }
}

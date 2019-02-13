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

package java.com.google.firebase.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.analytics.GoogleAnalyticsAdapter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GoogleAnalyticsAdapterTest {
  // Default timeout to wait for the GoogleAnalyticsAdapter executor to complete all queued tasks.
  private static final long DEFAULT_TIMEOUT_MS = 500;

  GoogleAnalyticsAdapter adapter;

  @Mock
  com.google.firebase.analytics.FirebaseAnalytics firebaseAnalytics;
  @Mock Context context;

  @Before
  public void setup() {
    FirebaseAnalytics.setInstance(firebaseAnalytics);

    adapter =
        new GoogleAnalyticsAdapter.Builder()
            .setWrappedSdkName("wrapper")
            .setSanitizedNamePrefix("wrapper_")
            .build();
  }

  @Test
  public void testLogEvent() {
    adapter.registerContext(context);

    Map<String, Object> params = new HashMap<>();
    params.put("param1", "value");
    params.put("param2", 42L);
    params.put("param3", 3.14D);
    adapter.logEvent("test_event", params);
    waitForIdle();

    ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
    verify(firebaseAnalytics).logEvent(ArgumentMatchers.eq("test_event"), bundleCaptor.capture());

    Bundle foundParams = new Bundle(bundleCaptor.getValue());
    assertEquals(4, foundParams.size());
    assertEquals("value", foundParams.getString("param1"));
    assertEquals(42L, foundParams.getLong("param2"));
    assertEquals(3.14D, foundParams.getDouble("param3"), 0);
    assertEquals("wrapper", foundParams.getString("api_wrapper"));
  }

  @Test
  public void testLogEvent_NullName() {
    adapter.registerContext(context);

    adapter.logEvent(null, new HashMap<String, Object>());
    waitForIdle();

    ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
    verify(firebaseAnalytics)
        .logEvent(ArgumentMatchers.eq("unnamed_event"), bundleCaptor.capture());

    Bundle foundParams = new Bundle(bundleCaptor.getValue());
    assertEquals(1, foundParams.size());
    assertEquals("wrapper", foundParams.getString("api_wrapper"));
  }

  @Test
  public void testLogEvent_NullParams() {
    adapter.registerContext(context);

    adapter.logEvent("test_event", null);
    waitForIdle();

    ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
    verify(firebaseAnalytics).logEvent(ArgumentMatchers.eq("test_event"), bundleCaptor.capture());

    Bundle foundParams = new Bundle(bundleCaptor.getValue());
    assertEquals(1, foundParams.size());
    assertEquals("wrapper", foundParams.getString("api_wrapper"));
  }

  @Test
  public void testLogEvent_AlreadyHasApiWrapperParam() {
    adapter.registerContext(context);

    Map<String, Object> params = new HashMap<>();
    params.put("api_wrapper", "custom_value");
    adapter.logEvent("test_event", params);
    waitForIdle();

    ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
    verify(firebaseAnalytics).logEvent(ArgumentMatchers.eq("test_event"), bundleCaptor.capture());

    Bundle foundParams = new Bundle(bundleCaptor.getValue());
    assertEquals(2, foundParams.size());
    assertEquals("wrapper", foundParams.getString("api_wrapper"));
    assertEquals("custom_value", foundParams.getString("wrapper_api_wrapper"));
  }

  @Test
  public void testLogEvent_ValidParameterCount() {
    adapter.registerContext(context);

    Map<String, Object> params = new HashMap<>();
    for (int i = 0; i < 24; i++) {
      params.put("param" + i, "value" + i);
    }
    adapter.logEvent("valid_params", params);
    waitForIdle();

    ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
    verify(firebaseAnalytics).logEvent(ArgumentMatchers.eq("valid_params"), bundleCaptor.capture());

    Bundle foundParams = new Bundle(bundleCaptor.getValue());
    assertEquals(25, foundParams.size());
    assertEquals("wrapper", foundParams.getString("api_wrapper"));
    for (int i = 0; i < 24; i++) {
      assertEquals("value" + i, foundParams.getString("param" + i));
    }
  }

  @Test
  public void testLogEvent_TooManyParameters() {
    adapter.registerContext(context);

    Map<String, Object> params = new HashMap<>();
    for (int i = 0; i < 25; i++) {
      params.put("param" + i, "value" + i);
    }
    adapter.logEvent("valid_params", params);
    waitForIdle();

    ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
    verify(firebaseAnalytics).logEvent(ArgumentMatchers.eq("valid_params"), bundleCaptor.capture());

    Bundle foundParams = new Bundle(bundleCaptor.getValue());
    assertEquals(25, foundParams.size());
    assertEquals("wrapper", foundParams.getString("api_wrapper"));
  }

  @Test
  public void testUserProperty() {
    adapter.registerContext(context);

    adapter.setUserProperty("firebase_exp_2", "exp_data");
    waitForIdle();
    verify(firebaseAnalytics)
        .setUserProperty(
            ArgumentMatchers.eq("wrapper_firebase_exp_2"), ArgumentMatchers.eq("exp_data"));
  }

  @Test
  public void testUserProperty_BlacklistedFirebaseExperiment() {
    adapter.registerContext(context);

    adapter.setUserProperty("firebase_exp_2", "exp_data");
    waitForIdle();
    verify(firebaseAnalytics)
        .setUserProperty(
            ArgumentMatchers.eq("wrapper_firebase_exp_2"), ArgumentMatchers.eq("exp_data"));
  }

  @Test
  public void testUserProperty_NullName() {
    adapter.registerContext(context);

    adapter.setUserProperty(null, "value");
    waitForIdle();
    verify(firebaseAnalytics)
        .setUserProperty(
            ArgumentMatchers.eq("unnamed_user_property"), ArgumentMatchers.eq("value"));
  }

  @Test
  public void testUserProperty_NullValue() {
    adapter.registerContext(context);

    adapter.setUserProperty("name", null);
    waitForIdle();
    verify(firebaseAnalytics)
        .setUserProperty(ArgumentMatchers.eq("name"), ArgumentMatchers.eq((String) null));
  }

  @Test
  public void testSetCollectionEnabled() {
    adapter.registerContext(context);

    adapter.setAnalyticsCollectionEnabled(true);
    waitForIdle();
    verify(firebaseAnalytics).setAnalyticsCollectionEnabled(ArgumentMatchers.eq(true));
  }

  @Test
  public void testSetUserId_ValidUserId() {
    adapter.registerContext(context);

    adapter.setUserId("user1234");
    waitForIdle();
    verify(firebaseAnalytics).setUserId(ArgumentMatchers.eq("user1234"));
  }

  @Test
  public void testSetUserId_NullUserId() {
    adapter.registerContext(context);

    adapter.setUserId(null);
    waitForIdle();
    verify(firebaseAnalytics).setUserId(ArgumentMatchers.eq((String) null));
  }

  @Test
  public void testSetSessionTimeoutDuration() {
    adapter.registerContext(context);

    adapter.setSessionTimeoutDuration(42L);
    waitForIdle();
    verify(firebaseAnalytics).setSessionTimeoutDuration(ArgumentMatchers.eq(42L));
  }

  @Test
  public void testSanitizeName() {
    assertEquals("name", adapter.sanitizeName("name", 100, null, Collections.<String>emptyList()));
    assertEquals(
        "wrapper__name", adapter.sanitizeName("_name", 100, null, Collections.<String>emptyList()));
  }

  @Test
  public void testSanitizeName_EmptyName() {
    assertEquals(
        "empty_name",
        adapter.sanitizeName(null, 100, "empty_name", Collections.<String>emptyList()));
    assertEquals(
        "empty_name", adapter.sanitizeName("", 100, "empty_name", Collections.<String>emptyList()));
    assertEquals(
        null, adapter.sanitizeName(" ", 100, "empty_name", Collections.<String>emptyList()));
    assertEquals(
        null, adapter.sanitizeName("      ", 100, "empty_name", Collections.<String>emptyList()));
  }

  @Test
  public void testSanitizeName_RestrictedPrefix() {
    assertEquals(
        "wrapper_ga_name",
        adapter.sanitizeName("ga_name", 100, null, Collections.<String>emptyList()));
    assertEquals(
        "wrapper_firebase_name",
        adapter.sanitizeName("firebase_name", 100, null, Collections.<String>emptyList()));
    assertEquals(
        "wrapper_google_name",
        adapter.sanitizeName("google_name", 100, null, Collections.<String>emptyList()));
    assertEquals(
        "GA_name", adapter.sanitizeName("GA_name", 100, null, Collections.<String>emptyList()));
    assertEquals(
        "Firebase_name",
        adapter.sanitizeName("Firebase_name", 100, null, Collections.<String>emptyList()));
    assertEquals(
        "Google_name",
        adapter.sanitizeName("Google_name", 100, null, Collections.<String>emptyList()));
  }

  @Test
  public void testSanitizeName_Truncation() {
    assertEquals("name", adapter.sanitizeName("name", 4, null, Collections.<String>emptyList()));
    assertEquals("nam", adapter.sanitizeName("name", 3, null, Collections.<String>emptyList()));
    assertEquals(
        "wrapper_ga_n", adapter.sanitizeName("ga_name", 12, null, Collections.<String>emptyList()));
  }

  @Test
  public void testSanitizeName_InvalidCharacters() {
    assertEquals(
        "name_", adapter.sanitizeName("name?", 100, null, Collections.<String>emptyList()));
    assertEquals(
        "name_", adapter.sanitizeName("name?!?!", 100, null, Collections.<String>emptyList()));
    assertEquals(
        "my_name", adapter.sanitizeName("my-name", 7, null, Collections.<String>emptyList()));
    assertEquals(
        "my_name", adapter.sanitizeName("my-!?!*name", 7, null, Collections.<String>emptyList()));
    assertEquals(
        "wrapper_ga_name",
        adapter.sanitizeName("ga!name", 100, null, Collections.<String>emptyList()));
    assertEquals(
        null, adapter.sanitizeName("!@#$%^&*()", 9, null, Collections.<String>emptyList()));
    assertEquals(
        null, adapter.sanitizeName("!@#$%^&*()", 8, null, Collections.<String>emptyList()));
    assertEquals(null, adapter.sanitizeName("!_!", 8, null, Collections.<String>emptyList()));
    assertEquals(
        "wrapper___a_", adapter.sanitizeName("!_a!", 100, null, Collections.<String>emptyList()));
  }

  @Test
  public void testSanitizeName_RestrictedNames() {
    assertEquals("wrapper_name", adapter.sanitizeName("name", 100, null, Arrays.asList("name")));
    assertEquals("name", adapter.sanitizeName("name", 100, null, Collections.<String>emptyList()));
    assertEquals("name", adapter.sanitizeName("name", 100, null, Arrays.asList("restricted_name")));
    assertEquals("name", adapter.sanitizeName("name", 100, null, Arrays.asList("Name")));
  }

  @Test
  public void testIsValidPublicNameFormat() {
    assertTrue(GoogleAnalyticsAdapter.isValidPublicNameFormat("name"));
    assertFalse(GoogleAnalyticsAdapter.isValidPublicNameFormat("_name"));
    assertFalse(GoogleAnalyticsAdapter.isValidPublicNameFormat("name?"));
  }

  @Test
  public void testIsValidPublicNameFormat_EmptyName() {
    assertFalse(GoogleAnalyticsAdapter.isValidPublicNameFormat(null));
    assertFalse(GoogleAnalyticsAdapter.isValidPublicNameFormat(""));
    assertFalse(GoogleAnalyticsAdapter.isValidPublicNameFormat(" "));
  }

  @Test
  public void testIsValidPublicNameFormat_RestrictedPrefix() {
    assertFalse(GoogleAnalyticsAdapter.isValidPublicNameFormat("ga_name"));
    assertFalse(GoogleAnalyticsAdapter.isValidPublicNameFormat("firebase_name"));
    assertFalse(GoogleAnalyticsAdapter.isValidPublicNameFormat("google_name"));
    assertTrue(GoogleAnalyticsAdapter.isValidPublicNameFormat("GA_name"));
    assertTrue(GoogleAnalyticsAdapter.isValidPublicNameFormat("Firebase_name"));
    assertTrue(GoogleAnalyticsAdapter.isValidPublicNameFormat("Google_name"));
  }

  @Test
  public void testIsValidPublicName_TooLong() {
    assertTrue(
        GoogleAnalyticsAdapter.isValidPublicName("name", 4, Collections.<String>emptyList()));
    assertFalse(
        GoogleAnalyticsAdapter.isValidPublicName("name", 3, Collections.<String>emptyList()));
  }

  @Test
  public void testIsValidPublicName_RestrictedNames() {
    assertFalse(GoogleAnalyticsAdapter.isValidPublicName("name", 100, Arrays.asList("name")));
    assertTrue(
        GoogleAnalyticsAdapter.isValidPublicName("name", 100, Collections.<String>emptyList()));
    assertTrue(
        GoogleAnalyticsAdapter.isValidPublicName("name", 100, Arrays.asList("restricted_name")));
    assertTrue(GoogleAnalyticsAdapter.isValidPublicName("name", 100, Arrays.asList("Name")));
  }

  @Test
  public void testIsStringTooLong() {
    assertFalse(GoogleAnalyticsAdapter.isStringTooLong("name", 10));
    assertFalse(GoogleAnalyticsAdapter.isStringTooLong("name", 4));
    assertTrue(GoogleAnalyticsAdapter.isStringTooLong("name", 3));
  }

  @Test
  public void testTrimString() {
    assertEquals("name", GoogleAnalyticsAdapter.trimString("name", 10));
    assertEquals("name", GoogleAnalyticsAdapter.trimString("name", 4));

    assertEquals("nam", GoogleAnalyticsAdapter.trimString("name", 3));
    assertEquals("", GoogleAnalyticsAdapter.trimString("name", 0));
    assertEquals("", GoogleAnalyticsAdapter.trimString("name", -1));
  }

  @Test
  public void testMapName() {
    Map<String, String> map = new HashMap<>();
    map.put("name1", "mapped1");
    map.put("name2", "mapped2");
    map.put("name3", null);

    assertEquals("mapped1", GoogleAnalyticsAdapter.mapName(map, "name1"));
    assertEquals(null, GoogleAnalyticsAdapter.mapName(map, "name3"));
    assertEquals("name4", GoogleAnalyticsAdapter.mapName(map, "name4"));
  }

  @Test
  public void testMapName_NullInput() {
    Map<String, String> map = new HashMap<>();
    map.put("name1", "mapped1");
    map.put("name2", "mapped2");
    assertEquals(null, GoogleAnalyticsAdapter.mapName(map, null));
  }

  @Test
  public void testAddToBundle_AddAsDouble() {
    assertEquals(
        3.14D,
        GoogleAnalyticsAdapter.addParamToBundle(new Bundle(), "key", 3.14, 100).getDouble("key"),
        0.000001);
    assertEquals(
        3.14D,
        GoogleAnalyticsAdapter.addParamToBundle(new Bundle(), "key", 3.14D, 100).getDouble("key"),
        0);
  }

  @Test
  public void testAddToBundle_AddAsLong() {
    assertEquals(
        -5L,
        GoogleAnalyticsAdapter.addParamToBundle(new Bundle(), "key", (byte) -5, 100).get("key"));
    assertEquals(
        -5L,
        GoogleAnalyticsAdapter.addParamToBundle(new Bundle(), "key", (short) -5, 100).get("key"));
    assertEquals(
        -5L, GoogleAnalyticsAdapter.addParamToBundle(new Bundle(), "key", -5, 100).get("key"));
    assertEquals(
        -5L, GoogleAnalyticsAdapter.addParamToBundle(new Bundle(), "key", -5L, 100).get("key"));
  }

  @Test
  public void testAddToBundle_AddAsString() {
    assertEquals(
        "a", GoogleAnalyticsAdapter.addParamToBundle(new Bundle(), "key", 'a', 100).get("key"));

    assertEquals(
        "hello",
        GoogleAnalyticsAdapter.addParamToBundle(new Bundle(), "key", "hello", 100).get("key"));
    assertEquals(
        "h", GoogleAnalyticsAdapter.addParamToBundle(new Bundle(), "key", "hello", 1).get("key"));

    assertEquals(
        "[a, b]",
        GoogleAnalyticsAdapter.addParamToBundle(new Bundle(), "key", new String[] {"a", "b"}, 100)
            .get("key"));
    assertEquals(
        "[a, 42, 3.14]",
        GoogleAnalyticsAdapter.addParamToBundle(
                new Bundle(), "key", new Object[] {"a", 42L, 3.14D}, 100)
            .get("key"));
  }

  private void waitForIdle() {
    waitForIdle(DEFAULT_TIMEOUT_MS);
  }

  private void waitForIdle(long timeoutMs) {
    final Semaphore lock = new Semaphore(0);
    adapter.executor.execute(
        new Runnable() {
          @Override
          public void run() {
            lock.release();
          }
        });
    try {
      lock.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ex) {
      throw new IllegalStateException(ex);
    }
  }
}

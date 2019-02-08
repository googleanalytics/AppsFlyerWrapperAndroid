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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Application;
import android.content.Context;
import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.AppsFlyerTrackingRequestListener;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** API tests to verify wrapper calls correctly call down to {@link AppsFlyerLib} */
@RunWith(MockitoJUnitRunner.class)
public class AppsFlyerLibWrapperTest {

  @Mock GoogleAnalyticsAdapter googleAnalyticsAdapter;
  @Mock AppsFlyerLib appsFlyerLib;
  AppsFlyerLibWrapper wrapper = new MockAppsFlyerLibWrapper();

  @Before
  public void setup() {
    doReturn(googleAnalyticsAdapter)
        .when(googleAnalyticsAdapter)
        .registerContext(ArgumentMatchers.<Context>any());
  }

  @Test
  public void testSetAppUserId() {
    wrapper.setAppUserId("id");
    verify(appsFlyerLib).setAppUserId(ArgumentMatchers.eq("id"));
    verify(googleAnalyticsAdapter).setUserId(ArgumentMatchers.eq("id"));
  }

  @Test
  public void testSetCustomerUserId() {
    wrapper.setCustomerUserId("id");
    verify(appsFlyerLib).setCustomerUserId(ArgumentMatchers.eq("id"));
    verify(googleAnalyticsAdapter).setUserId(ArgumentMatchers.eq("id"));
  }

  @Test
  public void testSetCustomerUserId_NullId() {
    wrapper.setCustomerUserId(null);
    verify(appsFlyerLib).setCustomerUserId(ArgumentMatchers.eq((String) null));
    verify(googleAnalyticsAdapter).setUserId(ArgumentMatchers.eq((String) null));
  }

  @Test
  public void testSetCustomerIdAndTrack() {
    Context context = mock(Context.class);
    wrapper.setCustomerIdAndTrack("id", context);

    verify(appsFlyerLib)
        .setCustomerIdAndTrack(ArgumentMatchers.eq("id"), ArgumentMatchers.eq(context));

    verify(googleAnalyticsAdapter).registerContext(context);
    verify(googleAnalyticsAdapter).setUserId(ArgumentMatchers.eq("id"));
  }

  @Test
  public void testInit_WithKeyAndListenerAndContext() {
    AppsFlyerConversionListener listener = mock(AppsFlyerConversionListener.class);
    Context context = mock(Context.class);

    doReturn(appsFlyerLib)
        .when(appsFlyerLib)
        .init(
            anyString(),
            ArgumentMatchers.<AppsFlyerConversionListener>any(),
            ArgumentMatchers.<Context>any());

    AppsFlyerLib lib = wrapper.init("key", listener, context);
    verify(appsFlyerLib)
        .init(
            ArgumentMatchers.eq("key"),
            ArgumentMatchers.eq(listener),
            ArgumentMatchers.eq(context));
    assertEquals(appsFlyerLib, lib);

    verify(googleAnalyticsAdapter).registerContext(context);
  }

  @Test
  public void testStartTracking_WithApplication() {
    Application application = mock(Application.class);

    wrapper.startTracking(application);

    verify(appsFlyerLib).startTracking(ArgumentMatchers.eq(application));
    verify(googleAnalyticsAdapter).registerContext(application);
  }

  @Test
  public void testStartTracking_WithApplicationAndKey() {
    Application application = mock(Application.class);
    wrapper.startTracking(application, "key");

    verify(appsFlyerLib)
        .startTracking(ArgumentMatchers.eq(application), ArgumentMatchers.eq("key"));

    verify(googleAnalyticsAdapter).registerContext(application);
  }

  @Test
  public void testStartTracking_WithApplicationAndKeyAndListener() {
    Application application = mock(Application.class);
    AppsFlyerTrackingRequestListener listener = mock(AppsFlyerTrackingRequestListener.class);
    wrapper.startTracking(application, "key", listener);

    verify(appsFlyerLib)
        .startTracking(
            ArgumentMatchers.eq(application),
            ArgumentMatchers.eq("key"),
            ArgumentMatchers.eq(listener));

    verify(googleAnalyticsAdapter).registerContext(application);
  }

  @Test
  public void testSetCurrencyCode() {
    wrapper.setCurrencyCode("USD");
    verify(appsFlyerLib).setCurrencyCode(ArgumentMatchers.eq("USD"));
  }

  @Test
  public void testTrackEvent() {
    Context context = mock(Context.class);
    Map<String, Object> params = new HashMap<>();
    params.put("key1", "value");
    params.put("key2", 1L);
    params.put("key3", 3.14D);
    wrapper.trackEvent(context, "name", new HashMap<>(params));

    verify(appsFlyerLib)
        .trackEvent(
            ArgumentMatchers.eq(context), ArgumentMatchers.eq("name"), ArgumentMatchers.eq(params));

    verify(googleAnalyticsAdapter).registerContext(context);
    verify(googleAnalyticsAdapter)
        .logEvent(ArgumentMatchers.eq("name"), ArgumentMatchers.eq(params));
  }

  @Test
  public void testTrackEvent_WithRevenue() {
    wrapper.setCurrencyCode("USD");

    Context context = mock(Context.class);
    Map<String, Object> params = new HashMap<>();
    params.put(AFInAppEventParameterName.REVENUE, 1L);
    wrapper.trackEvent(context, "name", new HashMap<>(params));

    verify(appsFlyerLib)
        .trackEvent(
            ArgumentMatchers.eq(context), ArgumentMatchers.eq("name"), ArgumentMatchers.eq(params));

    Map<String, Object> gaParamsWithCurrency = new HashMap<>();
    gaParamsWithCurrency.put(AFInAppEventParameterName.REVENUE, 1L);
    gaParamsWithCurrency.put(AFInAppEventParameterName.CURRENCY, "USD");
    verify(googleAnalyticsAdapter)
        .logEvent(ArgumentMatchers.eq("name"), ArgumentMatchers.eq(gaParamsWithCurrency));
  }

  @Test
  public void testTrackEvent_WithRevenueButNoCurrency() {
    Context context = mock(Context.class);
    Map<String, Object> params = new HashMap<>();
    params.put(AFInAppEventParameterName.REVENUE, 1L);
    wrapper.trackEvent(context, "name", new HashMap<>(params));

    verify(appsFlyerLib)
        .trackEvent(
            ArgumentMatchers.eq(context), ArgumentMatchers.eq("name"), ArgumentMatchers.eq(params));

    Map<String, Object> gaParamsWithCurrency = new HashMap<>();
    gaParamsWithCurrency.put(AFInAppEventParameterName.REVENUE, 1L);
    gaParamsWithCurrency.put(AFInAppEventParameterName.CURRENCY, "USD");
    verify(googleAnalyticsAdapter)
        .logEvent(ArgumentMatchers.eq("name"), ArgumentMatchers.eq(gaParamsWithCurrency));
  }

  @Test
  public void testTrackEvent_WithRevenueAndCurrency() {
    wrapper.setCurrencyCode("USD");

    Context context = mock(Context.class);
    Map<String, Object> params = new HashMap<>();
    params.put(AFInAppEventParameterName.REVENUE, 1L);
    params.put(AFInAppEventParameterName.CURRENCY, "CAD");
    wrapper.trackEvent(context, "name", new HashMap<>(params));

    verify(appsFlyerLib)
        .trackEvent(
            ArgumentMatchers.eq(context), ArgumentMatchers.eq("name"), ArgumentMatchers.eq(params));

    verify(googleAnalyticsAdapter)
        .logEvent(ArgumentMatchers.eq("name"), ArgumentMatchers.eq(params));
  }

  @Test
  public void testSetPreinstallAttribution() {
    wrapper.setPreinstallAttribution("mediaSource", "campaign", "siteId");

    verify(appsFlyerLib)
        .setPreinstallAttribution(
            ArgumentMatchers.eq("mediaSource"),
            ArgumentMatchers.eq("campaign"),
            ArgumentMatchers.eq("siteId"));

    Map<String, Object> params = new HashMap<>();
    params.put("source", "mediaSource");
    params.put("campaign", "campaign");
    params.put("medium", "siteId");
    verify(googleAnalyticsAdapter)
        .logEvent(
            ArgumentMatchers.eq("campaign_details"),
            ArgumentMatchers.eq(params));
  }

  @Test
  public void testSetMinTimeBetweenSessions() {
    wrapper.setMinTimeBetweenSessions(42);
    verify(appsFlyerLib).setMinTimeBetweenSessions(ArgumentMatchers.eq(42));
    verify(googleAnalyticsAdapter).setSessionTimeoutDuration(ArgumentMatchers.eq(42000L));
  }

  class MockAppsFlyerLibWrapper extends AppsFlyerLibWrapper {
    @Override
    GoogleAnalyticsAdapter getGoogleAnalytics() {
      return googleAnalyticsAdapter;
    }

    @Override
    AppsFlyerLib getAppsFlyerLib() {
      return appsFlyerLib;
    }
  }
}

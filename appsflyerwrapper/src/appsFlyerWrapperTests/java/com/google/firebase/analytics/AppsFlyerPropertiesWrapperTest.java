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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.content.Context;
import com.appsflyer.AppsFlyerProperties;
import com.google.firebase.analytics.AppsFlyerPropertiesWrapper;
import com.google.firebase.analytics.GoogleAnalyticsAdapter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** API tests to verify wrapper calls correctly call down to {@link AppsFlyerProperties} */
@RunWith(MockitoJUnitRunner.class)
public class AppsFlyerPropertiesWrapperTest {

  @Mock
  GoogleAnalyticsAdapter googleAnalyticsAdapter;
  @Mock AppsFlyerProperties appsFlyerProperties;
  AppsFlyerPropertiesWrapper wrapper = new MockAppsFlyerPropertiesWrapper();

  @Before
  public void setup() {
    doReturn(googleAnalyticsAdapter)
        .when(googleAnalyticsAdapter)
        .registerContext(ArgumentMatchers.<Context>any());
  }

  @Test
  public void testSetString() {
    wrapper.set("key", "value");
    verify(appsFlyerProperties).set(ArgumentMatchers.eq("key"), ArgumentMatchers.eq("value"));
    verify(googleAnalyticsAdapter)
        .setUserProperty(ArgumentMatchers.eq("key"), ArgumentMatchers.eq("value"));
  }

  @Test
  public void testSetString_UserId() {
    wrapper.set(AppsFlyerProperties.APP_USER_ID, "id");
    verify(appsFlyerProperties)
        .set(ArgumentMatchers.eq(AppsFlyerProperties.APP_USER_ID), ArgumentMatchers.eq("id"));
    verify(googleAnalyticsAdapter).setUserId(ArgumentMatchers.eq("id"));
  }

  @Test
  public void testSetStringArray() {
    wrapper.set("key", new String[] {"a", "b", "c"});
    verify(appsFlyerProperties)
        .set(ArgumentMatchers.eq("key"), ArgumentMatchers.eq(new String[] {"a", "b", "c"}));
    verify(googleAnalyticsAdapter)
        .setUserProperty(ArgumentMatchers.eq("key"), ArgumentMatchers.eq("[a, b, c]"));
  }

  @Test
  public void testSetInt() {
    wrapper.set("key", 42);
    verify(appsFlyerProperties).set(ArgumentMatchers.eq("key"), ArgumentMatchers.eq(42));
    verify(googleAnalyticsAdapter)
        .setUserProperty(ArgumentMatchers.eq("key"), ArgumentMatchers.eq("42"));
  }

  @Test
  public void testSetLong() {
    wrapper.set("key", 1234L);
    verify(appsFlyerProperties).set(ArgumentMatchers.eq("key"), ArgumentMatchers.eq(1234L));
    verify(googleAnalyticsAdapter)
        .setUserProperty(ArgumentMatchers.eq("key"), ArgumentMatchers.eq("1234"));
  }

  @Test
  public void testSetBoolean() {
    wrapper.set("key", true);
    verify(appsFlyerProperties).set(ArgumentMatchers.eq("key"), ArgumentMatchers.eq(true));
    verify(googleAnalyticsAdapter)
        .setUserProperty(ArgumentMatchers.eq("key"), ArgumentMatchers.eq("true"));
  }

  class MockAppsFlyerPropertiesWrapper extends AppsFlyerPropertiesWrapper {
    @Override
    GoogleAnalyticsAdapter getGoogleAnalytics() {
      return googleAnalyticsAdapter;
    }

    @Override
    AppsFlyerProperties getAppsFlyerProperties() {
      return appsFlyerProperties;
    }
  }
}

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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

/**
 * Mock FirebaseAnalytics to inspect API calls for unit tests. Automatic mock generation with
 * Mockito does not work because FirebaseAnalytics is marked as final.
 */
public abstract class FirebaseAnalytics {
  private static FirebaseAnalytics instance;

  public static void setInstance(FirebaseAnalytics instance) {
    FirebaseAnalytics.instance = instance;
  }

  public static FirebaseAnalytics getInstance(Context context) {
    return instance;
  }

  public abstract void logEvent(
      @NonNull @Size(min = 1, max = 40) String name, @Nullable Bundle params);

  public abstract void setUserProperty(
      @NonNull @Size(min = 1, max = 24) String name, @Nullable @Size(max = 36) String value);

  public abstract void setAnalyticsCollectionEnabled(boolean enabled);

  public abstract void setUserId(@Nullable String id);

  public abstract void setSessionTimeoutDuration(long milliseconds);
}

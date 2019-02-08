package com.wrapper.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFInAppEventType;
import com.appsflyer.AppsFlyerProperties;
import com.google.firebase.analytics.AppsFlyerLibWrapper;
import com.google.firebase.analytics.AppsFlyerPropertiesWrapper;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);


    // Initializing AppsFlyer
    AppsFlyerLibWrapper.getInstance().startTracking(getApplication(), "your-dev-key");


    // Tracking Predefined Events
    Map<String, Object> levelAchievedParams = new HashMap<>();
    levelAchievedParams.put(AFInAppEventParameterName.LEVEL, 4);
    levelAchievedParams.put(AFInAppEventParameterName.USER_SCORE, 100);
    AppsFlyerLibWrapper.getInstance()
        .trackEvent(this, AFInAppEventType.LEVEL_ACHIEVED, levelAchievedParams);


    // Tracking Events w/ Currency
    Map<String, Object> purchaseParams = new HashMap<>();
    purchaseParams.put(AFInAppEventParameterName.REVENUE, 87);

    AppsFlyerLibWrapper.getInstance().setCurrencyCode("USD");
    // --- or ---
    // purchaseParams.put(AFInAppEventParameterName.CURRENCY, "USD");

    AppsFlyerLibWrapper.getInstance().trackEvent(this, AFInAppEventType.PURCHASE, purchaseParams);


    // Tracking Custom Events
    Map<String, Object> customEventParams = new HashMap<>();
    customEventParams.put("param_name", "param_value");
    AppsFlyerLibWrapper.getInstance().trackEvent(this, "custom_event", customEventParams);


    // Setting Customer ID
    AppsFlyerLibWrapper.getInstance().setCustomerUserId("user_id");
    // --- or ---
    // AppsFlyerPropertiesWrapper.getInstance().set(AppsFlyerProperties.APP_USER_ID, "user_id");


    // Setting Custom Property
    AppsFlyerPropertiesWrapper.getInstance().set("favorite_color", "green");


    // Setting Pre-Install Attribution
    AppsFlyerLibWrapper.getInstance().setPreinstallAttribution("media", "campaign", "site");
  }
}

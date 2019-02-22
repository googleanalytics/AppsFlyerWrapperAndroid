# Google Analytics for Firebase Wrapper for AppsFlyer

_Copyright (c) 2019 Google Inc. All rights reserved._

The __Google Analytics for Firebase__ wrapper for AppsFlyer allows developers to
easily send information to both the AppsFlyer and Google Analytics for Firebase
backends.

## Using the Wrapper

In order to use the AppsFlyer wrapper:

1.  [Follow the steps here](https://firebase.google.com/docs/analytics/ios/start)
    to set up the Google Analytics for Firebase SDK in your app.
2.  Copy the source files inside the AppsFlyerWrapper directory (ignoring
    subdirectories) into your project.
3.  Replace supported references to `AppsFlyerLib.getInstance()` with
    `AppsFlyerLibWrapper.getInstance()`, and references to
    `AppsFlyerProperties.getInstance()` with
    `AppsFlyerPropertiesWrapper.getInstance()`.

Some methods are not supported by the wrapper. For these methods, directly call
the base implementation in `AppsFlyerLib.getInstance()` or
`AppsFlyerProperties.getInstance()`.

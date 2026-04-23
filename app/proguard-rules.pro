-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.raulshma.minkoa.data.SlotContent { *; }
-keep class com.raulshma.minkoa.data.SlotContent$App { *; }
-keep class com.raulshma.minkoa.data.SlotContent$Widget { *; }
-keep class com.raulshma.minkoa.data.SavedLayout { *; }

-keepclassmembers class com.raulshma.minkoa.notifications.LauncherNotificationListener {
    public void onListenerConnected();
    public void onNotificationPosted(**);
    public void onNotificationRemoved(**);
}

-keep class com.raulshma.minkoa.gestures.LauncherDeviceAdminReceiver { *; }

-dontwarn kotlinx.coroutines.**

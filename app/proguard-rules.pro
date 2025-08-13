# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}

# Preserve JavaScript interface
-dontnote android.webkit.JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve JavascriptInterface annotations and members in all web-bridge classes
-keepattributes JavascriptInterface
-keep class lv.lvrtc.webbridge.** { *; }
-keepclassmembers class lv.lvrtc.webbridge.** {
    @android.webkit.JavascriptInterface <methods>;
}

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keep class lv.lvrtc.networklogic.model.** { *; }

-keep enum * { *; }

# Biometric
-keep class lv.lvrtc.commonfeature.features.biometric.** { *; }

# Keep navigation config related classes
-keep class lv.lvrtc.navigation.** { *; }
-keep class lv.lvrtc.uilogic.navigation.** { *; }

# Keep all classes referenced in BiometricUiConfig
-keepclassmembers class * {
    @lv.lvrtc.uilogic.serializer.* *;
}

-keep interface lv.lvrtc.uilogic.serializer.UiSerializableParser
-keep interface lv.lvrtc.uilogic.serializer.UiSerializable
-keepclassmembers class * implements lv.lvrtc.uilogic.serializer.adapter.SerializableAdapterType { *; }
-keepclassmembers class * implements lv.lvrtc.uilogic.serializer.UiSerializableParser { *; }
-keepclassmembers class * implements lv.lvrtc.uilogic.serializer.UiSerializable { *; }

# Keep Gson TypeToken and related classes
-keep class com.google.common.reflect.TypeToken { *; }
-keep class * extends com.google.common.reflect.TypeToken

# Keep generic signature of TypeToken (important for reflection)
-keepattributes Signature

# Keep Gson classes used with reflection
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

-keep class lv.lvrtc.corelogic.controller.PaymentRequestData { *; }

-keep class lv.lvrtc.corelogic.controller.PaymentDetails { *; }
-keepclassmembers class lv.lvrtc.corelogic.controller.PaymentDetails {
    <fields>;
}

-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.SuspendLambda {
    <fields>;
    <methods>;
}

# Keep navigation and web bridge classes
-keep class lv.lvrtc.webbridge.** { *; }
-keep class lv.lvrtc.webfeature.** { *; }

# Keep PaymentStatusState and related classes
-keep class lv.lvrtc.presentationfeature.interactor.PaymentStatusState** { *; }
-keep class lv.lvrtc.presentationfeature.interactor.PaymentPresentationInteractor** { *; }

# Keep navigation command classes
-keep class lv.lvrtc.webfeature.ui.NavigationCommand** { *; }
-keep class lv.lvrtc.webfeature.ui.WebEffect** { *; }

# Keep all sealed classes and their subclasses
-keep class * extends lv.lvrtc.presentationfeature.interactor.PaymentStatusState { *; }

# Prevent obfuscation of suspend function parameters
-keepclassmembers class * {
    kotlin.coroutines.Continuation *;
}

# Keep Flow related classes
-keep class kotlinx.coroutines.flow.** { *; }
-keepclassmembers class * {
    kotlinx.coroutines.flow.FlowCollector *;
}

-keep class androidx.appcompat.app.AppCompatDelegateImpl** { *; }
-keep class androidx.appcompat.app.** { *; }

# Keep payment-related classes from being obfuscated
-keep class lv.lvrtc.presentationfeature.bridge.** { *; }

-keep class lv.lvrtc.presentationfeature.interactor.PaymentPresentationInteractorImpl** { *; }
-keep class lv.lvrtc.presentationfeature.interactor.PaymentPresentationInteractorImpl$** { *; }

# Keep PaymentStatusResponse and related network models
-keep class lv.lvrtc.networklogic.api.payment.PaymentStatusResponse { *; }
-keepclassmembers class lv.lvrtc.networklogic.api.payment.PaymentStatusResponse {
    <fields>;
}

# Keep PaymentApiClient
-keep class lv.lvrtc.networklogic.api.payment.PaymentApiClient { *; }

# Keep coroutine continuation classes used in payment polling
-keep class lv.lvrtc.presentationfeature.interactor.PaymentPresentationInteractorImpl$pollPaymentStatus$** { *; }

# Keep all lambda classes in presentation feature
-keep class lv.lvrtc.presentationfeature.interactor.** extends kotlin.coroutines.jvm.internal.SuspendLambda { *; }

# Additional coroutines rules for payment polling
-keepclassmembers class lv.lvrtc.presentationfeature.interactor.PaymentPresentationInteractorImpl {
    kotlin.coroutines.Continuation *;
    kotlinx.coroutines.flow.Flow *;
}

# Bouncycastle
-keep class org.bouncycastle.** { *; }

-dontwarn com.eygraber.uri.JvmUriKt

-keep class com.eygraber.uri.** { *; }
-keepclassmembers class com.eygraber.uri.** { *; }

# Core Libs
-keep class com.nimbusds.jwt.**{ *; }
-keep class com.nimbusds.jose.**{ *; }

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Koin DI framework
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }
-dontwarn org.koin.**

# Keep Koin generated modules
-keep class org.koin.ksp.generated.** { *; }
-keepclassmembers class org.koin.ksp.generated.** { *; }

# Keep Koin annotations
-keep class org.koin.core.annotation.** { *; }
-keepattributes *Annotation*

# Keep Koin scopes and scope management
-keep class org.koin.core.scope.** { *; }
-keepclassmembers class org.koin.core.scope.** { *; }

# Keep presentation feature specific classes
-keep class lv.lvrtc.presentationfeature.** { *; }
-keepclassmembers class lv.lvrtc.presentationfeature.** { *; }

# Keep generated Koin modules for presentation feature
-keep class **FeaturePresentationModule** { *; }
-keepclassmembers class **FeaturePresentationModule** { *; }

# Keep lambda expressions used in Koin modules
-keepclassmembers class * {
    *** lambda$*(...);
}

# Keep synthetic methods and classes
-keep class **$$ExternalSyntheticLambda* { *; }
-keepclassmembers class **$$ExternalSyntheticLambda* { *; }

# Keep R8 lambda methods
-keep class **$r8$lambda$** { *; }
-keepclassmembers class **$r8$lambda$** { *; }

# Keep all factory methods in DI modules
-keepclassmembers class * {
    *** provide*(...);
}

# Keep Koin module builders and DSL
-keep class org.koin.dsl.** { *; }
-keepclassmembers class org.koin.dsl.** { *; }

# Keep Koin instance creation
-keep class org.koin.core.instance.** { *; }
-keepclassmembers class org.koin.core.instance.** { *; }

# Keep Koin registry
-keep class org.koin.core.registry.** { *; }
-keepclassmembers class org.koin.core.registry.** { *; }
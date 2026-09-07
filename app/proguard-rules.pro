# 🔥 PRODUCTION R8 RULES
-keep class neunix.dailychunk.MainActivity { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends androidx.appcompat.app.AppCompatActivity { *; }
-keep class * extends androidx.fragment.app.Fragment { *; }
-keep class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable { static final long serialVersionUID; }
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-keepclassmembers class * { public <init>(...); }
-keepclasseswithmembernames class * { native <methods>; }
-keepattributes Signature,Exceptions,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn kotlinx.**
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

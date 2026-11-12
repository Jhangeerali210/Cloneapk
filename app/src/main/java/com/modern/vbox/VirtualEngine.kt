package com.modern.vbox

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dalvik.system.DexClassLoader
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.URL
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Random
import java.util.UUID
import java.util.zip.ZipFile

data class DeviceProfile(
    var brand: String,
    var manufacturer: String,
    var model: String,
    var device: String,
    var product: String,
    var fingerprint: String
) {
    fun serialize(): String = "$brand~$manufacturer~$model~$device~$product~$fingerprint"

    companion object {
        fun deserialize(raw: String): DeviceProfile {
            val p = raw.split("~")
            return if (p.size >= 6) {
                DeviceProfile(p[0], p[1], p[2], p[3], p[4], p[5])
            } else {
                presets().first()
            }
        }

        fun presets(): List<DeviceProfile> = listOf(
            DeviceProfile(
                brand = "google",
                manufacturer = "Google",
                model = "Pixel 9 Pro XL",
                device = "komodo",
                product = "komodo",
                fingerprint = "google/komodo/komodo:15/AP3A.241005.015/12366759:user/release-keys"
            ),
            DeviceProfile(
                brand = "samsung",
                manufacturer = "samsung",
                model = "SM-S928B",
                device = "e3q",
                product = "e3qxeea",
                fingerprint = "samsung/e3qxeea/e3q:15/AP3A.240905.015.A2/S928BXXU3AXFE:user/release-keys"
            ),
            DeviceProfile(
                brand = "Xiaomi",
                manufacturer = "Xiaomi",
                model = "24030PN60G",
                device = "aurora",
                product = "aurora_global",
                fingerprint = "Xiaomi/aurora_global/aurora:15/AQ3A.240812.002/OS2.0.5.0.VNAMIXM:user/release-keys"
            ),
            DeviceProfile(
                brand = "OnePlus",
                manufacturer = "OnePlus",
                model = "CPH2581",
                device = "OP5929L1",
                product = "CPH2581",
                fingerprint = "OnePlus/CPH2581/OP5929L1:15/AP3A.240617.008/U.R4T3.1699c97_1:user/release-keys"
            )
        )
    }
}

data class SimWifiConfig(
    var enabled: Boolean = true,
    var carrierName: String = "Jazz 4G",
    var mccMnc: String = "41001",
    var countryIso: String = "pk",
    var imei: String = generateRandomImei(),
    var imsi: String = generateRandomImsi("41001"),
    var iccid: String = generateRandomIccid(),
    var wifiMac: String = generateRandomMac(),
    var wifiBssid: String = generateRandomMac(),
    var wifiSsid: String = "PTCL_FlashFiber_5G"
) {
    fun serialize(): String =
        "$enabled~$carrierName~$mccMnc~$countryIso~$imei~$imsi~$iccid~$wifiMac~$wifiBssid~$wifiSsid"

    companion object {
        fun deserialize(raw: String): SimWifiConfig {
            val p = raw.split("~")
            return if (p.size >= 10) {
                SimWifiConfig(
                    enabled = p[0].toBoolean(),
                    carrierName = p[1],
                    mccMnc = p[2],
                    countryIso = p[3],
                    imei = p[4],
                    imsi = p[5],
                    iccid = p[6],
                    wifiMac = p[7],
                    wifiBssid = p[8],
                    wifiSsid = p[9]
                )
            } else {
                presets().first()
            }
        }

        fun presets(): List<SimWifiConfig> = listOf(
            SimWifiConfig(true, "Jazz 4G", "41001", "pk", generateRandomImei(), generateRandomImsi("41001"), generateRandomIccid(), generateRandomMac(), generateRandomMac(), "PTCL_FlashFiber_5G"),
            SimWifiConfig(true, "Zong 4G", "41004", "pk", generateRandomImei(), generateRandomImsi("41004"), generateRandomIccid(), generateRandomMac(), generateRandomMac(), "Zong_MBB_5G"),
            SimWifiConfig(true, "T-Mobile US", "310260", "us", generateRandomImei(), generateRandomImsi("310260"), generateRandomIccid(), generateRandomMac(), generateRandomMac(), "Xfinity_5G"),
            SimWifiConfig(true, "Vodafone UK", "23415", "gb", generateRandomImei(), generateRandomImsi("23415"), generateRandomIccid(), generateRandomMac(), generateRandomMac(), "VirginMedia_5G"),
            SimWifiConfig(true, "Etisalat UAE", "42402", "ae", generateRandomImei(), generateRandomImsi("42402"), generateRandomIccid(), generateRandomMac(), generateRandomMac(), "eLife_Home_5G")
        )

        fun generateRandomImei(): String {
            val r = Random()
            val sb = StringBuilder("35")
            for (i in 0 until 13) sb.append(r.nextInt(10))
            return sb.toString()
        }

        fun generateRandomImsi(mccMnc: String): String {
            val r = Random()
            val sb = StringBuilder(mccMnc.take(6).ifBlank { "41001" })
            while (sb.length < 15) sb.append(r.nextInt(10))
            return sb.toString()
        }

        fun generateRandomIccid(): String {
            val r = Random()
            val sb = StringBuilder("899201")
            while (sb.length < 19) sb.append(r.nextInt(10))
            return sb.toString()
        }

        fun generateRandomMac(): String {
            val r = Random()
            val b = ByteArray(6)
            r.nextBytes(b)
            b[0] = (b[0].toInt() and 0xFC or 0x02).toByte()
            return b.joinToString(":") { String.format("%02X", it) }
        }
    }
}

data class ProxyConfig(
    var enabled: Boolean = false,
    var type: String = "SOCKS",
    var host: String = "",
    var port: Int = 1080,
    var username: String = "",
    var password: String = ""
) {
    fun serialize(): String = "$enabled~$type~$host~$port~$username~$password"

    companion object {
        fun deserialize(raw: String): ProxyConfig {
            val p = raw.split("~")
            return if (p.size >= 6) {
                ProxyConfig(p[0].toBoolean(), p[1].ifBlank { "SOCKS" }, p[2], p[3].toIntOrNull() ?: 1080, p[4], p[5])
            } else ProxyConfig()
        }
    }
}

data class GpsConfig(
    var enabled: Boolean = false,
    var latitude: Double = 31.5204,
    var longitude: Double = 74.3587,
    var label: String = "Lahore, PK"
) {
    fun serialize(): String = "$enabled~$latitude~$longitude~$label"

    companion object {
        fun deserialize(raw: String): GpsConfig {
            val p = raw.split("~")
            return if (p.size >= 4) {
                GpsConfig(p[0].toBoolean(), p[1].toDoubleOrNull() ?: 31.5204, p[2].toDoubleOrNull() ?: 74.3587, p[3])
            } else GpsConfig()
        }

        fun presets(): List<GpsConfig> = listOf(
            GpsConfig(true, 31.5204, 74.3587, "Lahore, Pakistan"),
            GpsConfig(true, 33.6844, 73.0479, "Islamabad, Pakistan"),
            GpsConfig(true, 40.7128, -74.0060, "New York, USA"),
            GpsConfig(true, 51.5074, -0.1278, "London, UK"),
            GpsConfig(true, 25.2048, 55.2708, "Dubai, UAE"),
            GpsConfig(true, 35.6762, 139.6503, "Tokyo, Japan")
        )
    }
}

data class VUser(
    val id: Int,
    val name: String,
    val virtualAndroidId: String,
    var deviceProfile: DeviceProfile,
    var gmsEnabled: Boolean = false,
    var proxyConfig: ProxyConfig = ProxyConfig(),
    var gpsConfig: GpsConfig = GpsConfig(),
    var simWifiConfig: SimWifiConfig = SimWifiConfig.presets().first(),
    var systemUserId: Int = -1
)

data class XposedModuleItem(
    val appName: String,
    val packageName: String,
    val apkPath: String,
    val entryClass: String,
    val description: String,
    var isEnabled: Boolean
)

class StubActivity : Activity()

/**
 * Transparent Activity triggered when clicking a Pinned Home Screen Shortcut for a Cloned App
 */
class ShortcutProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VBoxCore.init(this)
        val userId = intent?.getIntExtra(VBoxCore.EXTRA_USER_ID, 0) ?: 0
        val pkgName = intent?.getStringExtra(VBoxCore.EXTRA_PKG_NAME)
        if (!pkgName.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Launching $pkgName in Space $userId...",
                Toast.LENGTH_SHORT
            ).show()
            VBoxCore.launchClonedApp(this, userId, pkgName)
        }
        finish()
    }
}

object VBoxCore {
    private const val TAG = "ModernVBox"
    const val EXTRA_TARGET_INTENT = "_vbox_target_intent_"
    const val EXTRA_USER_ID = "_vbox_user_id_"
    const val EXTRA_PKG_NAME = "_vbox_pkg_name_"

    private var isInitialized = false
    lateinit var hostContext: Context
        private set

    var activeUserId: Int = 0
    var activeVirtualPackage: String? = null
    var isRootAvailable: Boolean = false
        private set

    // Engine Modes: "HYBRID_AUTO" (Root Multi-User + Hook), "IN_PROCESS_HOOK" (Pure Non-Root Hook)
    var preferredEngineMode: String = "HYBRID_AUTO"

    private val classLoaderCache = mutableMapOf<String, DexClassLoader>()
    private val resourcesCache = mutableMapOf<String, Resources>()
    private val appInstanceCache = mutableMapOf<String, Application>()

    fun init(context: Context) {
        if (isInitialized) return
        hostContext = context.applicationContext

        installGlobalCrashShield()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("L")
            } catch (e: Throwable) {
                Log.e(TAG, "HiddenApiBypass warning", e)
            }
        }

        VEnvironment.ensureDefaultUser()
        hookActivityTaskManager()
        hookActivityThreadInstrumentation()
        hookPackageManagerForStealth()
        LocationSpoofer.installLocationBinderHook(hostContext)
        TelephonyAndWifiSpoofer.installTelephonyAndWifiHooks(hostContext)

        Thread {
            isRootAvailable = RootShell.checkRoot()
        }.start()

        isInitialized = true
    }

    /**
     * Prevents unhandled guest app exceptions from killing the cloner process
     */
    private fun installGlobalCrashShield() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "CrashShield caught exception in ${thread.name}: ${throwable.message}", throwable)
            if (thread == Looper.getMainLooper().thread) {
                try {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            hostContext,
                            "Shielded Guest Error: ${throwable.javaClass.simpleName} - ${throwable.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Looper.loop()
                } catch (e: Throwable) {
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            }
        }
    }

    /**
     * Master Crash-Free App Launcher for Android 15, 16, 17 & Kernel 6.6
     */
    fun launchClonedApp(activity: Activity, userId: Int, pkgName: String) {
        if (!VEnvironment.isAppCloned(userId, pkgName)) {
            Toast.makeText(activity, "Pehle is App ko Space $userId mein Clone karein!", Toast.LENGTH_SHORT).show()
            return
        }

        activeUserId = userId
        activeVirtualPackage = pkgName

        val vUser = VEnvironment.getUsers().find { it.id == userId }
        if (vUser != null) {
            DeviceSpoofer.applySpoofForUser(vUser)
            ProxyEngine.applyProxyForUser(vUser)
        }
        TelephonyAndWifiSpoofer.applySimSystemPropertiesForActiveUser()
        LocationSpoofer.installLocationBinderHook(hostContext)
        KeyboxEngine.reloadCertificates()

        Thread {
            // 1. If Root (KernelSU Next / Magisk) is available & HYBRID_AUTO is enabled, use Kernel 6.6 Native Multi-User Container
            val rootOk = isRootAvailable || RootShell.checkRoot().also { isRootAvailable = it }
            if (rootOk && preferredEngineMode == "HYBRID_AUTO") {
                val sysLaunchOk = KernelMultiUserBridge.launchInSystemContainer(userId, pkgName, vUser)
                if (sysLaunchOk) return@Thread
            }

            // 2. Non-Root / In-Process Split-APK Hook Engine Launch
            activity.runOnUiThread {
                launchViaInProcessHookOrSandbox(activity, userId, pkgName)
            }
        }.start()
    }

    private fun launchViaInProcessHookOrSandbox(activity: Activity, userId: Int, pkgName: String) {
        try {
            val entryComponent = resolveMainLauncherComponent(userId, pkgName)
            if (entryComponent == null) {
                Toast.makeText(activity, "Launcher Activity nahi mili ($pkgName)", Toast.LENGTH_LONG).show()
                return
            }

            val targetIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = entryComponent
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_PKG_NAME, pkgName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }

            // Route directly to StubActivity so it works reliably on Android 15, 16, and 17 even if IActivityClient blocks Binder proxy
            val stubIntent = Intent(activity, StubActivity::class.java).apply {
                putExtra(EXTRA_TARGET_INTENT, targetIntent)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_PKG_NAME, pkgName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(stubIntent)
        } catch (e: Throwable) {
            Log.e(TAG, "In-process launch fallback for $pkgName", e)
            fallbackSafeIntentLaunch(activity, userId, pkgName)
        }
    }

    fun resolveMainLauncherComponent(userId: Int, pkgName: String): ComponentName? {
        val pm = hostContext.packageManager
        // 1. First ask PackageManager for real MAIN/LAUNCHER activity
        val hostLaunchIntent = pm.getLaunchIntentForPackage(pkgName)
        if (hostLaunchIntent?.component != null) {
            return hostLaunchIntent.component
        }

        // 2. If it's an External APK not installed on host, parse base.apk activities
        val apkPath = VEnvironment.getClonedApkFile(userId, pkgName).absolutePath
        val pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES)
        val actName = pkgInfo?.activities?.firstOrNull { it.exported }?.name
            ?: pkgInfo?.activities?.firstOrNull()?.name
        return if (actName != null) ComponentName(pkgName, actName) else null
    }

    fun fallbackSafeIntentLaunch(context: Context, userId: Int, pkgName: String) {
        try {
            val launchIntent = hostContext.packageManager.getLaunchIntentForPackage(pkgName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.putExtra(EXTRA_USER_ID, userId)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(
                    context,
                    "External APK ($pkgName) ko system container mein chalane ke liye Root enable karein ya install karein.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Throwable) {
            Toast.makeText(context, "Launch Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    private fun hookActivityTaskManager() {
        try {
            val atmClass = Class.forName("android.app.ActivityTaskManager")
            val singletonField = atmClass.getDeclaredField("IActivityTaskManagerSingleton")
            singletonField.isAccessible = true
            replaceSingletonInstance(singletonField.get(null), "android.app.IActivityTaskManager")
        } catch (_: Throwable) {}

        try {
            // Android 12 - 17 ActivityClient singleton hook
            val acClass = Class.forName("android.app.ActivityClient")
            val singletonField = acClass.getDeclaredField("INTERFACE_SINGLETON")
            singletonField.isAccessible = true
            replaceSingletonInstance(singletonField.get(null), "android.app.IActivityClientController")
        } catch (_: Throwable) {}
    }

    @SuppressLint("PrivateApi")
    private fun replaceSingletonInstance(singleton: Any?, interfaceName: String) {
        if (singleton == null) return
        val singletonClass = Class.forName("android.util.Singleton")
        val getMethod = singletonClass.getDeclaredMethod("get")
        getMethod.isAccessible = true
        val originalInstance = getMethod.invoke(singleton) ?: return
        if (Proxy.isProxyClass(originalInstance.javaClass)) return

        val interfaceClass = Class.forName(interfaceName)
        val proxyInstance = Proxy.newProxyInstance(
            interfaceClass.classLoader,
            arrayOf(interfaceClass),
            BinderStartActivityProxy(originalInstance)
        )

        val mInstanceField = singletonClass.getDeclaredField("mInstance")
        mInstanceField.isAccessible = true
        mInstanceField.set(singleton, proxyInstance)
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    private fun hookActivityThreadInstrumentation() {
        try {
            val atClass = Class.forName("android.app.ActivityThread")
            val currentAtMethod = atClass.getDeclaredMethod("currentActivityThread")
            currentAtMethod.isAccessible = true
            val activityThread = currentAtMethod.invoke(null)

            val instField = atClass.getDeclaredField("mInstrumentation")
            instField.isAccessible = true
            val baseInstrumentation = instField.get(activityThread) as Instrumentation

            if (baseInstrumentation !is VInstrumentation) {
                instField.set(activityThread, VInstrumentation(baseInstrumentation))
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Instrumentation hook error", e)
        }
    }

    @SuppressLint("PrivateApi")
    private fun hookPackageManagerForStealth() {
        try {
            val atClass = Class.forName("android.app.ActivityThread")
            val currentAtMethod = atClass.getDeclaredMethod("currentActivityThread")
            currentAtMethod.isAccessible = true
            val activityThread = currentAtMethod.invoke(null)

            val sPmField = atClass.getDeclaredField("sPackageManager")
            sPmField.isAccessible = true
            val originalPm = sPmField.get(null) ?: return
            if (Proxy.isProxyClass(originalPm.javaClass)) return

            val iPmClass = Class.forName("android.content.pm.IPackageManager")
            val proxyPm = Proxy.newProxyInstance(
                iPmClass.classLoader,
                arrayOf(iPmClass),
                PackageManagerStealthProxy(originalPm)
            )
            sPmField.set(null, proxyPm)

            val pm = hostContext.packageManager
            val mPmField = pm.javaClass.getDeclaredField("mPM")
            mPmField.isAccessible = true
            mPmField.set(pm, proxyPm)
        } catch (_: Throwable) {}
    }

    /**
     * Split-APK Aware DexClassLoader for Android 15, 16, 17
     */
    fun getOrCreateClassLoader(userId: Int, pkgName: String): DexClassLoader {
        val key = "${userId}_$pkgName"
        classLoaderCache[key]?.let { return it }

        val allApks = VEnvironment.getAllApkFilesForApp(userId, pkgName)
        allApks.forEach { apk ->
            apk.setReadable(true, false)
            apk.setWritable(false, false)
        }

        val dexPath = allApks.joinToString(File.pathSeparator) { it.absolutePath }
        val odexDir = VEnvironment.getOdexDir(userId, pkgName)
        val libDir = VEnvironment.getNativeLibDir(userId, pkgName)

        // Use hostContext.classLoader as parent so AndroidX / AppCompat classes don't throw ClassCastException
        val loader = DexClassLoader(
            dexPath,
            odexDir.absolutePath,
            libDir.absolutePath,
            hostContext.classLoader
        )
        classLoaderCache[key] = loader
        return loader
    }

    /**
     * Unified Split-APK + Host Resources Merger (Fixes Theme.AppCompat crashes on Android 15/16/17)
     */
    @SuppressLint("DiscouragedPrivateApi")
    fun getOrCreateResources(userId: Int, pkgName: String): Resources {
        val key = "${userId}_$pkgName"
        resourcesCache[key]?.let { return it }

        return try {
            val assetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
            val addAssetPath = AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
            addAssetPath.isAccessible = true

            // Add all guest APKs (base.apk + split_config.*.apk)
            val allApks = VEnvironment.getAllApkFilesForApp(userId, pkgName)
            for (apk in allApks) {
                addAssetPath.invoke(assetManager, apk.absolutePath)
            }
            // Also add host sourceDir so Theme.MaterialComponents / AppCompat fallback attributes resolve cleanly
            addAssetPath.invoke(assetManager, hostContext.applicationInfo.sourceDir)

            val hostRes = hostContext.resources
            @Suppress("DEPRECATION")
            val mergedRes = Resources(assetManager, hostRes.displayMetrics, hostRes.configuration)
            resourcesCache[key] = mergedRes
            mergedRes
        } catch (e: Throwable) {
            hostContext.resources
        }
    }

    fun clearCacheFor(userId: Int, pkgName: String) {
        val key = "${userId}_$pkgName"
        classLoaderCache.remove(key)
        resourcesCache.remove(key)
        appInstanceCache.remove(key)
    }

    fun getOrCreateVirtualApplication(
        userId: Int,
        pkgName: String,
        classLoader: ClassLoader,
        baseContext: Context
    ): Application? {
        val key = "${userId}_$pkgName"
        appInstanceCache[key]?.let { return it }

        return try {
            val apkPath = VEnvironment.getClonedApkFile(userId, pkgName).absolutePath
            val pkgInfo = hostContext.packageManager.getPackageArchiveInfo(apkPath, 0)
            val appClassName = pkgInfo?.applicationInfo?.className ?: "android.app.Application"
            val appClass = classLoader.loadClass(appClassName)
            val app = appClass.getDeclaredConstructor().newInstance() as Application

            val attachMethod = Application::class.java.getDeclaredMethod("attach", Context::class.java)
            attachMethod.isAccessible = true
            attachMethod.invoke(app, baseContext)
            app.onCreate()

            appInstanceCache[key] = app
            app
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * Root Shell & Kernel 6.6 Native Multi-User Container Bridge
 * Ensures 100% crash-free execution for complex Split-APK apps when KernelSU Next / Magisk is granted
 */
object RootShell {
    fun checkRoot(): Boolean {
        val out = exec("id")
        return out.contains("uid=0")
    }

    fun exec(command: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val errReader = BufferedReader(InputStreamReader(p.errorStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) sb.append(line).append("\n")
            while (errReader.readLine().also { line = it } != null) sb.append(line).append("\n")
            p.waitFor()
            sb.toString().trim()
        } catch (e: Throwable) {
            "ERR: ${e.message}"
        }
    }
}

object KernelMultiUserBridge {
    fun ensureSystemUserForVirtualSpace(userId: Int, spaceName: String): Int {
        if (userId == 0) return 0
        val users = VEnvironment.getUsers()
        val vUser = users.find { it.id == userId }

        val rawUsers = RootShell.exec("pm list users")
        if (vUser != null && vUser.systemUserId > 0 && rawUsers.contains("UserInfo{${vUser.systemUserId}:")) {
            return vUser.systemUserId
        }

        // Check if a system user with matching name already exists
        val cleanName = "VBox_$userId"
        val existingMatch = Regex("""UserInfo\{(\d+):$cleanName""").find(rawUsers)
        if (existingMatch != null) {
            val foundId = existingMatch.groupValues[1].toIntOrNull() ?: -1
            if (foundId > 0) {
                VEnvironment.updateSystemUserId(userId, foundId)
                return foundId
            }
        }

        // Create a real Android 15/16/17 isolated Multi-User container
        val createOut = RootShell.exec("pm create-user \"$cleanName\"")
        val idMatch = Regex("""id\s+(\d+)""").find(createOut)
        val createdSysId = idMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1
        if (createdSysId > 0) {
            RootShell.exec("am start-user $createdSysId")
            VEnvironment.updateSystemUserId(userId, createdSysId)
            return createdSysId
        }
        return -1
    }

    fun launchInSystemContainer(userId: Int, pkgName: String, vUser: VUser?): Boolean {
        val sysUid = if (userId == 0) {
            // For Space 0, if user wants isolated clone separate from owner 0, map to dedicated VBox_0 user
            ensureSystemUserForVirtualSpace(99, "VBox_Space0")
        } else {
            ensureSystemUserForVirtualSpace(userId, vUser?.name ?: "Space_$userId")
        }

        if (sysUid <= 0) return false

        // 1. Start the system container user
        RootShell.exec("am start-user $sysUid")

        // 2. Ensure package is installed in sysUid (works for both installed apps & external APKs)
        val checkPkg = RootShell.exec("pm list packages --user $sysUid $pkgName")
        if (!checkPkg.contains("package:$pkgName")) {
            val installExisting = RootShell.exec("cmd package install-existing --user $sysUid $pkgName")
            if (!installExisting.contains("installed for user")) {
                val apkFile = VEnvironment.getClonedApkFile(userId, pkgName)
                if (apkFile.exists()) {
                    RootShell.exec("pm install --bypass-low-target-sdk-block --user $sysUid \"${apkFile.absolutePath}\"")
                }
            }
        }

        // 3. Apply Per-User Virtual Android ID (SSAID) inside container
        if (vUser != null) {
            RootShell.exec("settings put --user $sysUid secure android_id ${vUser.virtualAndroidId}")
            val hideAdb = VXposedEngine.getEnabledModulePackages(userId).contains(VXposedEngine.BUILTIN_HIDE_ADB)
            if (hideAdb) {
                RootShell.exec("settings put global adb_enabled 0")
            }
        }

        // 4. Resolve launcher activity and start inside isolated system user
        val comp = VBoxCore.resolveMainLauncherComponent(userId, pkgName)?.flattenToShortString()
        val launchCmd = if (comp != null) {
            "am start --user $sysUid -n $comp"
        } else {
            "monkey --pct-syskeys 0 -p $pkgName -c android.intent.category.LAUNCHER 1"
        }
        val res = RootShell.exec(launchCmd)
        return !res.contains("Error:") && !res.contains("Exception")
    }
}

/**
 * HOME SCREEN SHORTCUT CREATOR (With Space Badge Icon Generator)
 */
object ShortcutHelper {
    fun createShortcutForClonedApp(
        context: Context,
        userId: Int,
        pkgName: String,
        appLabel: String,
        appIcon: Drawable
    ) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Launcher Pin Shortcuts support nahi karta!", Toast.LENGTH_LONG).show()
            return
        }

        val badgedBitmap = createBadgedIconBitmap(appIcon, "S$userId")
        val shortcutIntent = Intent(context, ShortcutProxyActivity::class.java).apply {
            action = "com.modern.vbox.ACTION_LAUNCH_SHORTCUT"
            putExtra(VBoxCore.EXTRA_USER_ID, userId)
            putExtra(VBoxCore.EXTRA_PKG_NAME, pkgName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val shortcutId = "vbox_shortcut_u${userId}_$pkgName"
        val shortcutInfo = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel("$appLabel ($userId)")
            .setLongLabel("$appLabel [Space $userId]")
            .setIcon(IconCompat.createWithBitmap(badgedBitmap))
            .setIntent(shortcutIntent)
            .build()

        val requested = ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
        if (requested) {
            Toast.makeText(
                context,
                "Shortcut Created: '$appLabel ($userId)'! (HyperOS/MIUI par Home Screen Shortcut permission allow rakhein)",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(context, "Shortcut request failed. Check launcher permissions.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createBadgedIconBitmap(drawable: Drawable, badgeText: String): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(8, 8, size - 8, size - 8)
        drawable.draw(canvas)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
        }
        val rect = RectF(size * 0.52f, size * 0.60f, size.toFloat() - 4f, size.toFloat() - 4f)
        canvas.drawRoundRect(rect, 22f, 22f, badgePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(badgeText, rect.centerX(), textY, textPaint)

        return bitmap
    }

    fun openShortcutPermissionSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(
                context,
                "'Other Permissions' -> 'Home screen shortcuts' ko Allow kar dein.",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Throwable) {}
    }
}

/**
 * 1. PER-USER DEVICE & FINGERPRINT SPOOFER
 */
object DeviceSpoofer {
    fun applySpoofForUser(user: VUser) {
        val p = user.deviceProfile
        setBuildField("BRAND", p.brand)
        setBuildField("MANUFACTURER", p.manufacturer)
        setBuildField("MODEL", p.model)
        setBuildField("DEVICE", p.device)
        setBuildField("PRODUCT", p.product)
        setBuildField("FINGERPRINT", p.fingerprint)
        setBuildField("BOARD", p.device)
        setBuildField("HARDWARE", p.device)
        setBuildField("TAGS", "release-keys")
        setBuildField("TYPE", "user")

        injectVirtualAndroidId(user.virtualAndroidId)
    }

    private fun setBuildField(fieldName: String, value: String) {
        try {
            val field = Build::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(null, value)
        } catch (_: Throwable) {}
    }

    @SuppressLint("PrivateApi")
    private fun injectVirtualAndroidId(virtualSsaid: String) {
        try {
            val cacheField = Settings.Secure::class.java.getDeclaredField("sNameValueCache")
            cacheField.isAccessible = true
            val nameValueCache = cacheField.get(null) ?: return
            val valuesField = nameValueCache.javaClass.getDeclaredField("mValues")
            valuesField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val mValues = valuesField.get(nameValueCache) as? MutableMap<String, String>
            mValues?.put(Settings.Secure.ANDROID_ID, virtualSsaid)
        } catch (_: Throwable) {}
    }
}

/**
 * 2. PER-USER TELEPHONY & WI-FI MAC SPOOFER
 */
object TelephonyAndWifiSpoofer {
    fun installTelephonyAndWifiHooks(context: Context) {
        hookTelephonyBinder(context)
        hookWifiBinder(context)
        applySimSystemPropertiesForActiveUser()
    }

    fun applySimSystemPropertiesForActiveUser() {
        val user = VEnvironment.getUsers().find { it.id == VBoxCore.activeUserId } ?: return
        val cfg = user.simWifiConfig
        if (!cfg.enabled) return

        System.setProperty("gsm.sim.operator.numeric", cfg.mccMnc)
        System.setProperty("gsm.operator.numeric", cfg.mccMnc)
        System.setProperty("gsm.sim.operator.alpha", cfg.carrierName)
        System.setProperty("gsm.operator.alpha", cfg.carrierName)
        System.setProperty("gsm.sim.operator.iso-country", cfg.countryIso)
        System.setProperty("gsm.operator.iso-country", cfg.countryIso)
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    fun hookTelephonyBinder(context: Context) {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val iTelephonyClass = Class.forName("com.android.internal.telephony.ITelephony")
            if (tm != null) {
                val getITelMethod = TelephonyManager::class.java.getDeclaredMethod("getITelephony")
                getITelMethod.isAccessible = true
                val origTel = getITelMethod.invoke(tm)
                if (origTel != null && !Proxy.isProxyClass(origTel.javaClass)) {
                    val proxy = Proxy.newProxyInstance(
                        iTelephonyClass.classLoader,
                        arrayOf(iTelephonyClass),
                        TelephonyBinderProxy(origTel)
                    )
                    val sITelephonyField = TelephonyManager::class.java.getDeclaredField("sITelephony")
                    sITelephonyField.isAccessible = true
                    sITelephonyField.set(null, proxy)
                }
            }
        } catch (_: Throwable) {}
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    fun hookWifiBinder(context: Context) {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
            val mServiceField = WifiManager::class.java.getDeclaredField("mService")
            mServiceField.isAccessible = true
            val origService = mServiceField.get(wm) ?: return
            if (Proxy.isProxyClass(origService.javaClass)) return

            val iWifiClass = Class.forName("android.net.wifi.IWifiManager")
            val proxyWifi = Proxy.newProxyInstance(
                iWifiClass.classLoader,
                arrayOf(iWifiClass),
                WifiBinderProxy(origService)
            )
            mServiceField.set(wm, proxyWifi)
        } catch (_: Throwable) {}
    }

    fun patchWifiInfoObject(info: WifiInfo, cfg: SimWifiConfig) {
        try {
            val macField = WifiInfo::class.java.getDeclaredField("mMacAddress")
            macField.isAccessible = true
            macField.set(info, cfg.wifiMac)
        } catch (_: Throwable) {}

        try {
            val bssidField = WifiInfo::class.java.getDeclaredField("mBSSID")
            bssidField.isAccessible = true
            bssidField.set(info, cfg.wifiBssid)
        } catch (_: Throwable) {}
    }
}

class TelephonyBinderProxy(private val baseTelephony: Any) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>?): Any? {
        val cfg = VEnvironment.getUsers().find { it.id == VBoxCore.activeUserId }?.simWifiConfig
        if (cfg != null && cfg.enabled) {
            when (method.name) {
                "getDeviceId", "getDeviceIdWithFeature", "getImeiForSlot", "getMeidForSlot" -> return cfg.imei
                "getSubscriberId", "getSubscriberIdForSubscriber" -> return cfg.imsi
                "getSimSerialNumber", "getIccSerialNumber" -> return cfg.iccid
                "getNetworkOperator", "getSimOperator", "getSimOperatorNumeric" -> return cfg.mccMnc
                "getNetworkOperatorName", "getSimOperatorName" -> return cfg.carrierName
                "getNetworkCountryIso", "getSimCountryIso" -> return cfg.countryIso
                "hasIccCard" -> return true
                "getSimState" -> return TelephonyManager.SIM_STATE_READY
            }
        }
        return try {
            method.invoke(baseTelephony, *(args ?: emptyArray()))
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}

class WifiBinderProxy(private val baseWifiService: Any) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>?): Any? {
        val cfg = VEnvironment.getUsers().find { it.id == VBoxCore.activeUserId }?.simWifiConfig
        val result = try {
            method.invoke(baseWifiService, *(args ?: emptyArray()))
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
        if (cfg != null && cfg.enabled) {
            if (method.name == "getConnectionInfo" && result is WifiInfo) {
                TelephonyAndWifiSpoofer.patchWifiInfoObject(result, cfg)
            } else if (method.name == "getFactoryMacAddresses") {
                return arrayOf(cfg.wifiMac)
            }
        }
        return result
    }
}

/**
 * 3. PER-USER PROXY / SOCKS5 ROUTING ENGINE
 */
object ProxyEngine {
    private val defaultSelector: ProxySelector? = ProxySelector.getDefault()

    fun applyProxyForUser(user: VUser) {
        val cfg = user.proxyConfig
        if (!cfg.enabled || cfg.host.isBlank()) {
            clearProxy()
            return
        }
        val proxyType = if (cfg.type.equals("HTTP", true)) java.net.Proxy.Type.HTTP else java.net.Proxy.Type.SOCKS
        if (proxyType == java.net.Proxy.Type.SOCKS) {
            System.setProperty("socksProxyHost", cfg.host)
            System.setProperty("socksProxyPort", cfg.port.toString())
        } else {
            System.setProperty("http.proxyHost", cfg.host)
            System.setProperty("http.proxyPort", cfg.port.toString())
            System.setProperty("https.proxyHost", cfg.host)
            System.setProperty("https.proxyPort", cfg.port.toString())
        }
        if (cfg.username.isNotBlank()) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(cfg.username, cfg.password.toCharArray())
                }
            })
        } else {
            Authenticator.setDefault(null)
        }
        ProxySelector.setDefault(object : ProxySelector() {
            override fun select(uri: URI?): List<java.net.Proxy> {
                return listOf(java.net.Proxy(proxyType, InetSocketAddress.createUnresolved(cfg.host, cfg.port)))
            }
            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                defaultSelector?.connectFailed(uri, sa, ioe)
            }
        })
    }

    fun clearProxy() {
        System.clearProperty("socksProxyHost")
        System.clearProperty("socksProxyPort")
        System.clearProperty("http.proxyHost")
        System.clearProperty("http.proxyPort")
        System.clearProperty("https.proxyHost")
        System.clearProperty("https.proxyPort")
        Authenticator.setDefault(null)
        if (defaultSelector != null) ProxySelector.setDefault(defaultSelector)
    }

    fun testProxyConnection(cfg: ProxyConfig): String {
        if (!cfg.enabled || cfg.host.isBlank()) return "Proxy disabled."
        return try {
            val proxyType = if (cfg.type.equals("HTTP", true)) java.net.Proxy.Type.HTTP else java.net.Proxy.Type.SOCKS
            val netProxy = java.net.Proxy(proxyType, InetSocketAddress(cfg.host, cfg.port))
            val conn = (URL("https://api.ipify.org").openConnection(netProxy) as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
            }
            val ip = conn.inputStream.bufferedReader().readText().trim()
            "Connected! Exit IP: $ip (${cfg.type} ${cfg.host}:${cfg.port})"
        } catch (e: Throwable) {
            "Proxy Test Failed: ${e.message}"
        }
    }
}

/**
 * 4. PER-USER FAKE GPS LOCATION SPOOFER
 */
object LocationSpoofer {
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    fun installLocationBinderHook(context: Context) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val mServiceField = LocationManager::class.java.getDeclaredField("mService")
            mServiceField.isAccessible = true
            val originalService = mServiceField.get(lm) ?: return
            if (Proxy.isProxyClass(originalService.javaClass)) return

            val iLocClass = Class.forName("android.location.ILocationManager")
            val proxyService = Proxy.newProxyInstance(
                iLocClass.classLoader,
                arrayOf(iLocClass),
                LocationBinderProxy(originalService)
            )
            mServiceField.set(lm, proxyService)
        } catch (_: Throwable) {}
    }

    fun buildFakeLocation(provider: String, gps: GpsConfig): Location {
        val loc = Location(provider.ifBlank { LocationManager.GPS_PROVIDER })
        loc.latitude = gps.latitude
        loc.longitude = gps.longitude
        loc.altitude = 35.0
        loc.accuracy = 3.2f
        loc.time = System.currentTimeMillis()
        loc.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        stripMockFlags(loc)
        return loc
    }

    fun stripMockFlags(loc: Location) {
        try {
            val mockField = Location::class.java.getDeclaredField("mIsFromMockProvider")
            mockField.isAccessible = true
            mockField.setBoolean(loc, false)
        } catch (_: Throwable) {}

        try {
            val maskField = Location::class.java.getDeclaredField("mFieldsMask")
            maskField.isAccessible = true
            maskField.setInt(loc, maskField.getInt(loc) and 0x10.inv())
        } catch (_: Throwable) {}
    }
}

class LocationBinderProxy(private val baseLocationService: Any) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>?): Any? {
        val gps = VEnvironment.getUsers().find { it.id == VBoxCore.activeUserId }?.gpsConfig
        if (gps != null && gps.enabled) {
            when (method.name) {
                "getLastLocation", "getLastKnownLocation" -> {
                    val provider = args?.firstOrNull { it is String } as? String ?: LocationManager.GPS_PROVIDER
                    return LocationSpoofer.buildFakeLocation(provider, gps)
                }
                "isProviderEnabled", "isProviderEnabledForUser" -> return true
            }
        }
        val result = try {
            method.invoke(baseLocationService, *(args ?: emptyArray()))
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
        if (gps != null && gps.enabled && result is Location) {
            result.latitude = gps.latitude
            result.longitude = gps.longitude
            LocationSpoofer.stripMockFlags(result)
        }
        return result
    }
}

/**
 * 5. BLACKBOX GMS / GOOGLE PLAY STORE ENGINE
 */
object GmsManager {
    val GMS_PACKAGES = listOf(
        "com.google.android.gsf",
        "com.google.android.gms",
        "com.android.vending"
    )

    fun isGmsInstalledInSpace(userId: Int): Boolean {
        return VEnvironment.isAppCloned(userId, "com.google.android.gms") ||
                VEnvironment.getUsers().find { it.id == userId }?.gmsEnabled == true
    }

    fun installGmsToSpace(context: Context, userId: Int): String {
        val pm = context.packageManager
        var installedCount = 0
        val log = StringBuilder()

        for (pkg in GMS_PACKAGES) {
            try {
                pm.getApplicationInfo(pkg, 0)
                val res = VEnvironment.clonePackageToUser(context, userId, pkg)
                if (res.startsWith("Cloned")) {
                    installedCount++
                    log.append("✔ Mounted: $pkg\n")
                }
            } catch (_: PackageManager.NameNotFoundException) {
                log.append("• Skipped: $pkg\n")
            }
        }
        VEnvironment.setUserGmsState(userId, true)
        return "GMS & Play Services Active in Space $userId ($installedCount packages mounted)\n$log"
    }

    fun removeGmsFromSpace(userId: Int): String {
        for (pkg in GMS_PACKAGES) VEnvironment.removeClonedApp(userId, pkg)
        VEnvironment.setUserGmsState(userId, false)
        return "GMS unlinked from Space $userId"
    }

    fun isGmsIntent(intent: Intent): Boolean {
        val pkg = intent.component?.packageName ?: intent.`package` ?: ""
        val action = intent.action ?: ""
        return pkg.startsWith("com.google.android.gms") ||
                pkg == "com.android.vending" ||
                action.contains("com.google.android.gms.auth")
    }
}

/**
 * 6. TRICKYSTORE & KEYBOX.XML ENGINE
 */
object KeyboxEngine {
    val parsedCertificates = mutableListOf<X509Certificate>()
    private val localKeyboxFile: File get() = File(VEnvironment.rootDir, "keybox.xml")

    fun saveKeyboxXml(xmlContent: String): String {
        localKeyboxFile.writeText(xmlContent.trim())
        val count = reloadCertificates()
        return "Keybox saved! Parsed $count X.509 Certificates."
    }

    fun getSavedKeyboxXml(): String {
        return if (localKeyboxFile.exists()) localKeyboxFile.readText() else SAMPLE_KEYBOX_TEMPLATE
    }

    fun reloadCertificates(): Int {
        parsedCertificates.clear()
        if (!localKeyboxFile.exists()) return 0
        val xml = localKeyboxFile.readText()
        val regex = Regex("""-----BEGIN CERTIFICATE-----(.*?)-----END CERTIFICATE-----""", RegexOption.DOT_MATCHES_ALL)
        val cf = CertificateFactory.getInstance("X.509")
        regex.findAll(xml).forEach { match ->
            try {
                val base64Cert = match.groupValues[1].replace("\\s".toRegex(), "")
                val certBytes = Base64.decode(base64Cert, Base64.DEFAULT)
                val cert = cf.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
                parsedCertificates.add(cert)
            } catch (_: Throwable) {}
        }
        return parsedCertificates.size
    }

    fun syncWithSystemTrickyStore(extraPackages: List<String>, modeFlag: String = "!"): String {
        val xml = getSavedKeyboxXml()
        val tempKb = File(VEnvironment.rootDir, "tmp_kb.xml")
        tempKb.writeText(xml)

        val pkgList = (extraPackages + VBoxCore.hostContext.packageName + "com.google.android.gms").distinct()
        val cmd = """
            mkdir -p /data/adb/tricky_store
            cp "${tempKb.absolutePath}" /data/adb/tricky_store/keybox.xml
            chmod 600 /data/adb/tricky_store/keybox.xml
            for pkg in ${pkgList.joinToString(" ")}; do
                grep -q "^${'$'}pkg" /data/adb/tricky_store/target.txt 2>/dev/null || echo "${'$'}pkg$modeFlag" >> /data/adb/tricky_store/target.txt
            done
            chmod 644 /data/adb/tricky_store/target.txt
            echo "TRICKYSTORE_SYNC_OK"
        """.trimIndent()

        val output = RootShell.exec(cmd)
        return if (output.contains("TRICKYSTORE_SYNC_OK")) {
            "Synced keybox.xml & target.txt ($modeFlag) to /data/adb/tricky_store/!"
        } else {
            "In-Process Keybox Active (${reloadCertificates()} certs). Root folder skipped."
        }
    }

    private const val SAMPLE_KEYBOX_TEMPLATE = """<?xml version="1.0"?>
<AndroidAttestation>
    <NumberOfKeyboxes>1</NumberOfKeyboxes>
    <Keybox DeviceID="ModernVBox_A15">
        <Key algorithm="ecdsa">
            <PrivateKey format="pem">
-----BEGIN EC PRIVATE KEY-----
PASTE_YOUR_PRIVATE_KEY_HERE
-----END EC PRIVATE KEY-----
            </PrivateKey>
            <CertificateChain>
                <NumberOfCertificates>3</NumberOfCertificates>
                <Certificate format="pem">
-----BEGIN CERTIFICATE-----
PASTE_YOUR_LEAF_CERTIFICATE_HERE
-----END CERTIFICATE-----
                </Certificate>
            </CertificateChain>
        </Key>
    </Keybox>
</AndroidAttestation>"""
}

/**
 * 7. VIRTUAL LSPOSED / XPOSED MODULE ENGINE
 */
object VXposedEngine {
    const val BUILTIN_ANTI_ROOT = "builtin.xposed.antiroot"
    const val BUILTIN_HIDE_ADB = "builtin.xposed.hideadb"
    const val BUILTIN_BYPASS_SECURE = "builtin.xposed.flagsecure"

    private val configFile: File get() = File(VEnvironment.rootDir, "vxposed_modules.txt")

    fun getEnabledModulePackages(userId: Int): Set<String> {
        if (!configFile.exists()) return setOf(BUILTIN_ANTI_ROOT, BUILTIN_HIDE_ADB)
        return configFile.readLines()
            .filter { it.startsWith("$userId|") }
            .map { it.substringAfter("$userId|").trim() }
            .toSet()
    }

    fun setModuleEnabled(userId: Int, modulePkg: String, enabled: Boolean) {
        val allLines = if (configFile.exists()) configFile.readLines().toMutableList() else mutableListOf()
        val entry = "$userId|$modulePkg"
        if (enabled) {
            if (!allLines.contains(entry)) allLines.add(entry)
        } else {
            allLines.remove(entry)
        }
        configFile.writeText(allLines.joinToString("\n") + "\n")
    }

    fun scanAllModules(context: Context, userId: Int): List<XposedModuleItem> {
        val enabledSet = getEnabledModulePackages(userId)
        val result = mutableListOf(
            XposedModuleItem("[Built-in] Anti-Root & HMA Stealth", BUILTIN_ANTI_ROOT, "internal", "InternalRootHook", "Hides KernelSU, Magisk, LSPosed, ZArchiver & Root packages", enabledSet.contains(BUILTIN_ANTI_ROOT)),
            XposedModuleItem("[Built-in] Hide ADB & Developer Mode", BUILTIN_HIDE_ADB, "internal", "InternalAdbHook", "Spoofs adb_enabled=0 for Banking Apps", enabledSet.contains(BUILTIN_HIDE_ADB)),
            XposedModuleItem("[Built-in] Bypass Screenshot Lock (FLAG_SECURE)", BUILTIN_BYPASS_SECURE, "internal", "InternalSecureWindowHook", "Allows screenshots inside restricted apps", enabledSet.contains(BUILTIN_BYPASS_SECURE))
        )

        val pm = context.packageManager
        for (app in pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            val meta = app.metaData ?: continue
            if (meta.containsKey("xposedmodule") || meta.containsKey("xposedminversion")) {
                val desc = meta.getString("xposeddescription") ?: "External Xposed / LSPosed Module"
                val entryClass = readXposedInitClass(app.sourceDir) ?: "UnknownEntry"
                result.add(
                    XposedModuleItem(
                        appName = pm.getApplicationLabel(app).toString(),
                        packageName = app.packageName,
                        apkPath = app.sourceDir,
                        entryClass = entryClass,
                        description = desc,
                        isEnabled = enabledSet.contains(app.packageName)
                    )
                )
            }
        }
        return result
    }

    private fun readXposedInitClass(apkPath: String): String? {
        return try {
            ZipFile(apkPath).use { zip ->
                val entry = zip.getEntry("assets/xposed_init") ?: return null
                zip.getInputStream(entry).bufferedReader().readLines()
                    .firstOrNull { it.isNotBlank() && !it.startsWith("#") }?.trim()
            }
        } catch (_: Throwable) { null }
    }

    fun applyHooksForActivity(activity: Activity, userId: Int, pkgName: String, appClassLoader: ClassLoader) {
        val enabled = getEnabledModulePackages(userId)
        if (enabled.contains(BUILTIN_BYPASS_SECURE)) {
            try {
                activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } catch (_: Throwable) {}
        }

        val modules = scanAllModules(VBoxCore.hostContext, userId)
            .filter { it.isEnabled && it.apkPath != "internal" }
        for (mod in modules) {
            try {
                val modApk = File(mod.apkPath)
                val odex = File(VEnvironment.rootDir, "xposed_odex_${mod.packageName}").apply { mkdirs() }
                val modLoader = DexClassLoader(modApk.absolutePath, odex.absolutePath, modApk.parent, appClassLoader)
                if (mod.entryClass != "UnknownEntry") {
                    modLoader.loadClass(mod.entryClass).getDeclaredConstructor().newInstance()
                }
            } catch (_: Throwable) {}
        }
    }
}

class PackageManagerStealthProxy(private val basePm: Any) : InvocationHandler {
    private val hiddenKeywords = listOf("kernelsu", "magisk", "lsposed", "edxposed", "tricky", "zarchiver", "hidemyapplist")

    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>?): Any? {
        val activePkg = VBoxCore.activeVirtualPackage
        val userId = VBoxCore.activeUserId
        val antiRootActive = activePkg != null &&
                VXposedEngine.getEnabledModulePackages(userId).contains(VXposedEngine.BUILTIN_ANTI_ROOT)

        if (antiRootActive && args != null && (method.name == "getPackageInfo" || method.name == "getApplicationInfo")) {
            val queriedPkg = args.firstOrNull { it is String } as? String ?: ""
            if (hiddenKeywords.any { queriedPkg.lowercase().contains(it) }) {
                throw PackageManager.NameNotFoundException("Hidden by ModernVBox")
            }
        }
        return try {
            method.invoke(basePm, *(args ?: emptyArray()))
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}

class BinderStartActivityProxy(private val baseBinder: Any) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>?): Any? {
        if (method.name == "startActivity" && args != null) {
            val intentIndex = args.indexOfFirst { it is Intent }
            if (intentIndex != -1) {
                val originalIntent = args[intentIndex] as Intent
                val targetPkg = originalIntent.component?.packageName

                if (GmsManager.isGmsIntent(originalIntent) &&
                    targetPkg != null &&
                    !VEnvironment.isAppCloned(VBoxCore.activeUserId, targetPkg)
                ) {
                    for (i in args.indices) {
                        if (args[i] is String && args[i] == VBoxCore.activeVirtualPackage) {
                            args[i] = VBoxCore.hostContext.packageName
                        }
                    }
                    return method.invoke(baseBinder, *args)
                }

                val isInternalVirtualNavigation = targetPkg != null &&
                        targetPkg != VBoxCore.hostContext.packageName &&
                        targetPkg == VBoxCore.activeVirtualPackage &&
                        originalIntent.component?.className != StubActivity::class.java.name &&
                        originalIntent.component?.className != ShortcutProxyActivity::class.java.name

                if (isInternalVirtualNavigation) {
                    val stubIntent = Intent().apply {
                        component = ComponentName(VBoxCore.hostContext.packageName, StubActivity::class.java.name)
                        putExtra(VBoxCore.EXTRA_TARGET_INTENT, originalIntent)
                        putExtra(VBoxCore.EXTRA_USER_ID, VBoxCore.activeUserId)
                        putExtra(VBoxCore.EXTRA_PKG_NAME, targetPkg)
                        flags = originalIntent.flags
                    }
                    args[intentIndex] = stubIntent
                }
            }
        }
        return try {
            method.invoke(baseBinder, *(args ?: emptyArray()))
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}

class VInstrumentation(private val base: Instrumentation) : Instrumentation() {

    override fun newActivity(cl: ClassLoader?, className: String?, intent: Intent?): Activity {
        if (intent != null && intent.hasExtra(VBoxCore.EXTRA_TARGET_INTENT)) {
            @Suppress("DEPRECATION")
            val targetIntent = intent.getParcelableExtra<Intent>(VBoxCore.EXTRA_TARGET_INTENT)
            val userId = intent.getIntExtra(VBoxCore.EXTRA_USER_ID, 0)
            val pkgName = intent.getStringExtra(VBoxCore.EXTRA_PKG_NAME)
                ?: targetIntent?.component?.packageName
            val realClassName = targetIntent?.component?.className

            if (pkgName != null && realClassName != null) {
                try {
                    VBoxCore.activeUserId = userId
                    VBoxCore.activeVirtualPackage = pkgName

                    VEnvironment.getUsers().find { it.id == userId }?.let { vUser ->
                        DeviceSpoofer.applySpoofForUser(vUser)
                        ProxyEngine.applyProxyForUser(vUser)
                    }
                    LocationSpoofer.installLocationBinderHook(VBoxCore.hostContext)
                    TelephonyAndWifiSpoofer.installTelephonyAndWifiHooks(VBoxCore.hostContext)

                    val vClassLoader = VBoxCore.getOrCreateClassLoader(userId, pkgName)
                    val activityClass = vClassLoader.loadClass(realClassName)
                    return activityClass.getDeclaredConstructor().newInstance() as Activity
                } catch (e: Throwable) {
                    Log.e("VInstrumentation", "Guest class load fallback for $realClassName", e)
                }
            }
        }
        return base.newActivity(cl, className, intent)
    }

    override fun callActivityOnCreate(activity: Activity, icicle: Bundle?) {
        val wrapperIntent = activity.intent
        var hookedUserId = -1
        var hookedPkg: String? = null

        if (wrapperIntent != null && wrapperIntent.hasExtra(VBoxCore.EXTRA_TARGET_INTENT)) {
            @Suppress("DEPRECATION")
            val targetIntent = wrapperIntent.getParcelableExtra<Intent>(VBoxCore.EXTRA_TARGET_INTENT)
            val userId = wrapperIntent.getIntExtra(VBoxCore.EXTRA_USER_ID, 0)
            val pkgName = wrapperIntent.getStringExtra(VBoxCore.EXTRA_PKG_NAME)

            if (targetIntent != null && pkgName != null) {
                hookedUserId = userId
                hookedPkg = pkgName
                try {
                    val vClassLoader = VBoxCore.getOrCreateClassLoader(userId, pkgName)
                    val vResources = VBoxCore.getOrCreateResources(userId, pkgName)
                    val vContext = VContextWrapper(
                        activity.baseContext,
                        userId,
                        pkgName,
                        vResources,
                        vClassLoader
                    )

                    injectVirtualEnvironment(activity, vContext, vResources, userId, pkgName, vClassLoader)
                    activity.intent = targetIntent
                } catch (e: Throwable) {
                    Log.e("VInstrumentation", "Context injection warning", e)
                }
            }
        }

        try {
            base.callActivityOnCreate(activity, icicle)
            if (activity is StubActivity && hookedPkg != null) {
                // If StubActivity itself opened because guest class required isolated process, trigger safe launch
                VBoxCore.fallbackSafeIntentLaunch(activity, hookedUserId.coerceAtLeast(0), hookedPkg)
                activity.finish()
                return
            }
        } catch (e: Throwable) {
            Log.e("VInstrumentation", "Guest onCreate shielded error", e)
            if (hookedPkg != null) {
                VBoxCore.fallbackSafeIntentLaunch(activity, hookedUserId.coerceAtLeast(0), hookedPkg)
            }
            activity.finish()
            return
        }

        if (hookedUserId != -1 && hookedPkg != null) {
            val cl = VBoxCore.getOrCreateClassLoader(hookedUserId, hookedPkg)
            VXposedEngine.applyHooksForActivity(activity, hookedUserId, hookedPkg, cl)
        }
    }

    private fun injectVirtualEnvironment(
        activity: Activity,
        vContext: VContextWrapper,
        vResources: Resources,
        userId: Int,
        pkgName: String,
        vClassLoader: ClassLoader
    ) {
        findField(ContextWrapper::class.java, "mBase")?.set(activity, vContext)
        findField(ContextThemeWrapper::class.java, "mResources")?.set(activity, vResources)

        val vApp = VBoxCore.getOrCreateVirtualApplication(userId, pkgName, vClassLoader, vContext)
        if (vApp != null) {
            findField(Activity::class.java, "mApplication")?.set(activity, vApp)
        }
    }

    private fun findField(clazz: Class<*>, name: String): Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                return current.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }
}

class VContextWrapper(
    base: Context,
    private val userId: Int,
    private val virtualPkg: String,
    private val virtualResources: Resources,
    private val virtualClassLoader: ClassLoader
) : ContextWrapper(base) {

    override fun getResources(): Resources = virtualResources
    override fun getAssets(): AssetManager = virtualResources.assets
    override fun getClassLoader(): ClassLoader = virtualClassLoader
    override fun getApplicationContext(): Context = this

    override fun getDataDir(): File = VEnvironment.getDataDir(userId, virtualPkg)
    override fun getFilesDir(): File = VEnvironment.getFilesDir(userId, virtualPkg)
    override fun getCacheDir(): File = VEnvironment.getCacheDir(userId, virtualPkg)
    override fun getCodeCacheDir(): File = VEnvironment.getOdexDir(userId, virtualPkg)

    override fun getExternalFilesDir(type: String?): File? {
        val dir = File(VEnvironment.getDataDir(userId, virtualPkg), "external_files/${type ?: ""}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        val isolatedPrefName = "vbox_u${userId}_${virtualPkg}_${name ?: "default"}"
        return super.getSharedPreferences(isolatedPrefName, mode)
    }

    override fun getDatabasePath(name: String?): File {
        val dbDir = VEnvironment.getDatabasesDir(userId, virtualPkg)
        return File(dbDir, name ?: "default.db")
    }

    override fun openOrCreateDatabase(name: String?, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory)
    }

    override fun openOrCreateDatabase(name: String?, mode: Int, factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).path, factory, errorHandler)
    }

    override fun getSystemService(name: String): Any? {
        val service = super.getSystemService(name)
        when (name) {
            Context.LOCATION_SERVICE -> LocationSpoofer.installLocationBinderHook(this)
            Context.TELEPHONY_SERVICE -> TelephonyAndWifiSpoofer.hookTelephonyBinder(this)
            Context.WIFI_SERVICE -> TelephonyAndWifiSpoofer.hookWifiBinder(this)
        }
        return service
    }
}

object VEnvironment {
    val rootDir: File
        get() = File(VBoxCore.hostContext.filesDir, "virtual_engine").apply { if (!exists()) mkdirs() }

    private val usersFile: File
        get() = File(rootDir, "users_registry_v5.txt")

    fun ensureDefaultUser() {
        if (!usersFile.exists() || usersFile.readText().isBlank()) {
            val androidId = generateRandomAndroidId()
            val defaultProfile = DeviceProfile.presets().first().serialize()
            val defaultProxy = ProxyConfig().serialize()
            val defaultGps = GpsConfig().serialize()
            val defaultSimWifi = SimWifiConfig.presets().first().serialize()
            usersFile.writeText("0|Space 0 (Primary)|$androidId|$defaultProfile|false|$defaultProxy|$defaultGps|$defaultSimWifi|-1\n")
        }
    }

    fun getUsers(): List<VUser> {
        ensureDefaultUser()
        return usersFile.readLines()
            .filter { it.contains("|") }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 5) {
                    VUser(
                        id = parts[0].toIntOrNull() ?: 0,
                        name = parts[1],
                        virtualAndroidId = parts[2],
                        deviceProfile = DeviceProfile.deserialize(parts[3]),
                        gmsEnabled = parts[4].toBoolean(),
                        proxyConfig = if (parts.size >= 6) ProxyConfig.deserialize(parts[5]) else ProxyConfig(),
                        gpsConfig = if (parts.size >= 7) GpsConfig.deserialize(parts[6]) else GpsConfig(),
                        simWifiConfig = if (parts.size >= 8) SimWifiConfig.deserialize(parts[7]) else SimWifiConfig.presets().first(),
                        systemUserId = if (parts.size >= 9) parts[8].toIntOrNull() ?: -1 else -1
                    )
                } else null
            }
    }

    private fun saveUsers(list: List<VUser>) {
        usersFile.writeText(
            list.joinToString("\n") {
                "${it.id}|${it.name}|${it.virtualAndroidId}|${it.deviceProfile.serialize()}|${it.gmsEnabled}|${it.proxyConfig.serialize()}|${it.gpsConfig.serialize()}|${it.simWifiConfig.serialize()}|${it.systemUserId}"
            } + "\n"
        )
    }

    fun createNewUser(spaceName: String, presetIndex: Int = 0): VUser {
        val current = getUsers().toMutableList()
        val nextId = (current.maxOfOrNull { it.id } ?: 0) + 1
        val devicePresets = DeviceProfile.presets()
        val simPresets = SimWifiConfig.presets()
        val chosenProfile = devicePresets[presetIndex % devicePresets.size]
        val chosenSim = simPresets[presetIndex % simPresets.size].copy(
            imei = SimWifiConfig.generateRandomImei(),
            imsi = SimWifiConfig.generateRandomImsi(simPresets[presetIndex % simPresets.size].mccMnc),
            iccid = SimWifiConfig.generateRandomIccid(),
            wifiMac = SimWifiConfig.generateRandomMac(),
            wifiBssid = SimWifiConfig.generateRandomMac()
        )
        val newUser = VUser(
            id = nextId,
            name = spaceName,
            virtualAndroidId = generateRandomAndroidId(),
            deviceProfile = chosenProfile,
            gmsEnabled = false,
            proxyConfig = ProxyConfig(),
            gpsConfig = GpsConfig(),
            simWifiConfig = chosenSim,
            systemUserId = -1
        )
        current.add(newUser)
        saveUsers(current)
        File(rootDir, "user_$nextId").mkdirs()
        return newUser
    }

    fun updateSystemUserId(userId: Int, sysUserId: Int) {
        val list = getUsers().toMutableList()
        list.find { it.id == userId }?.systemUserId = sysUserId
        saveUsers(list)
    }

    fun updateUserDeviceProfile(userId: Int, newProfile: DeviceProfile) {
        val list = getUsers().toMutableList()
        list.find { it.id == userId }?.deviceProfile = newProfile
        saveUsers(list)
    }

    fun updateUserSimWifiConfig(userId: Int, newSimWifi: SimWifiConfig) {
        val list = getUsers().toMutableList()
        list.find { it.id == userId }?.simWifiConfig = newSimWifi
        saveUsers(list)
    }

    fun updateUserProxyConfig(userId: Int, newProxy: ProxyConfig) {
        val list = getUsers().toMutableList()
        list.find { it.id == userId }?.proxyConfig = newProxy
        saveUsers(list)
    }

    fun updateUserGpsConfig(userId: Int, newGps: GpsConfig) {
        val list = getUsers().toMutableList()
        list.find { it.id == userId }?.gpsConfig = newGps
        saveUsers(list)
    }

    fun setUserGmsState(userId: Int, enabled: Boolean) {
        val list = getUsers().toMutableList()
        list.find { it.id == userId }?.gmsEnabled = enabled
        saveUsers(list)
    }

    fun deleteUser(userId: Int): Boolean {
        if (userId == 0) return false
        val u = getUsers().find { it.id == userId }
        if (u != null && u.systemUserId > 0 && VBoxCore.isRootAvailable) {
            Thread { RootShell.exec("pm remove-user ${u.systemUserId}") }.start()
        }
        val remaining = getUsers().filter { it.id != userId }
        saveUsers(remaining)
        File(rootDir, "user_$userId").deleteRecursively()
        return true
    }

    fun getAppRootDir(userId: Int, pkgName: String): File = File(rootDir, "user_$userId/$pkgName")
    fun getClonedApkFile(userId: Int, pkgName: String): File = File(getAppRootDir(userId, pkgName), "base.apk")

    fun getAllApkFilesForApp(userId: Int, pkgName: String): List<File> {
        val dir = getAppRootDir(userId, pkgName)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.filter { it.isFile && it.name.endsWith(".apk") }?.sortedBy { it.name } ?: emptyList()
    }

    fun getOdexDir(userId: Int, pkgName: String): File =
        File(getAppRootDir(userId, pkgName), "odex").apply { if (!exists()) mkdirs() }

    fun getNativeLibDir(userId: Int, pkgName: String): File =
        File(getAppRootDir(userId, pkgName), "lib").apply { if (!exists()) mkdirs() }

    fun getDataDir(userId: Int, pkgName: String): File =
        File(getAppRootDir(userId, pkgName), "data").apply { if (!exists()) mkdirs() }

    fun getFilesDir(userId: Int, pkgName: String): File =
        File(getDataDir(userId, pkgName), "files").apply { if (!exists()) mkdirs() }

    fun getCacheDir(userId: Int, pkgName: String): File =
        File(getDataDir(userId, pkgName), "cache").apply { if (!exists()) mkdirs() }

    fun getDatabasesDir(userId: Int, pkgName: String): File =
        File(getDataDir(userId, pkgName), "databases").apply { if (!exists()) mkdirs() }

    fun isAppCloned(userId: Int, pkgName: String): Boolean = getClonedApkFile(userId, pkgName).exists()

    fun getClonedPackagesForUser(userId: Int): List<String> {
        val userDir = File(rootDir, "user_$userId")
        if (!userDir.exists()) return emptyList()
        return userDir.listFiles()
            ?.filter { it.isDirectory && File(it, "base.apk").exists() }
            ?.map { it.name } ?: emptyList()
    }

    /**
     * Clones Installed App + ALL Split APKs (split_config.arm64_v8a.apk, etc.) + Native .so Libs
     */
    fun clonePackageToUser(context: Context, userId: Int, pkgName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(pkgName, 0)
            val appDir = getAppRootDir(userId, pkgName)
            appDir.mkdirs()

            // 1. Copy base.apk
            val sourceBase = File(appInfo.sourceDir)
            val destBase = getClonedApkFile(userId, pkgName)
            copyReadOnlyApk(sourceBase, destBase)
            extractNativeLibraries(destBase, getNativeLibDir(userId, pkgName))

            // 2. Copy all Split APKs (CRITICAL FIX FOR ANDROID 15/16/17 CRASHES)
            var splitCount = 0
            appInfo.splitSourceDirs?.forEachIndexed { index, splitPath ->
                val splitFile = File(splitPath)
                if (splitFile.exists()) {
                    val destSplit = File(appDir, "split_$index.apk")
                    copyReadOnlyApk(splitFile, destSplit)
                    extractNativeLibraries(destSplit, getNativeLibDir(userId, pkgName))
                    splitCount++
                }
            }

            // 3. Also pre-link in Kernel 6.6 Multi-User container if Root is available
            if (VBoxCore.isRootAvailable && userId > 0) {
                val sysUid = KernelMultiUserBridge.ensureSystemUserForVirtualSpace(userId, "VBox_$userId")
                if (sysUid > 0) {
                    RootShell.exec("cmd package install-existing --user $sysUid $pkgName")
                }
            }

            getFilesDir(userId, pkgName)
            getCacheDir(userId, pkgName)
            getDatabasesDir(userId, pkgName)

            "Cloned Successfully in Space $userId (Base + $splitCount Split APKs + Native Libs Ready)"
        } catch (e: Throwable) {
            "Clone Error: ${e.message}"
        }
    }

    /**
     * Clones an External .APK file directly from Storage URI or File Path into Virtual Space
     */
    fun cloneExternalApkUriToUser(context: Context, userId: Int, uri: Uri): String {
        return try {
            val tempApk = File(rootDir, "temp_external_clone.apk")
            if (tempApk.exists()) {
                tempApk.setWritable(true)
                tempApk.delete()
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempApk).use { output ->
                    input.copyTo(output)
                }
            } ?: return "Storage stream open nahi ho saka!"

            cloneExternalApkFileToUser(context, userId, tempApk)
        } catch (e: Throwable) {
            "External APK Clone Error: ${e.message}"
        }
    }

    fun cloneExternalApkFileToUser(context: Context, userId: Int, apkFile: File): String {
        return try {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
            ) ?: return "Invalid ya Corrupted APK file!"

            val pkgName = pkgInfo.packageName
            val destApk = getClonedApkFile(userId, pkgName)
            destApk.parentFile?.mkdirs()

            copyReadOnlyApk(apkFile, destApk)
            extractNativeLibraries(destApk, getNativeLibDir(userId, pkgName))

            if (VBoxCore.isRootAvailable) {
                val sysUid = if (userId == 0) 0 else KernelMultiUserBridge.ensureSystemUserForVirtualSpace(userId, "VBox_$userId")
                if (sysUid >= 0) {
                    RootShell.exec("pm install --bypass-low-target-sdk-block --user $sysUid \"${destApk.absolutePath}\"")
                }
            }

            getFilesDir(userId, pkgName)
            getCacheDir(userId, pkgName)
            getDatabasesDir(userId, pkgName)

            "External APK ($pkgName) Cloned into Space $userId!"
        } catch (e: Throwable) {
            "APK Install Error: ${e.message}"
        }
    }

    private fun copyReadOnlyApk(source: File, dest: File) {
        if (dest.exists()) {
            dest.setWritable(true)
            dest.delete()
        }
        source.inputStream().use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
        // Android 14, 15, 16, 17 Read-Only DEX Enforcement
        dest.setReadable(true, false)
        dest.setWritable(false, false)
    }

    private fun extractNativeLibraries(apkFile: File, destLibDir: File) {
        val supportedAbis = Build.SUPPORTED_ABIS
        try {
            ZipFile(apkFile).use { zip ->
                for (abi in supportedAbis) {
                    var foundForAbi = false
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (!entry.isDirectory && entry.name.startsWith("lib/$abi/") && entry.name.endsWith(".so")) {
                            val soName = entry.name.substringAfterLast("/")
                            val outFile = File(destLibDir, soName)
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(outFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            outFile.setReadable(true, false)
                            outFile.setExecutable(true, false)
                            foundForAbi = true
                        }
                    }
                    if (foundForAbi) break
                }
            }
        } catch (_: Throwable) {}
    }

    fun scanDownloadFolderApks(): List<File> {
        val result = mutableListOf<File>()
        try {
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dlDir?.listFiles()?.forEach { f ->
                if (f.isFile && f.name.lowercase().endsWith(".apk")) result.add(f)
            }
        } catch (_: Throwable) {}
        return result
    }

    fun clearVirtualAppData(userId: Int, pkgName: String) {
        getDataDir(userId, pkgName).deleteRecursively()
        getFilesDir(userId, pkgName)
        getCacheDir(userId, pkgName)
        getDatabasesDir(userId, pkgName)
        VBoxCore.clearCacheFor(userId, pkgName)
    }

    fun removeClonedApp(userId: Int, pkgName: String) {
        getAllApkFilesForApp(userId, pkgName).forEach { it.setWritable(true) }
        getAppRootDir(userId, pkgName).deleteRecursively()
        VBoxCore.clearCacheFor(userId, pkgName)
    }

    private fun generateRandomAndroidId(): String {
        return UUID.randomUUID().toString().replace("-", "").take(16)
    }
}

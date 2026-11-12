package com.modern.vbox

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_PICK_EXTERNAL_APK = 5001
    }

    data class AppEntry(
        val label: String,
        val packageName: String,
        val icon: Drawable,
        var isCloned: Boolean,
        val isExternalOnly: Boolean = false
    )

    private val users = mutableListOf<VUser>()
    private val allApps = mutableListOf<AppEntry>()
    private val filteredApps = mutableListOf<AppEntry>()
    private var selectedUser: VUser? = null

    // 0 = Cloned Apps Tab, 1 = All Apps (+ Clone) Tab
    private var currentTab: Int = 0
    private var currentSearchQuery: String = ""

    private lateinit var spaceChipsContainer: LinearLayout
    private lateinit var statusCardTitle: TextView
    private lateinit var statusCardSubtitle: TextView
    private lateinit var engineBadgeBtn: TextView
    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var gridAdapter: AppGridAdapter
    private lateinit var tabClonedBtn: TextView
    private lateinit var tabAllAppsBtn: TextView
    private lateinit var tabToolsBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VBoxCore.init(this)
        buildModernDashboardUI()
        reloadSpaces()
    }

    private fun roundedBg(fillHex: String, radiusDp: Float = 16f, strokeHex: String? = null, strokeWidth: Int = 2): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * resources.displayMetrics.density
            setColor(Color.parseColor(fillHex))
            if (strokeHex != null) {
                setStroke(strokeWidth, Color.parseColor(strokeHex))
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buildModernDashboardUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#090D16"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. TOP APP BAR
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(10))
        }

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val appTitle = TextView(this).apply {
            text = "ModernVBox v5.0"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }

        val appSub = TextView(this).apply {
            text = "Android 15 • 16 • 17 | Kernel 6.6 Hybrid Engine"
            textSize = 11f
            setTextColor(Color.parseColor("#38BDF8"))
        }
        titleCol.addView(appTitle)
        titleCol.addView(appSub)

        engineBadgeBtn = TextView(this).apply {
            text = "⚡ HYBRID"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            background = roundedBg("#132A26", 20f, "#10B981", 2)
            setPadding(dp(12), dp(7), dp(12), dp(7))
            setOnClickListener { showEngineModeSelector() }
        }

        val btnImportApk = TextView(this).apply {
            text = "+ APK"
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = roundedBg("#6366F1", 20f)
            setPadding(dp(14), dp(7), dp(14), dp(7))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(8) }
            setOnClickListener { showExternalApkDialog() }
        }

        topBar.addView(titleCol)
        topBar.addView(engineBadgeBtn)
        topBar.addView(btnImportApk)
        root.addView(topBar)

        // 2. HORIZONTAL VIRTUAL SPACE CHIPS BAR
        val chipScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(14), dp(4), dp(14), dp(8))
        }
        spaceChipsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        chipScroll.addView(spaceChipsContainer)
        root.addView(chipScroll)

        // 3. TELEMETRY & ANTI-DETECT STATUS CARD
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#131C2E", 18f, "#1E293B", 2)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(16), dp(4), dp(16), dp(10))
            }
        }

        statusCardTitle = TextView(this).apply {
            text = "Active Space: Space 0"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }

        statusCardSubtitle = TextView(this).apply {
            text = "Loading virtual environment telemetry..."
            textSize = 11.5f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(4), 0, dp(10))
        }

        // Quick Action Pills inside Status Card
        val quickToolsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val quickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        quickRow.addView(createQuickChip("📱 Device Spoof", "#4F46E5") { showDeviceSpoofDialog() })
        quickRow.addView(createQuickChip("📶 SIM & MAC", "#059669") { showSimWifiDialog() })
        quickRow.addView(createQuickChip("🌐 Proxy/SOCKS5", "#7C3AED") { showProxyDialog() })
        quickRow.addView(createQuickChip("📍 Fake GPS", "#0D9488") { showGpsDialog() })
        quickRow.addView(createQuickChip("▶️ GMS Play", "#0284C7") { showGmsManagerDialog() })
        quickRow.addView(createQuickChip("🔑 Keybox XML", "#D97706") { showKeyboxDialog() })
        quickRow.addView(createQuickChip("🧩 LSPosed", "#DB2777") { showXposedManagerDialog() })
        quickToolsScroll.addView(quickRow)

        statusCard.addView(statusCardTitle)
        statusCard.addView(statusCardSubtitle)
        statusCard.addView(quickToolsScroll)
        root.addView(statusCard)

        // 4. MODERN SEARCH BAR
        searchInput = EditText(this).apply {
            hint = "🔍 Search apps or package name..."
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            textSize = 13.5f
            background = roundedBg("#131C2E", 14f, "#1E293B", 2)
            setPadding(dp(16), dp(11), dp(16), dp(11))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(16), 0, dp(16), dp(10))
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    currentSearchQuery = s?.toString()?.trim() ?: ""
                    applyFilterAndRefreshGrid()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        root.addView(searchInput)

        // 5. 2-COLUMN MODERN APP CARDS GRID (RecyclerView)
        recyclerView = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            setPadding(dp(10), dp(4), dp(10), dp(12))
            clipToPadding = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        gridAdapter = AppGridAdapter()
        recyclerView.adapter = gridAdapter
        root.addView(recyclerView)

        // 6. MODERN BOTTOM NAVIGATION BAR
        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedBg("#111827", 0f, "#1E293B", 2)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        tabClonedBtn = createBottomNavItem("🚀 Cloned Space", true) {
            switchTab(0)
        }
        tabAllAppsBtn = createBottomNavItem("➕ Clone Apps", false) {
            switchTab(1)
        }
        tabToolsBtn = createBottomNavItem("⚙️ Space Tools", false) {
            showSpaceManagementHub()
        }

        bottomNav.addView(tabClonedBtn)
        bottomNav.addView(tabAllAppsBtn)
        bottomNav.addView(tabToolsBtn)
        root.addView(bottomNav)

        setContentView(root)
    }

    private fun createQuickChip(label: String, colorHex: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = roundedBg(colorHex, 12f)
            setPadding(dp(12), dp(7), dp(12), dp(7))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { rightMargin = dp(8) }
            setOnClickListener { onClick() }
        }
    }

    private fun createBottomNavItem(label: String, active: Boolean, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 12.5f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (active) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
            background = if (active) roundedBg("#1E293B", 14f) else null
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun switchTab(tabIndex: Int) {
        currentTab = tabIndex
        tabClonedBtn.setTextColor(if (tabIndex == 0) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
        tabClonedBtn.background = if (tabIndex == 0) roundedBg("#1E293B", 14f) else null

        tabAllAppsBtn.setTextColor(if (tabIndex == 1) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
        tabAllAppsBtn.background = if (tabIndex == 1) roundedBg("#1E293B", 14f) else null

        applyFilterAndRefreshGrid()
    }

    private fun reloadSpaces() {
        val currentId = selectedUser?.id ?: 0
        users.clear()
        users.addAll(VEnvironment.getUsers())
        selectedUser = users.find { it.id == currentId } ?: users.firstOrNull()
        VBoxCore.activeUserId = selectedUser?.id ?: 0

        renderSpaceChips()
        loadAppsForSelectedUser()
    }

    private fun renderSpaceChips() {
        spaceChipsContainer.removeAllViews()
        val activeId = selectedUser?.id ?: 0

        for (u in users) {
            val isSelected = u.id == activeId
            val chip = TextView(this).apply {
                text = "👤 ${u.name} (ID:${u.id})"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))
                background = if (isSelected) {
                    roundedBg("#2563EB", 18f, "#60A5FA", 2)
                } else {
                    roundedBg("#131C2E", 18f, "#1E293B", 2)
                }
                setPadding(dp(14), dp(8), dp(14), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = dp(8) }
                setOnClickListener {
                    selectedUser = u
                    VBoxCore.activeUserId = u.id
                    TelephonyAndWifiSpoofer.applySimSystemPropertiesForActiveUser()
                    renderSpaceChips()
                    loadAppsForSelectedUser()
                }
                setOnLongClickListener {
                    confirmDeleteUser(u)
                    true
                }
            }
            spaceChipsContainer.addView(chip)
        }

        // "+ New Space" Chip
        val addChip = TextView(this).apply {
            text = "＋ New Space"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#10B981"))
            background = roundedBg("#0F2922", 18f, "#10B981", 2)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener { promptCreateUser() }
        }
        spaceChipsContainer.addView(addChip)
    }

    private fun loadAppsForSelectedUser() {
        val currentUser = selectedUser ?: return
        val sw = currentUser.simWifiConfig
        val proxyState = if (currentUser.proxyConfig.enabled) "${currentUser.proxyConfig.type} ${currentUser.proxyConfig.host}" else "Direct"
        val gpsState = if (currentUser.gpsConfig.enabled) currentUser.gpsConfig.label else "Real"

        statusCardTitle.text = "Space ${currentUser.id}: ${currentUser.name}  •  ${currentUser.deviceProfile.brand.uppercase()} ${currentUser.deviceProfile.model}"
        statusCardSubtitle.text =
            "SSAID: ${currentUser.virtualAndroidId}  |  SIM: ${sw.carrierName} (${sw.countryIso.uppercase()})\n" +
            "IMEI: ${sw.imei}  |  MAC: ${sw.wifiMac}  |  Net: $proxyState  |  GPS: $gpsState"

        Thread {
            val pm = packageManager
            val clonedPkgs = VEnvironment.getClonedPackagesForUser(currentUser.id).toSet()

            val installedMap = mutableMapOf<String, AppEntry>()
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != packageName }
                .forEach { info ->
                    installedMap[info.packageName] = AppEntry(
                        label = pm.getApplicationLabel(info).toString(),
                        packageName = info.packageName,
                        icon = pm.getApplicationIcon(info),
                        isCloned = clonedPkgs.contains(info.packageName),
                        isExternalOnly = false
                    )
                }

            // Include External APKs cloned into this space that are not installed on host
            for (cPkg in clonedPkgs) {
                if (!installedMap.containsKey(cPkg)) {
                    val apkFile = VEnvironment.getClonedApkFile(currentUser.id, cPkg)
                    val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                    val appInfo = archiveInfo?.applicationInfo
                    if (appInfo != null) {
                        appInfo.sourceDir = apkFile.absolutePath
                        appInfo.publicSourceDir = apkFile.absolutePath
                        installedMap[cPkg] = AppEntry(
                            label = pm.getApplicationLabel(appInfo).toString(),
                            packageName = cPkg,
                            icon = pm.getApplicationIcon(appInfo),
                            isCloned = true,
                            isExternalOnly = true
                        )
                    }
                }
            }

            val sorted = installedMap.values.sortedWith(
                compareByDescending<AppEntry> { it.isCloned }.thenBy { it.label.lowercase() }
            )

            runOnUiThread {
                allApps.clear()
                allApps.addAll(sorted)
                // If user has no cloned apps yet in this space, automatically show "Clone Apps" tab so screen isn't empty
                val hasClones = allApps.any { it.isCloned }
                if (!hasClones && currentTab == 0) {
                    switchTab(1)
                } else {
                    applyFilterAndRefreshGrid()
                }
            }
        }.start()
    }

    private fun applyFilterAndRefreshGrid() {
        filteredApps.clear()
        val q = currentSearchQuery.lowercase()

        val source = if (currentTab == 0) {
            allApps.filter { it.isCloned }
        } else {
            allApps
        }

        filteredApps.addAll(
            source.filter {
                q.isBlank() || it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        )
        gridAdapter.notifyDataSetChanged()
    }

    /**
     * RecyclerView 2-Column Modern Card Adapter
     */
    inner class AppGridAdapter : RecyclerView.Adapter<AppGridAdapter.CardHolder>() {

        inner class CardHolder(
            val rootCard: LinearLayout,
            val iconView: ImageView,
            val badgeText: TextView,
            val titleText: TextView,
            val pkgText: TextView,
            val primaryActionBtn: TextView,
            val shortcutActionBtn: TextView
        ) : RecyclerView.ViewHolder(rootCard)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder {
            val card = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBg("#131C2E", 18f, "#1E293B", 2)
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = GridLayoutManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(6), dp(6), dp(6), dp(6))
                }
            }

            val topRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val icon = ImageView(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
            }

            val spacer = View(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            }

            val badge = TextView(this@MainActivity).apply {
                textSize = 9.5f
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(8), dp(4), dp(8), dp(4))
            }

            topRow.addView(icon)
            topRow.addView(spacer)
            topRow.addView(badge)

            val title = TextView(this@MainActivity).apply {
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                maxLines = 1
                setPadding(0, dp(10), 0, dp(2))
            }

            val pkg = TextView(this@MainActivity).apply {
                textSize = 10.5f
                setTextColor(Color.parseColor("#64748B"))
                maxLines = 1
                setPadding(0, 0, 0, dp(12))
            }

            val btnRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val primaryBtn = TextView(this@MainActivity).apply {
                textSize = 11.5f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val shortcutBtn = TextView(this@MainActivity).apply {
                text = "📌"
                textSize = 12f
                gravity = Gravity.CENTER
                background = roundedBg("#1E293B", 10f)
                setPadding(dp(10), dp(8), dp(10), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { leftMargin = dp(6) }
            }

            btnRow.addView(primaryBtn)
            btnRow.addView(shortcutBtn)

            card.addView(topRow)
            card.addView(title)
            card.addView(pkg)
            card.addView(btnRow)

            return CardHolder(card, icon, badge, title, pkg, primaryBtn, shortcutBtn)
        }

        override fun onBindViewHolder(holder: CardHolder, position: Int) {
            val item = filteredApps[position]
            val uId = selectedUser?.id ?: 0

            holder.iconView.setImageDrawable(item.icon)
            holder.titleText.text = item.label
            holder.pkgText.text = item.packageName

            if (item.isCloned) {
                holder.badgeText.text = if (item.isExternalOnly) "EXT APK • S$uId" else "READY • S$uId"
                holder.badgeText.setTextColor(Color.parseColor("#34D399"))
                holder.badgeText.background = roundedBg("#0F2922", 10f)

                holder.primaryActionBtn.text = "▶ Launch"
                holder.primaryActionBtn.background = roundedBg("#10B981", 10f)
                holder.shortcutActionBtn.visibility = View.VISIBLE

                holder.primaryActionBtn.setOnClickListener {
                    VBoxCore.launchClonedApp(this@MainActivity, uId, item.packageName)
                }
                holder.shortcutActionBtn.setOnClickListener {
                    ShortcutHelper.createShortcutForClonedApp(
                        this@MainActivity,
                        uId,
                        item.packageName,
                        item.label,
                        item.icon
                    )
                }
            } else {
                holder.badgeText.text = "HOST APP"
                holder.badgeText.setTextColor(Color.parseColor("#94A3B8"))
                holder.badgeText.background = roundedBg("#1E293B", 10f)

                holder.primaryActionBtn.text = "＋ Clone to S$uId"
                holder.primaryActionBtn.background = roundedBg("#2563EB", 10f)
                holder.shortcutActionBtn.visibility = View.GONE

                holder.primaryActionBtn.setOnClickListener {
                    cloneAppWithProgress(item, uId)
                }
            }

            holder.rootCard.setOnClickListener {
                showAppMenu(item)
            }
        }

        override fun getItemCount(): Int = filteredApps.size
    }

    private fun cloneAppWithProgress(app: AppEntry, userId: Int) {
        statusCardSubtitle.text = "⏳ Cloning ${app.label} (+ Split APKs & Native Libs) into Space $userId..."
        Thread {
            val msg = VEnvironment.clonePackageToUser(this, userId, app.packageName)
            runOnUiThread {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                loadAppsForSelectedUser()
            }
        }.start()
    }

    /**
     * EXTERNAL .APK CLONER DIALOG (File Picker + Download Folder Scanner)
     */
    private fun showExternalApkDialog() {
        val user = selectedUser ?: return
        val downloadedApks = VEnvironment.scanDownloadFolderApks()
        val dlOptions = downloadedApks.map { "📦 Clone Download/${it.name}" }.toTypedArray()

        val baseOptions = arrayOf(
            "📂 Pick .APK File from Phone Storage (File Manager)"
        ) + dlOptions

        AlertDialog.Builder(this)
            .setTitle("Clone External .APK into Space ${user.id}")
            .setItems(baseOptions) { _, which ->
                if (which == 0) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/vnd.android.package-archive"
                    }
                    try {
                        @Suppress("DEPRECATION")
                        startActivityForResult(intent, REQ_PICK_EXTERNAL_APK)
                    } catch (e: Throwable) {
                        Toast.makeText(this, "File picker error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val chosenFile = downloadedApks[which - 1]
                    statusCardSubtitle.text = "⏳ Installing ${chosenFile.name} into Space ${user.id}..."
                    Thread {
                        val res = VEnvironment.cloneExternalApkFileToUser(this, user.id, chosenFile)
                        runOnUiThread {
                            Toast.makeText(this, res, Toast.LENGTH_LONG).show()
                            switchTab(0)
                            loadAppsForSelectedUser()
                        }
                    }.start()
                }
            }
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_EXTERNAL_APK && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val user = selectedUser ?: return
            statusCardSubtitle.text = "⏳ Cloning selected .APK file into Space ${user.id}..."
            Thread {
                val msg = VEnvironment.cloneExternalApkUriToUser(this, user.id, uri)
                runOnUiThread {
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    switchTab(0)
                    loadAppsForSelectedUser()
                }
            }.start()
        }
    }

    private fun showEngineModeSelector() {
        val modes = arrayOf(
            "⚡ HYBRID AUTO (Kernel 6.6 Multi-User + Hook Engine - 100% Crash Free)",
            "🛡️ IN-PROCESS HOOK ONLY (Non-Root Split-APK Virtual Loader)"
        )
        AlertDialog.Builder(this)
            .setTitle("Select Virtual Execution Engine")
            .setItems(modes) { _, which ->
                if (which == 0) {
                    VBoxCore.preferredEngineMode = "HYBRID_AUTO"
                    engineBadgeBtn.text = "⚡ HYBRID"
                } else {
                    VBoxCore.preferredEngineMode = "IN_PROCESS_HOOK"
                    engineBadgeBtn.text = "🛡️ HOOK"
                }
                Toast.makeText(this, "Engine mode updated!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showSpaceManagementHub() {
        val user = selectedUser ?: return
        val actions = arrayOf(
            "1. 📱 Per-User Device Model & Fingerprint Spoofer",
            "2. 📶 SIM Operator, IMEI, IMSI & Wi-Fi MAC Spoofer",
            "3. 🌐 Proxy / SOCKS5 Configurator & IP Tester",
            "4. 📍 Fake GPS Location Spoofer",
            "5. ▶️ BlackBox GMS & Google Play Login Manager",
            "6. 🔑 TrickyStore Keybox.xml & target.txt Sync",
            "7. 🧩 Virtual LSPosed / Xposed Modules",
            "8. 📌 Allow Home Screen Shortcut Permission (HyperOS/MIUI)",
            "9. 🗑️ Delete Current Virtual Space (Space ${user.id})"
        )
        AlertDialog.Builder(this)
            .setTitle("Space ${user.id} (${user.name}) Control Hub")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showDeviceSpoofDialog()
                    1 -> showSimWifiDialog()
                    2 -> showProxyDialog()
                    3 -> showGpsDialog()
                    4 -> showGmsManagerDialog()
                    5 -> showKeyboxDialog()
                    6 -> showXposedManagerDialog()
                    7 -> ShortcutHelper.openShortcutPermissionSettings(this)
                    8 -> confirmDeleteUser(user)
                }
            }
            .show()
    }

    private fun showAppMenu(app: AppEntry) {
        val currentUser = selectedUser ?: return
        val actions = arrayOf(
            "1. 🚀 Launch Cloned App in Space ${currentUser.id}",
            "2. 📌 Create Home Screen Shortcut (Badged S${currentUser.id})",
            "3. 🔄 Clone / Re-Sync Split APKs in Space ${currentUser.id}",
            "4. 🔑 Add Package to TrickyStore target.txt (!)",
            "5. 🧹 Clear Virtual App Data (Space ${currentUser.id})",
            "6. ❌ Remove Clone from Space ${currentUser.id}"
        )

        AlertDialog.Builder(this)
            .setTitle("${app.label} [Space ${currentUser.id}]")
            .setItems(actions) { _, index ->
                when (index) {
                    0 -> VBoxCore.launchClonedApp(this, currentUser.id, app.packageName)
                    1 -> {
                        if (!app.isCloned) {
                            Toast.makeText(this, "Pehle App ko Clone karein!", Toast.LENGTH_SHORT).show()
                        } else {
                            ShortcutHelper.createShortcutForClonedApp(
                                this,
                                currentUser.id,
                                app.packageName,
                                app.label,
                                app.icon
                            )
                        }
                    }
                    2 -> cloneAppWithProgress(app, currentUser.id)
                    3 -> {
                        Thread {
                            val res = KeyboxEngine.syncWithSystemTrickyStore(listOf(app.packageName), "!")
                            runOnUiThread { Toast.makeText(this, res, Toast.LENGTH_LONG).show() }
                        }.start()
                    }
                    4 -> {
                        VEnvironment.clearVirtualAppData(currentUser.id, app.packageName)
                        Toast.makeText(this, "Data cleared in Space ${currentUser.id}", Toast.LENGTH_SHORT).show()
                    }
                    5 -> {
                        VEnvironment.removeClonedApp(currentUser.id, app.packageName)
                        Toast.makeText(this, "Removed from Space ${currentUser.id}", Toast.LENGTH_SHORT).show()
                        loadAppsForSelectedUser()
                    }
                }
            }
            .show()
    }

    private fun showDeviceSpoofDialog() {
        val user = selectedUser ?: return
        val presets = DeviceProfile.presets()
        val options = presets.map { "Preset: ${it.brand.uppercase()} - ${it.model}" }.toTypedArray() +
                "Custom Model / Brand / Fingerprint Editor..."

        AlertDialog.Builder(this)
            .setTitle("Device Spoofer [Space ${user.id}]")
            .setItems(options) { _, which ->
                if (which < presets.size) {
                    val chosen = presets[which]
                    VEnvironment.updateUserDeviceProfile(user.id, chosen)
                    reloadSpaces()
                } else {
                    showCustomDeviceEditor(user)
                }
            }
            .show()
    }

    private fun showCustomDeviceEditor(user: VUser) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val etBrand = EditText(this).apply { hint = "Brand"; setText(user.deviceProfile.brand) }
        val etModel = EditText(this).apply { hint = "Model"; setText(user.deviceProfile.model) }
        val etDevice = EditText(this).apply { hint = "Device Codename"; setText(user.deviceProfile.device) }
        val etFp = EditText(this).apply { hint = "Build.FINGERPRINT"; setText(user.deviceProfile.fingerprint) }

        layout.addView(etBrand)
        layout.addView(etModel)
        layout.addView(etDevice)
        layout.addView(etFp)

        AlertDialog.Builder(this)
            .setTitle("Custom Device Spoof (Space ${user.id})")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val updated = DeviceProfile(
                    brand = etBrand.text.toString().ifBlank { "google" },
                    manufacturer = etBrand.text.toString().ifBlank { "Google" },
                    model = etModel.text.toString().ifBlank { "Pixel 9 Pro" },
                    device = etDevice.text.toString().ifBlank { "caiman" },
                    product = etDevice.text.toString().ifBlank { "caiman" },
                    fingerprint = etFp.text.toString().ifBlank { user.deviceProfile.fingerprint }
                )
                VEnvironment.updateUserDeviceProfile(user.id, updated)
                reloadSpaces()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSimWifiDialog() {
        val user = selectedUser ?: return
        val presets = SimWifiConfig.presets()
        val options = presets.map {
            "Carrier: ${it.carrierName} (${it.countryIso.uppercase()} - ${it.mccMnc})"
        }.toTypedArray() + arrayOf(
            "🎲 1-Click Randomize IMEI + IMSI + ICCID + Wi-Fi MAC",
            "✏️ Custom SIM / IMEI / Wi-Fi MAC Editor...",
            "Disable SIM & Wi-Fi Spoofing"
        )

        AlertDialog.Builder(this)
            .setTitle("SIM & Wi-Fi MAC Spoofer [Space ${user.id}]")
            .setItems(options) { _, which ->
                when {
                    which < presets.size -> {
                        val base = presets[which]
                        val fresh = base.copy(
                            enabled = true,
                            imei = SimWifiConfig.generateRandomImei(),
                            imsi = SimWifiConfig.generateRandomImsi(base.mccMnc),
                            iccid = SimWifiConfig.generateRandomIccid(),
                            wifiMac = SimWifiConfig.generateRandomMac(),
                            wifiBssid = SimWifiConfig.generateRandomMac()
                        )
                        VEnvironment.updateUserSimWifiConfig(user.id, fresh)
                        reloadSpaces()
                    }
                    which == presets.size -> {
                        val cur = user.simWifiConfig
                        val randomized = cur.copy(
                            enabled = true,
                            imei = SimWifiConfig.generateRandomImei(),
                            imsi = SimWifiConfig.generateRandomImsi(cur.mccMnc),
                            iccid = SimWifiConfig.generateRandomIccid(),
                            wifiMac = SimWifiConfig.generateRandomMac(),
                            wifiBssid = SimWifiConfig.generateRandomMac()
                        )
                        VEnvironment.updateUserSimWifiConfig(user.id, randomized)
                        reloadSpaces()
                    }
                    which == presets.size + 1 -> showCustomSimWifiEditor(user)
                    else -> {
                        VEnvironment.updateUserSimWifiConfig(user.id, user.simWifiConfig.copy(enabled = false))
                        reloadSpaces()
                    }
                }
            }
            .show()
    }

    private fun showCustomSimWifiEditor(user: VUser) {
        val sw = user.simWifiConfig
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val etCarrier = EditText(this).apply { hint = "Carrier Name"; setText(sw.carrierName) }
        val etMccMnc = EditText(this).apply { hint = "MCC+MNC"; setText(sw.mccMnc) }
        val etIso = EditText(this).apply { hint = "Country ISO"; setText(sw.countryIso) }
        val etImei = EditText(this).apply { hint = "15-Digit IMEI"; setText(sw.imei) }
        val etImsi = EditText(this).apply { hint = "15-Digit IMSI"; setText(sw.imsi) }
        val etMac = EditText(this).apply { hint = "Wi-Fi MAC"; setText(sw.wifiMac) }

        layout.addView(etCarrier)
        layout.addView(etMccMnc)
        layout.addView(etIso)
        layout.addView(etImei)
        layout.addView(etImsi)
        layout.addView(etMac)
        scroll.addView(layout)

        AlertDialog.Builder(this)
            .setTitle("Custom SIM & Wi-Fi [Space ${user.id}]")
            .setView(scroll)
            .setPositiveButton("Save") { _, _ ->
                val mcc = etMccMnc.text.toString().ifBlank { "41001" }
                val updated = sw.copy(
                    enabled = true,
                    carrierName = etCarrier.text.toString().ifBlank { "Jazz 4G" },
                    mccMnc = mcc,
                    countryIso = etIso.text.toString().lowercase().ifBlank { "pk" },
                    imei = etImei.text.toString().ifBlank { SimWifiConfig.generateRandomImei() },
                    imsi = etImsi.text.toString().ifBlank { SimWifiConfig.generateRandomImsi(mcc) },
                    wifiMac = etMac.text.toString().ifBlank { SimWifiConfig.generateRandomMac() }
                )
                VEnvironment.updateUserSimWifiConfig(user.id, updated)
                reloadSpaces()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProxyDialog() {
        val user = selectedUser ?: return
        val cfg = user.proxyConfig
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val cbEnable = CheckBox(this).apply { text = "Enable Proxy for Space ${user.id}"; isChecked = cfg.enabled }
        val typeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("SOCKS", "HTTP"))
            setSelection(if (cfg.type.equals("HTTP", true)) 1 else 0)
        }
        val etHost = EditText(this).apply { hint = "Proxy Host / IP"; setText(cfg.host) }
        val etPort = EditText(this).apply { hint = "Port"; inputType = InputType.TYPE_CLASS_NUMBER; setText(cfg.port.toString()) }
        val etUser = EditText(this).apply { hint = "Username (Optional)"; setText(cfg.username) }
        val etPass = EditText(this).apply { hint = "Password (Optional)"; setText(cfg.password) }

        layout.addView(cbEnable)
        layout.addView(typeSpinner)
        layout.addView(etHost)
        layout.addView(etPort)
        layout.addView(etUser)
        layout.addView(etPass)

        AlertDialog.Builder(this)
            .setTitle("Proxy / SOCKS5 [Space ${user.id}]")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newCfg = ProxyConfig(
                    cbEnable.isChecked,
                    typeSpinner.selectedItem.toString(),
                    etHost.text.toString().trim(),
                    etPort.text.toString().toIntOrNull() ?: 1080,
                    etUser.text.toString().trim(),
                    etPass.text.toString().trim()
                )
                VEnvironment.updateUserProxyConfig(user.id, newCfg)
                reloadSpaces()
            }
            .setNeutralButton("Test IP") { _, _ ->
                val testCfg = ProxyConfig(
                    true,
                    typeSpinner.selectedItem.toString(),
                    etHost.text.toString().trim(),
                    etPort.text.toString().toIntOrNull() ?: 1080,
                    etUser.text.toString().trim(),
                    etPass.text.toString().trim()
                )
                VEnvironment.updateUserProxyConfig(user.id, testCfg)
                reloadSpaces()
                Thread {
                    val res = ProxyEngine.testProxyConnection(testCfg)
                    runOnUiThread {
                        AlertDialog.Builder(this).setTitle("Proxy Test").setMessage(res).setPositiveButton("OK", null).show()
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGpsDialog() {
        val user = selectedUser ?: return
        val presets = GpsConfig.presets()
        val items = presets.map { "📍 ${it.label} (${it.latitude}, ${it.longitude})" }.toTypedArray() +
                arrayOf("Custom Coordinates...", "Disable Fake GPS")

        AlertDialog.Builder(this)
            .setTitle("Fake GPS Spoofer [Space ${user.id}]")
            .setItems(items) { _, which ->
                when {
                    which < presets.size -> {
                        VEnvironment.updateUserGpsConfig(user.id, presets[which])
                        reloadSpaces()
                    }
                    which == presets.size -> showCustomGpsEditor(user)
                    else -> {
                        VEnvironment.updateUserGpsConfig(user.id, user.gpsConfig.copy(enabled = false))
                        reloadSpaces()
                    }
                }
            }
            .show()
    }

    private fun showCustomGpsEditor(user: VUser) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(8))
        }
        val etLabel = EditText(this).apply { hint = "City Label"; setText(user.gpsConfig.label) }
        val etLat = EditText(this).apply { hint = "Latitude"; setText(user.gpsConfig.latitude.toString()) }
        val etLng = EditText(this).apply { hint = "Longitude"; setText(user.gpsConfig.longitude.toString()) }
        layout.addView(etLabel)
        layout.addView(etLat)
        layout.addView(etLng)

        AlertDialog.Builder(this)
            .setTitle("Custom GPS [Space ${user.id}]")
            .setView(layout)
            .setPositiveButton("Apply") { _, _ ->
                val newGps = GpsConfig(
                    true,
                    etLat.text.toString().toDoubleOrNull() ?: 31.5204,
                    etLng.text.toString().toDoubleOrNull() ?: 74.3587,
                    etLabel.text.toString().ifBlank { "Custom GPS" }
                )
                VEnvironment.updateUserGpsConfig(user.id, newGps)
                reloadSpaces()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGmsManagerDialog() {
        val user = selectedUser ?: return
        val items = arrayOf(
            "1. Install & Mount GMS Core / Play Services in Space ${user.id}",
            "2. Launch Google Play Store in Space ${user.id}",
            "3. Unlink GMS from Space ${user.id}"
        )
        AlertDialog.Builder(this)
            .setTitle("GMS & Play Store [Space ${user.id}]")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        Thread {
                            val msg = GmsManager.installGmsToSpace(this, user.id)
                            runOnUiThread {
                                AlertDialog.Builder(this).setTitle("GMS Mounted").setMessage(msg).setPositiveButton("OK", null).show()
                                reloadSpaces()
                            }
                        }.start()
                    }
                    1 -> VBoxCore.launchClonedApp(this, user.id, "com.android.vending")
                    2 -> {
                        GmsManager.removeGmsFromSpace(user.id)
                        reloadSpaces()
                    }
                }
            }
            .show()
    }

    private fun showKeyboxDialog() {
        val user = selectedUser ?: return
        val etXml = EditText(this).apply {
            setText(KeyboxEngine.getSavedKeyboxXml())
            minLines = 8
            maxLines = 14
            textSize = 11f
        }
        AlertDialog.Builder(this)
            .setTitle("TrickyStore Keybox.xml")
            .setView(etXml)
            .setPositiveButton("Save Keybox") { _, _ ->
                val res = KeyboxEngine.saveKeyboxXml(etXml.text.toString())
                Toast.makeText(this, res, Toast.LENGTH_LONG).show()
                reloadSpaces()
            }
            .setNeutralButton("Sync /data/adb/tricky_store (!)") { _, _ ->
                KeyboxEngine.saveKeyboxXml(etXml.text.toString())
                Thread {
                    val clonedPkgs = VEnvironment.getClonedPackagesForUser(user.id)
                    val syncMsg = KeyboxEngine.syncWithSystemTrickyStore(clonedPkgs, "!")
                    runOnUiThread {
                        AlertDialog.Builder(this).setTitle("TrickyStore Sync").setMessage(syncMsg).setPositiveButton("OK", null).show()
                        reloadSpaces()
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showXposedManagerDialog() {
        val user = selectedUser ?: return
        val modules = VXposedEngine.scanAllModules(this, user.id)
        val labels = modules.map { "${it.appName}\n(${it.description})" }.toTypedArray()
        val checked = modules.map { it.isEnabled }.toBooleanArray()

        AlertDialog.Builder(this)
            .setTitle("Virtual LSPosed Modules [Space ${user.id}]")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                VXposedEngine.setModuleEnabled(user.id, modules[which].packageName, isChecked)
            }
            .setPositiveButton("Apply Hooks") { _, _ ->
                Toast.makeText(this, "Xposed Hooks applied for Space ${user.id}!", Toast.LENGTH_SHORT).show()
                reloadSpaces()
            }
            .show()
    }

    private fun promptCreateUser() {
        val input = EditText(this).apply { hint = "e.g. Work_Space_2" }
        AlertDialog.Builder(this)
            .setTitle("Create New Virtual Space")
            .setMessage("Har نئے Space ko auto-random Android ID, IMEI, Wi-Fi MAC aur alag Device Model mile ga:")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().ifBlank { "Space_${users.size}" }
                val newUser = VEnvironment.createNewUser(name, users.size)
                selectedUser = newUser
                reloadSpaces()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteUser(user: VUser) {
        if (user.id == 0) {
            Toast.makeText(this, "Primary Space 0 delete nahi ho sakta!", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete ${user.name} (ID:${user.id})?")
            .setMessage("Is space ki tamam cloned apps aur data delete ho jayein ge.")
            .setPositiveButton("Delete") { _, _ ->
                VEnvironment.deleteUser(user.id)
                selectedUser = null
                reloadSpaces()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

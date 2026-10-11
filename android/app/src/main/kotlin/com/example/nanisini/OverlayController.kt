package com.example.nanisini

import android.content.Context
import android.os.Build
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.util.Log

class OverlayController(
    private val context: Context,
    private val onBackConsumed: () -> Unit = {},
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val green = Color.parseColor("#0B6CFF")
    private val ink = Color.parseColor("#111827")
    private val muted = Color.parseColor("#6B7280")
    private val card = Color.parseColor("#F8F9FB")
    private val statusBarHeight = context.resources.getDimensionPixelSize(
        context.resources.getIdentifier("status_bar_height", "dimen", "android").takeIf { it > 0 } ?: 0,
    )

    private var startOverlay: View? = null
    private var expirationOverlay: View? = null
    private var runningCheckOverlay: View? = null
    private var extensionOverlay: View? = null
    private var returnPointOverlay: View? = null
    private val zenRegular: Typeface by lazy {
        runCatching { Typeface.createFromAsset(context.assets, "flutter_assets/assets/fonts/ZenKakuGothicNew-Regular.ttf") }
            .getOrDefault(Typeface.DEFAULT)
    }
    private val zenBold: Typeface by lazy {
        runCatching { Typeface.createFromAsset(context.assets, "flutter_assets/assets/fonts/ZenKakuGothicNew-Bold.ttf") }
            .getOrDefault(Typeface.DEFAULT_BOLD)
    }

    fun showStartOverlay(
        session: ActiveSession,
        appInfo: AppDisplayInfo,
        purposeCategories: List<PurposeCategoryModel>,
        purposeItems: List<PurposeItemModel>,
        activityCategories: List<ActivityCategoryModel>,
        recentActivityDetails: List<String>,
        existingReturnPoint: ReturnPoint?,
        onStart: (SessionDraft) -> Unit,
        onCancel: () -> Unit,
    ): Boolean {
        return showQuickStartOverlay(session, appInfo, purposeCategories, purposeItems, existingReturnPoint, onStart, onCancel)
        @Suppress("UNREACHABLE_CODE")
        dismissAll()
        val root = buildFullScreenRoot()
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(page, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        var selectedCategory: PurposeCategoryModel? = null
        var selectedPurpose: PurposeItemModel? = null
        var purposeText = ""
        var durationMinutes = 0
        var selectedActivity: ActivityCategoryModel? = null
        var activityDetail = ""

        fun replacePage(builder: () -> Unit) {
            page.animate().alpha(0f).translationX(-dp(12).toFloat()).setDuration(100).withEndAction {
                page.removeAllViews()
                builder()
                page.alpha = 0f
                page.translationX = dp(12).toFloat()
                page.animate().alpha(1f).translationX(0f).setDuration(220).start()
                scroll.scrollTo(0, 0)
            }.start()
        }

        fun addAppHeader() {
            val icon = ImageView(context).apply {
                setImageDrawable(appInfo.icon)
                contentDescription = appInfo.label
            }
            page.addView(icon, centeredParams(64, 64))
            page.addView(TextView(context).apply {
                text = appInfo.label
                gravity = Gravity.CENTER
                textSize = 16f
                setTextColor(ink)
                typeface = Typeface.DEFAULT_BOLD
            }, matchWrapParams(top = 8))
        }

        lateinit var showCategories: () -> Unit
        lateinit var showPurposes: () -> Unit
        lateinit var showDuration: () -> Unit
        lateinit var showActivity: () -> Unit
        lateinit var showDetail: () -> Unit

        showCategories = {
            replacePage {
                addAppHeader()
                page.addView(titleTextView("このアプリで何をしますか？"), matchWrapParams(top = 20))
                page.addView(subtitleTextView("カテゴリを選択してください"), matchWrapParams(top = 6, bottom = 14))
                purposeCategories.sortedBy { it.displayOrder }.forEach { category ->
                    page.addView(choiceButton(category.name) {
                        selectedCategory = category
                        selectedPurpose = null
                        purposeText = ""
                        showPurposes()
                    }, matchWrapParams(bottom = 8))
                }
                page.addView(sectionLabel("いつでも選べます"), matchWrapParams(top = 8, bottom = 6))
                page.addView(choiceButton("特に目的はない") {
                    selectedCategory = null; selectedPurpose = null; purposeText = "特に目的はない"; showDuration()
                }, matchWrapParams(bottom = 8))
                page.addView(choiceButton("その他の目的") {
                    selectedCategory = null; selectedPurpose = null; purposeText = "その他の目的"; showPurposes()
                }, matchWrapParams(bottom = 16))
                page.addView(makeSecondaryButton("今は開かない").apply { setOnClickListener { dismissStartOverlay(); onCancel() } })
            }
        }

        showPurposes = {
            replacePage {
                page.addView(appNameText(appInfo.label))
                page.addView(titleTextView("利用目的を選択"), matchWrapParams(top = 12))
                page.addView(subtitleTextView(selectedCategory?.name ?: "その他の目的"), matchWrapParams(top = 4, bottom = 12))
                val available = purposeItems.filter { it.categoryId == selectedCategory?.id && it.label !in fixedPurposes }
                available.forEach { item ->
                    page.addView(choiceButton(item.label) {
                        selectedPurpose = item
                        purposeText = item.label
                        showDuration()
                    }, matchWrapParams(bottom = 8))
                }
                page.addView(sectionLabel("その他"), matchWrapParams(top = 12))
                fixedPurposes.forEach { label ->
                    page.addView(choiceButton(label) {
                        selectedPurpose = null
                        purposeText = label
                        showDuration()
                    }, matchWrapParams(bottom = 8))
                }
                page.addView(makeSecondaryButton("戻る").apply { setOnClickListener { showCategories() } }, matchWrapParams(top = 8))
            }
        }

        showDuration = {
            replacePage {
                page.addView(appNameText(appInfo.label))
                page.addView(titleTextView("今回の利用時間"), matchWrapParams(top = 12))
                page.addView(subtitleTextView("まずおおよその時間帯を選び、次に具体的な時間を選択してください"), matchWrapParams(top = 4, bottom = 12))
                val durationPicker = createDurationPicker { minutes ->
                    durationMinutes = minutes
                    showActivity()
                }
                page.addView(durationPicker.view)
                page.addView(makeSecondaryButton("戻る").apply {
                    setOnClickListener { if (purposeText == "特に目的はない") showCategories() else showPurposes() }
                }, matchWrapParams(top = 16))
            }
        }

        showActivity = {
            replacePage {
                page.addView(appNameText(appInfo.label))
                page.addView(titleTextView("開く前にしていたこと"), matchWrapParams(top = 12))
                page.addView(subtitleTextView("中断する作業を記録します"), matchWrapParams(top = 4, bottom = 12))
                page.addView(sectionLabel("ジャンル"))
                val activityGroup = RadioGroup(context).apply {
                    orientation = RadioGroup.VERTICAL
                }
                activityCategories.sortedBy { it.displayOrder }.forEach { category ->
                    activityGroup.addView(radioButton(category.name).apply {
                        id = View.generateViewId()
                        isChecked = selectedActivity?.id == category.id
                        setOnClickListener {
                            selectedActivity = category
                            showDetail()
                        }
                    })
                }
                page.addView(activityGroup)
                page.addView(makeSecondaryButton("戻る").apply { setOnClickListener { showDuration() } }, matchWrapParams(top = 14))
            }
        }

        showDetail = {
            replacePage {
                page.addView(appNameText(appInfo.label))
                page.addView(titleTextView("開く前にしていたことの詳細"), matchWrapParams(top = 12))
                page.addView(subtitleTextView("具体的な内容を入力してください（必須）"), matchWrapParams(top = 4, bottom = 12))
                if (recentActivityDetails.isNotEmpty()) {
                    page.addView(sectionLabel("過去24時間の入力"), matchWrapParams(bottom = 6))
                    recentActivityDetails.forEach { previous ->
                        page.addView(choiceButton(previous) {
                            activityDetail = previous
                            showDetail()
                        }, matchWrapParams(bottom = 8))
                    }
                }
                page.addView(sectionLabel("詳細（必須）"), matchWrapParams(top = 14))
                val detail = EditText(context).apply {
                    hint = "例：企画書の続きを作成していた"
                    setText(activityDetail)
                    minLines = 3; maxLines = 4
                    filters = arrayOf(InputFilter.LengthFilter(100))
                    gravity = Gravity.TOP
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = roundedBackground(Color.WHITE, Color.parseColor("#D1D5DB"), dp(16))
                }
                page.addView(detail)
                page.addView(subtitleTextView("必須・最大100文字"), matchWrapParams(top = 4))
                val error = errorText(); page.addView(error)
                page.addView(buttonRow("戻る", { activityDetail = detail.text.toString(); showActivity() }, "利用を始める", {
                    activityDetail = detail.text.toString().trim()
                    if (activityDetail.isBlank()) {
                        error.showError("詳細を入力してください")
                        return@buttonRow
                    }
                    val draft = SessionDraft(
                        packageName = session.packageName,
                        applicationLabel = session.applicationLabel.ifBlank { appInfo.label },
                        purposeCategoryId = selectedCategory?.id,
                        purposeItemId = selectedPurpose?.id,
                        purposeText = purposeText,
                        durationMillis = durationMinutes * 60_000L,
                        currentActivityCategoryId = selectedActivity?.id,
                        currentActivityCategoryName = selectedActivity?.name,
                        currentActivityDetail = activityDetail,
                    )
                    // Remove the modal before the callback adds the timer bar (or
                    // resumes a suspended expiry), so WindowManager never races two views.
                    dismissStartOverlay(); onStart(draft)
                }), matchWrapParams(top = 14))
                page.addView(makeSecondaryButton("今は開かない").apply { setOnClickListener { dismissStartOverlay(); onCancel() } }, matchWrapParams(top = 8))
            }
        }
        showCategories()
        return addWindow(root, fullscreenLayoutParams()).also { if (it) startOverlay = root }
    }

    private fun showQuickStartOverlay(
        session: ActiveSession,
        appInfo: AppDisplayInfo,
        categories: List<PurposeCategoryModel>,
        purposeItems: List<PurposeItemModel>,
        existingReturnPoint: ReturnPoint?,
        onStart: (SessionDraft) -> Unit,
        onCancel: () -> Unit,
    ): Boolean {
        dismissAll()
        val root = buildFullScreenRoot()
        val page = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(22), dp(24), dp(22)) }
        val scroll = ScrollView(context).apply { isFillViewport = true; addView(page, ViewGroup.LayoutParams(-1, -2)) }
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        var category: PurposeCategoryModel? = null
        var purposeText = ""
        var durationMinutes = 5
        var returnPoint = existingReturnPoint?.detail.orEmpty()
        fun header(title: String, subtitle: String? = null) {
            page.addView(ImageView(context).apply { setImageDrawable(appInfo.icon); contentDescription = appInfo.label }, centeredParams(56, 56))
            page.addView(appNameText(appInfo.label), matchWrapParams(top = 8))
            page.addView(titleTextView(title), matchWrapParams(top = 18))
            subtitle?.let { page.addView(subtitleTextView(it), matchWrapParams(top = 5, bottom = 14)) }
        }
        fun replace(block: () -> Unit) { page.removeAllViews(); block(); scroll.scrollTo(0, 0) }
        fun input(hintText: String, value: String) = object : EditText(context) {
            override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
                if (keyCode != KeyEvent.KEYCODE_BACK) return super.onKeyPreIme(keyCode, event)
                if (event.action == KeyEvent.ACTION_UP) {
                    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(windowToken, 0)
                    clearFocus()
                    root.requestFocus()
                }
                return true
            }
        }.apply {
            hint = hintText; setText(value); minLines = 3; maxLines = 5
            filters = arrayOf(InputFilter.LengthFilter(100)); gravity = Gravity.TOP
            background = roundedBackground(Color.WHITE, Color.parseColor("#D1D5DB"), dp(14))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        lateinit var showGenres: () -> Unit
        lateinit var showPurposeDetail: () -> Unit
        lateinit var showTime: () -> Unit
        lateinit var showContext: () -> Unit
        showContext = {
            replace {
                header("終わったらどこに戻る？", "$purposeText ・ ${durationMinutes}分")
                page.addView(subtitleTextView("スマホを開く前の行動や、本来やろうとしていたことを選びます"), matchWrapParams(bottom = 14))
                val presets = listOf("アプリ開発", "勉強", "読書")
                presets.forEach { value ->
                    page.addView(choiceButton(if (returnPoint == value) "✓ $value" else value) {
                        returnPoint = value
                        showContext()
                    }, matchWrapParams(bottom = 8))
                }
                val custom = input("自由入力（任意）", returnPoint.takeUnless { it in presets }.orEmpty()).apply {
                    minLines = 2
                    maxLines = 3
                }
                page.addView(custom, matchWrapParams(top = 6))
                page.addView(makePrimaryButton("${appInfo.label}を開く").apply { setOnClickListener {
                    val typed = custom.text.toString().trim()
                    if (typed.isNotBlank()) returnPoint = typed
                    dismissStartOverlay()
                    onStart(SessionDraft(
                        packageName = session.packageName,
                        applicationLabel = session.applicationLabel.ifBlank { appInfo.label },
                        purposeCategoryId = category?.id,
                        purposeItemId = null,
                        purposeText = purposeText,
                        durationMillis = durationMinutes * 60_000L,
                        currentActivityCategoryId = null,
                        currentActivityCategoryName = null,
                        currentActivityDetail = returnPoint,
                        nextActionDetail = returnPoint,
                    ))
                } }, matchWrapParams(top = 14))
                page.addView(makeSecondaryButton("戻る").apply { setOnClickListener { showTime() } }, matchWrapParams(top = 8))
                page.addView(makeSecondaryButton("今は開かない").apply { setOnClickListener { dismissStartOverlay();onCancel() } }, matchWrapParams(top = 8))
            }
        }
        showTime = {
            replace {
                header("どれくらい使う？", purposeText)
                page.addView(sectionLabel("よく使う時間"), matchWrapParams(bottom = 6))
                val quickRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                val quickScroll = HorizontalScrollView(context).apply {
                    isHorizontalScrollBarEnabled = false
                    addView(quickRow, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
                page.addView(quickScroll, matchWrapParams(bottom = 8))

                lateinit var confirmButton: Button
                fun updateDuration(minutes: Int) {
                    durationMinutes = minutes.coerceIn(1, 180)
                    confirmButton.text = "${durationMinutes}分にする"
                }

                val picker = NumberPicker(context).apply {
                    minValue = 1
                    maxValue = 180
                    value = durationMinutes.coerceIn(1, 180)
                    wrapSelectorWheel = false
                    contentDescription = "利用時間、分。上下にスワイプして1分単位で選択"
                    setOnValueChangedListener { _, _, newValue -> updateDuration(newValue) }
                }
                listOf(5, 10, 15, 30).forEach { minutes ->
                    quickRow.addView(makeSecondaryButton("${minutes}分").apply {
                        contentDescription = "利用時間を${minutes}分に設定"
                        setOnClickListener {
                            picker.value = minutes
                            updateDuration(minutes)
                        }
                    }, LinearLayout.LayoutParams(dp(76), dp(48)).apply { marginEnd = dp(8) })
                }
                page.addView(picker, centeredParams(180, 180, top = 8))
                page.addView(subtitleTextView("詳細設定：上下にスワイプして1分単位で調整できます"), matchWrapParams(bottom = 8))

                val adjustRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                listOf(-5, -1, 1, 5).forEach { amount ->
                    val label = if (amount > 0) "+$amount" else amount.toString()
                    adjustRow.addView(makeSecondaryButton(label).apply {
                        contentDescription = if (amount > 0) "${amount}分増やす" else "${-amount}分減らす"
                        setOnClickListener {
                            val next = (picker.value + amount).coerceIn(1, 180)
                            picker.value = next
                            updateDuration(next)
                        }
                    }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                        if (amount != -5) marginStart = dp(6)
                    })
                }
                page.addView(adjustRow, matchWrapParams(bottom = 14))

                confirmButton = makePrimaryButton("${durationMinutes}分にする").apply {
                    setOnClickListener { durationMinutes=picker.value;showContext() }
                }
                page.addView(confirmButton)
                page.addView(makeSecondaryButton("戻る").apply { setOnClickListener { durationMinutes=picker.value;showPurposeDetail() } }, matchWrapParams(top = 8))
            }
        }
        showPurposeDetail = {
            replace {
                header("${appInfo.label}をどうして開く？", category?.name)
                val detail = input("もう少し具体的に書く（任意）", purposeText); page.addView(detail)
                page.addView(subtitleTextView("カテゴリだけでも次へ進めます"), matchWrapParams(top = 6))
                page.addView(makePrimaryButton("時間を決める").apply { setOnClickListener {
                    purposeText = detail.text.toString().trim().ifBlank { category?.name ?: "その他" }
                    showTime()
                } },matchWrapParams(top=14))
                page.addView(makeSecondaryButton("戻る").apply { setOnClickListener { purposeText=detail.text.toString();showGenres() } },matchWrapParams(top=8))
            }
        }
        showGenres = {
            replace {
                header("何しに開く？", "まずジャンルを選んでください")
                categories.sortedBy { it.displayOrder }.forEach { item -> page.addView(choiceButton(item.name) { category=item;showPurposeDetail() },matchWrapParams(bottom=8)) }
                page.addView(choiceButton("その他") { category=null;showPurposeDetail() },matchWrapParams(bottom=12))
                page.addView(makeSecondaryButton("今は開かない").apply { setOnClickListener { dismissStartOverlay();onCancel() } })
            }
        }
        showGenres()
        return addWindow(root, fullscreenLayoutParams()).also { if(it) startOverlay=root }
    }

    /** The normal repeat-open flow keeps real-world context, purpose and time together. */
    private fun showCompactStartOverlay(
        session: ActiveSession,
        appInfo: AppDisplayInfo,
        purposeItems: List<PurposeItemModel>,
        returnPoint: ReturnPoint,
        onStart: (SessionDraft) -> Unit,
        onCancel: () -> Unit,
    ): Boolean {
        dismissAll()
        val root = buildFullScreenRoot()
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(page, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        var selectedPurpose: PurposeItemModel? = null
        var purposeText = ""
        var durationMinutes = 0
        var customPurposeMode = false

        fun createDraft() = SessionDraft(
            packageName = session.packageName,
            applicationLabel = session.applicationLabel.ifBlank { appInfo.label },
            purposeCategoryId = selectedPurpose?.categoryId,
            purposeItemId = selectedPurpose?.id,
            purposeText = purposeText,
            durationMillis = durationMinutes * 60_000L,
            currentActivityCategoryId = returnPoint.categoryId,
            currentActivityCategoryName = returnPoint.category,
            currentActivityDetail = returnPoint.detail,
        )

        fun addContextHeader() {
            page.addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(ImageView(context).apply {
                    setImageDrawable(appInfo.icon)
                    contentDescription = appInfo.label
                }, LinearLayout.LayoutParams(dp(52), dp(52)).apply { marginEnd = dp(12) })
                addView(titleTextView(appInfo.label), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            })
            val activity = returnPoint.detail.ifBlank { returnPoint.category.orEmpty() }.ifBlank { "今の行動" }
            page.addView(infoCard("開く前は", "${activity}をしていました"), matchWrapParams(top = 14))
            page.addView(titleTextView("何しに開く？"), matchWrapParams(top = 18, bottom = 10))
        }

        lateinit var render: () -> Unit
        lateinit var confirmNoPurpose: () -> Unit

        confirmNoPurpose = {
            page.removeAllViews()
            addContextHeader()
            page.addView(infoCard("利用目的", "特に目的はありません。"), matchWrapParams(top = 8))
            page.addView(titleTextView("それでも${appInfo.label}を開きますか？"), matchWrapParams(top = 18))
            page.addView(makePrimaryButton("開く").apply {
                setOnClickListener { dismissStartOverlay(); onStart(createDraft()) }
            }, matchWrapParams(top = 16))
            page.addView(makeSecondaryButton("やめる").apply {
                setOnClickListener { dismissStartOverlay(); onCancel() }
            }, matchWrapParams(top = 8))
            scroll.scrollTo(0, 0)
        }

        render = {
            page.removeAllViews()
            addContextHeader()
            if (purposeText.isBlank()) {
                purposeItems.asSequence()
                    .filter { it.label !in fixedPurposes }
                    .distinctBy { it.label }
                    .forEach { item ->
                        page.addView(choiceButton(item.label) {
                            selectedPurpose = item
                            purposeText = item.label
                            customPurposeMode = false
                            render()
                        }, matchWrapParams(bottom = 8))
                    }
                if (customPurposeMode) {
                    val custom = EditText(context).apply {
                        hint = "目的を入力"
                        filters = arrayOf(InputFilter.LengthFilter(60))
                        background = roundedBackground(Color.WHITE, Color.parseColor("#D1D5DB"), dp(16))
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                    }
                    page.addView(custom, matchWrapParams(bottom = 8))
                    page.addView(makePrimaryButton("この目的にする").apply {
                        setOnClickListener {
                            val value = custom.text.toString().trim()
                            if (value.isNotBlank()) { purposeText = value; selectedPurpose = null; render() }
                        }
                    }, matchWrapParams(bottom = 8))
                } else {
                    page.addView(choiceButton("その他") { customPurposeMode = true; render() }, matchWrapParams(bottom = 8))
                }
                page.addView(choiceButton("特に目的はない") {
                    selectedPurpose = null
                    purposeText = "特に目的はない"
                    render()
                }, matchWrapParams(bottom = 12))
            } else {
                page.addView(infoCard("利用目的", purposeText))
                page.addView(makeSecondaryButton("目的を選び直す").apply {
                    setOnClickListener { selectedPurpose = null; purposeText = ""; customPurposeMode = false; durationMinutes = 0; render() }
                }, matchWrapParams(top = 8))
                page.addView(titleTextView("何分使う？"), matchWrapParams(top = 18, bottom = 10))
                listOf(3, 5, 10, 15, 30).forEach { minutes ->
                    val label = if (durationMinutes == minutes) "✓ ${minutes}分" else "${minutes}分"
                    page.addView(choiceButton(label) { durationMinutes = minutes; render() }, matchWrapParams(bottom = 8))
                }
                val custom = EditText(context).apply {
                    hint = "自由設定（1〜180分）"
                    inputType = InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(3))
                    if (durationMinutes !in listOf(0, 3, 5, 10, 15, 30)) setText(durationMinutes.toString())
                    background = roundedBackground(Color.WHITE, Color.parseColor("#D1D5DB"), dp(16))
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                }
                page.addView(custom, matchWrapParams(bottom = 8))
                page.addView(makePrimaryButton("利用を始める").apply {
                    setOnClickListener {
                        val customMinutes = custom.text.toString().toIntOrNull()
                        if (custom.text.isNotBlank()) durationMinutes = customMinutes?.takeIf { it in 1..180 } ?: 0
                        if (durationMinutes !in 1..180) return@setOnClickListener
                        if (purposeText == "特に目的はない") confirmNoPurpose()
                        else { dismissStartOverlay(); onStart(createDraft()) }
                    }
                }, matchWrapParams(top = 8))
            }
            page.addView(makeSecondaryButton("今は開かない").apply {
                setOnClickListener { dismissStartOverlay(); onCancel() }
            }, matchWrapParams(top = 10, bottom = 16))
            scroll.scrollTo(0, 0)
        }

        render()
        return addWindow(root, fullscreenLayoutParams()).also { if (it) startOverlay = root }
    }

    fun showReviewCaptchaOverlay(
        appInfo: AppDisplayInfo,
        purposeText: String,
        question: ReviewQuestionModel?,
        onAnswered: (String, Boolean) -> Unit,
        onContinue: () -> Unit,
    ): Boolean {
        dismissAll()
        val root = buildFullScreenRoot()
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(page, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        fun proceed() { dismissStartOverlay(); onContinue() }
        fun showResult(correct: Boolean) {
            page.removeAllViews()
            page.addView(TextView(context).apply { text=if(correct) "✓" else "!"; textSize=28f; setTextColor(if(correct) green else Color.parseColor("#B66A12")) })
            page.addView(titleTextView(if (correct) "正解" else "もう一度確認"), matchWrapParams(top=6))
            if (!correct) page.addView(infoCard("正解", question?.answer.orEmpty()), matchWrapParams(top = 14))
            page.addView(infoCard("目的", "「$purposeText」"), matchWrapParams(top = 14))
            page.addView(subtitleTextView("${appInfo.label}を開きます"), matchWrapParams(top = 14))
            page.addView(makePrimaryButton(if (correct) "開く" else "確認して開く").apply {
                setOnClickListener { proceed() }
            }, matchWrapParams(top = 18))
            scroll.scrollTo(0, 0)
        }

        page.addView(titleTextView("復習問題"))
        page.addView(subtitleTextView("$purposeText ・ ${appInfo.label}"), matchWrapParams(top = 6, bottom = 18))
        if (question == null) {
            page.addView(infoCard("復習問題", "問題が登録されていません"))
            page.addView(infoCard("目的", "「$purposeText」"), matchWrapParams(top = 10))
            page.addView(makePrimaryButton("このまま開く").apply { setOnClickListener { proceed() } }, matchWrapParams(top = 18))
        } else {
            val optionPattern = Regex("^(?:選択肢\\s*)?[A-CＡ-Ｃ][.．、:：)）\\s]+(.+)$")
            val lines = question.question.lines().map(String::trim).filter(String::isNotEmpty)
            val options = lines.mapNotNull { optionPattern.matchEntire(it)?.groupValues?.get(1)?.trim() }
            val questionText = lines.filterNot { optionPattern.matches(it) }.joinToString("\n")
            page.addView(titleTextView(questionText))
            if (options.size == 3) {
                options.forEach { option ->
                    page.addView(choiceButton(option) {
                        val correct = ReviewAnswerEvaluator.isCorrect(option, question.answer)
                        onAnswered(question.id, correct)
                        showResult(correct)
                    }, matchWrapParams(top = 10))
                }
            } else {
                // Keep old questions usable until they are edited into the new
                // three-choice format.
                val answer = EditText(context).apply {
                    hint = "回答を入力"
                    maxLines = 3
                    background = roundedBackground(Color.WHITE, Color.parseColor("#D1D5DB"), dp(16))
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                }
                page.addView(answer, matchWrapParams(top = 18))
                page.addView(makePrimaryButton("回答する").apply {
                    setOnClickListener {
                        if (answer.text.toString().trim().isEmpty()) return@setOnClickListener
                        val correct = ReviewAnswerEvaluator.isCorrect(answer.text.toString(), question.answer)
                        onAnswered(question.id, correct)
                        showResult(correct)
                    }
                }, matchWrapParams(top = 12))
            }
        }
        return addWindow(root, fullscreenLayoutParams()).also { if (it) startOverlay = root }
    }

    fun showExpirationOverlay(
        session: ActiveSession,
        appInfo: AppDisplayInfo,
        onReturn: () -> Unit,
        onExtend: () -> Unit,
    ): Boolean {
        dismissExpirationOverlay()
        val root = buildFullScreenRoot()
        val nextAction = session.nextActionDetail.orEmpty().ifBlank { "元の行動に戻る" }
        val content = centeredCard("${minutesText(session.durationMillis)}経ちました", "最初の目的「${session.purposeText.orEmpty()}」")
        content.addView(TextView(context).apply {
            text = nextAction
            textSize = 30f
            setTextColor(ink)
            typeface = zenBold
            gravity = Gravity.START
            setLineSpacing(dp(3).toFloat(), 1f)
            contentDescription = "次にやること、$nextAction"
        }, matchWrapParams(bottom = 20))
        content.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(context).apply { setImageDrawable(appInfo.icon); adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(10) })
            addView(appNameText(appInfo.label), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }, matchWrapParams(bottom = 12))
        content.addView(infoCard("今回すること", session.purposeText.orEmpty()))
        content.addView(subtitleTextView("今どうしますか？"), matchWrapParams(top = 14))
        content.addView(makePrimaryButton("戻る").apply {
            setOnClickListener { showEncouragement(root, nextAction, onReturn) }
        }, matchWrapParams(top = 18))
        content.addView(makeSecondaryButton("延長する").apply { setOnClickListener { onExtend() } }, matchWrapParams(top = 8))
        root.addView(content, centeredOverlayParams())
        val shown = addWindow(root, fullscreenLayoutParams())
        if (shown) { expirationOverlay = root; root.alpha = 0f; root.animate().alpha(1f).setDuration(220).start() }
        return shown
    }

    fun showRunningCheckOverlay(
        session: ActiveSession,
        appInfo: AppDisplayInfo,
        remainingMillis: Long,
        onNext: () -> Unit,
        onContinue: () -> Unit,
    ): Boolean {
        dismissRunningCheckOverlay()
        val root = buildFullScreenRoot()
        val nextAction = session.nextActionDetail.orEmpty().ifBlank { "元の行動に戻る" }
        val content = centeredCard(
            "次にやること",
            "残り ${TimerNotificationController.formatRemaining(remainingMillis)}",
        )
        content.addView(TextView(context).apply {
            text = nextAction
            textSize = 30f
            setTextColor(ink)
            typeface = zenBold
            setLineSpacing(dp(3).toFloat(), 1f)
            contentDescription = "次にやること、$nextAction"
        }, matchWrapParams(bottom = 20))
        content.addView(infoCard("今していること", session.purposeText.orEmpty()))
        content.addView(subtitleTextView("ここで切り替えるか、設定した時間まで続けられます"), matchWrapParams(top = 14))
        content.addView(makePrimaryButton("次に進む").apply {
            setOnClickListener { showEncouragement(root, nextAction, onNext) }
        }, matchWrapParams(top = 18))
        content.addView(makeSecondaryButton("${appInfo.label}をまだ使う").apply {
            setOnClickListener { onContinue() }
        }, matchWrapParams(top = 8))
        root.addView(content, centeredOverlayParams())
        val shown = addWindow(root, fullscreenLayoutParams())
        if (shown) runningCheckOverlay = root
        return shown
    }

    fun showReturnPointOverlay(returnPoint: ReturnPoint?, completed: Boolean, onReturn: () -> Unit): Boolean {
        dismissReturnPointOverlay()
        val root = buildFullScreenRoot()
        val title = if (completed) "目的を達成しました" else "最初の目的から外れていました"
        val content = centeredCard(title, "戻る場所を確認しましょう")
        val value = listOfNotNull(returnPoint?.category?.takeIf { it.isNotBlank() }, returnPoint?.detail?.takeIf { it.isNotBlank() }).joinToString("\n").ifBlank { "未設定" }
        content.addView(infoCard("戻る場所", value))
        content.addView(makePrimaryButton("戻る").apply { setOnClickListener { dismissReturnPointOverlay(); onReturn() } }, matchWrapParams(top = 18))
        root.addView(content, centeredOverlayParams())
        return addWindow(root, fullscreenLayoutParams()).also { if (it) returnPointOverlay = root }
    }

    private fun showEncouragement(root: FrameLayout, nextAction: String, onStart: () -> Unit) {
        root.removeAllViews()
        val content = centeredCard("次の行動へ", "切り替える準備ができました").apply {
            gravity = Gravity.CENTER_HORIZONTAL
            for (index in 0 until childCount) {
                (getChildAt(index) as? TextView)?.gravity = Gravity.CENTER
            }
        }
        content.addView(TextView(context).apply {
            text = "「$nextAction」\n頑張ってください"
            textSize = 28f
            setTextColor(ink)
            typeface = zenBold
            gravity = Gravity.CENTER
            setLineSpacing(dp(6).toFloat(), 1.08f)
            contentDescription = "$nextAction、頑張ってください"
        }, matchWrapParams(bottom = 20))
        content.addView(makePrimaryButton("始める").apply {
            setOnClickListener { onStart() }
        }, matchWrapParams())
        root.addView(content, centeredOverlayParams())
    }

    @Suppress("unused")
    private fun showCompactExtensionOverlay(
        session: ActiveSession,
        appInfo: AppDisplayInfo,
        purposeCategories: List<PurposeCategoryModel>,
        purposeItems: List<PurposeItemModel>,
        onExtend: (SessionDraft) -> Unit,
        onClose: () -> Unit,
    ): Boolean {
        dismissExtensionOverlay()
        val root = buildFullScreenRoot()
        val content = centeredCard("あと何分使いますか？", "目的はそのまま維持されます")
        content.addView(infoCard("目的", session.purposeText.orEmpty()))
        fun choose(minutes: Int) {
            dismissExtensionOverlay()
            onExtend(
                SessionDraft(
                    session.packageName, session.applicationLabel.ifBlank { appInfo.label },
                    session.purposeCategoryId, session.purposeItemId, session.purposeText,
                    minutes * 60_000L, session.currentActivityCategoryId,
                    session.currentActivityCategoryName, session.currentActivityDetail,
                ),
            )
        }
        listOf(3, 5, 10, 15).forEach { minutes ->
            content.addView(choiceButton("${minutes}分") { choose(minutes) }, matchWrapParams(top = 8))
        }
        val custom = EditText(context).apply {
            hint = "自由設定（1〜180分）"
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(3))
            background = roundedBackground(Color.WHITE, Color.parseColor("#D1D5DB"), dp(16))
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        content.addView(custom, matchWrapParams(top = 8))
        content.addView(makeSecondaryButton("自由設定で次へ").apply {
            setOnClickListener { custom.text.toString().toIntOrNull()?.takeIf { it in 1..180 }?.let(::choose) }
        }, matchWrapParams(top = 8))
        content.addView(makeSecondaryButton("終了").apply { setOnClickListener { onClose() } }, matchWrapParams(top = 12))
        root.addView(content, centeredOverlayParams())
        return addWindow(root, fullscreenLayoutParams()).also { if (it) extensionOverlay = root }
    }

    fun showExtensionOverlay(
        session: ActiveSession,
        appInfo: AppDisplayInfo,
        purposeCategories: List<PurposeCategoryModel>,
        purposeItems: List<PurposeItemModel>,
        onExtend: (SessionDraft) -> Unit,
        onClose: () -> Unit,
    ): Boolean {
        dismissExtensionOverlay()
        val root = buildFullScreenRoot()
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(page, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        var samePurpose = true
        var selectedCategory: PurposeCategoryModel? = null
        var selectedPurpose: PurposeItemModel? = null
        var purposeText = session.purposeText.orEmpty()
        var durationMinutes = 0

        fun replacePage(builder: () -> Unit) {
            page.animate().alpha(0f).translationX(-dp(12).toFloat()).setDuration(100).withEndAction {
                page.removeAllViews(); builder()
                page.alpha = 0f; page.translationX = dp(12).toFloat()
                page.animate().alpha(1f).translationX(0f).setDuration(220).start()
                scroll.scrollTo(0, 0)
            }.start()
        }

        lateinit var showPurposeChoice: () -> Unit
        lateinit var showCategories: () -> Unit
        lateinit var showPurposes: () -> Unit
        lateinit var showDuration: () -> Unit
        lateinit var showConfirmation: () -> Unit

        showPurposeChoice = {
            replacePage {
                page.addView(appNameText(appInfo.label))
                page.addView(titleTextView("利用目的"), matchWrapParams(top = 12))
                page.addView(subtitleTextView("これまでと同じ目的で延長しますか？"), matchWrapParams(top = 4, bottom = 14))
                page.addView(infoCard("現在の利用目的", session.purposeText.orEmpty()))
                page.addView(choiceButton("同じ目的") {
                    samePurpose = true
                    selectedCategory = null; selectedPurpose = null
                    purposeText = session.purposeText.orEmpty()
                    showDuration()
                }, matchWrapParams(top = 14, bottom = 8))
                page.addView(choiceButton("そうでない") {
                    samePurpose = false
                    purposeText = ""
                    showCategories()
                }, matchWrapParams(bottom = 8))
                page.addView(makeSecondaryButton("アプリを閉じる").apply { setOnClickListener { onClose() } }, matchWrapParams(top = 8))
            }
        }

        showCategories = {
            replacePage {
                page.addView(appNameText(appInfo.label))
                page.addView(titleTextView("このアプリで何をしますか？"), matchWrapParams(top = 12))
                purposeCategories.sortedBy { it.displayOrder }.forEach { category ->
                    page.addView(choiceButton(category.name) {
                        selectedCategory = category; selectedPurpose = null; purposeText = ""; showPurposes()
                    }, matchWrapParams(bottom = 8))
                }
                page.addView(choiceButton("特に目的はない") {
                    selectedCategory = null; selectedPurpose = null; purposeText = "特に目的はない"; showDuration()
                }, matchWrapParams(bottom = 8))
                page.addView(choiceButton("その他の目的") {
                    selectedCategory = null; selectedPurpose = null; purposeText = "その他の目的"; showPurposes()
                }, matchWrapParams(bottom = 8))
                page.addView(makeSecondaryButton("戻る").apply { setOnClickListener { showPurposeChoice() } }, matchWrapParams(top = 8))
            }
        }

        showPurposes = {
            replacePage {
                page.addView(appNameText(appInfo.label))
                page.addView(titleTextView("利用目的を選択"), matchWrapParams(top = 12))
                val available = purposeItems.filter { it.categoryId == selectedCategory?.id && it.label !in fixedPurposes }
                available.forEach { item ->
                    page.addView(choiceButton(item.label) {
                        selectedPurpose = item; purposeText = item.label; showDuration()
                    }, matchWrapParams(bottom = 8))
                }
                fixedPurposes.forEach { label ->
                    page.addView(choiceButton(label) {
                        selectedPurpose = null; purposeText = label; showDuration()
                    }, matchWrapParams(bottom = 8))
                }
                page.addView(makeSecondaryButton("戻る").apply { setOnClickListener { showCategories() } }, matchWrapParams(top = 8))
            }
        }

        showDuration = {
            replacePage {
                page.addView(appNameText(appInfo.label))
                page.addView(titleTextView("今回の利用時間"), matchWrapParams(top = 12))
                val picker = createDurationPicker { minutes ->
                    durationMinutes = minutes
                    showConfirmation()
                }
                page.addView(picker.view, matchWrapParams(top = 12))
                page.addView(makeSecondaryButton("戻る").apply {
                    setOnClickListener { if (samePurpose) showPurposeChoice() else showPurposes() }
                }, matchWrapParams(top = 14))
            }
        }

        showConfirmation = {
            replacePage {
                page.addView(appNameText(appInfo.label))
                page.addView(titleTextView("それでも延長しますか？"), matchWrapParams(top = 12))
                page.addView(infoCard("延長前はこれをやっていた", activityText(session)), matchWrapParams(top = 12))
                page.addView(infoCard("延長後の利用目的", purposeText), matchWrapParams(top = 10))
                page.addView(infoCard("延長時間", "${durationMinutes}分"), matchWrapParams(top = 10))
                page.addView(makePrimaryButton("それでも延長する").apply { setOnClickListener {
                    val draft = SessionDraft(
                        session.packageName, session.applicationLabel.ifBlank { appInfo.label },
                        if (samePurpose) session.purposeCategoryId else selectedCategory?.id,
                        if (samePurpose) session.purposeItemId else selectedPurpose?.id,
                        purposeText, durationMinutes * 60_000L,
                        session.currentActivityCategoryId,
                        session.currentActivityCategoryName,
                        session.currentActivityDetail,
                    )
                    dismissExtensionOverlay(); onExtend(draft)
                } }, matchWrapParams(top = 16))
                page.addView(makeSecondaryButton("戻る").apply {
                    setOnClickListener { showDuration() }
                }, matchWrapParams(top = 8))
                page.addView(makeSecondaryButton("アプリを閉じる").apply { setOnClickListener { onClose() } }, matchWrapParams(top = 8))
            }
        }

        showPurposeChoice()
        return addWindow(root, fullscreenLayoutParams()).also { if (it) extensionOverlay = root }
    }

    fun hideStartOverlay() = dismissStartOverlay()
    fun isStartOverlayShowing(): Boolean = startOverlay != null
    fun hideExpirationOverlay() = dismissExpirationOverlay()
    fun isExpirationOverlayShowing(): Boolean = expirationOverlay != null
    fun hideRunningCheckOverlay() = dismissRunningCheckOverlay()
    fun isRunningCheckOverlayShowing(): Boolean = runningCheckOverlay != null
    fun hideExtensionOverlay() = dismissExtensionOverlay()
    fun hideReturnPointOverlay() = dismissReturnPointOverlay()
    fun dismissAll() { dismissStartOverlay(); dismissExpirationOverlay(); dismissRunningCheckOverlay(); dismissExtensionOverlay(); dismissReturnPointOverlay() }

    private fun centeredCard(title: String, subtitle: String) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(20))
        background = roundedBackground(Color.WHITE, Color.parseColor("#E5E7EB"), dp(20)); elevation = dp(4).toFloat()
        addView(titleTextView(title)); addView(subtitleTextView(subtitle), matchWrapParams(top = 6, bottom = 16))
    }
    private fun choiceButton(label: String, action: () -> Unit) = makeSecondaryButton(label).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL; setOnClickListener { action() } }
    private fun radioButton(label: String) = RadioButton(context).apply { text = label; textSize = 15f; setTextColor(ink); minHeight = dp(48); buttonTintList = android.content.res.ColorStateList.valueOf(green) }
    private fun appNameText(label: String) = TextView(context).apply { text = label; textSize = 14f; setTextColor(muted); typeface = Typeface.DEFAULT_BOLD }
    private fun sectionLabel(label: String) = TextView(context).apply { text = label; textSize = 13f; setTextColor(muted); typeface = Typeface.DEFAULT_BOLD }
    private fun errorText() = TextView(context).apply { textSize = 13f; setTextColor(Color.parseColor("#B91C1C")); visibility = View.GONE }
    private fun TextView.showError(message: String) { text = message; visibility = View.VISIBLE }
    private fun buttonRow(left: String, onLeft: () -> Unit, right: String, onRight: () -> Unit) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(makeSecondaryButton(left).apply { setOnClickListener { onLeft() } }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(6) })
        addView(makePrimaryButton(right).apply { setOnClickListener { onRight() } }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginStart = dp(6) })
    }
    private fun infoCard(label: String, value: String) = labelTextView(label, value).apply { setPadding(dp(14), dp(10), dp(14), dp(10)); background = roundedBackground(card, Color.TRANSPARENT, dp(16)) }
    private fun labelTextView(label: String, value: String) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(8))
        addView(TextView(context).apply { text = label; textSize = 12f; setTextColor(muted) })
        addView(TextView(context).apply { text = value.ifBlank { "未入力" }; textSize = 15f; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD })
    }
    private fun titleTextView(value: String) = TextView(context).apply { text = value; textSize = 23f; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD }
    private fun subtitleTextView(value: String) = TextView(context).apply { text = value; textSize = 14f; setTextColor(muted) }
    private fun makePrimaryButton(label: String) = Button(context).apply { text = label; minHeight = dp(52); setTextColor(Color.WHITE); background = roundedBackground(green, green, dp(18)); isAllCaps = false }
    private fun makeSecondaryButton(label: String) = Button(context).apply { text = label; minHeight = dp(52); setTextColor(ink); background = roundedBackground(Color.WHITE, Color.parseColor("#D1D5DB"), dp(18)); isAllCaps = false }

    private data class DurationPicker(
        val view: View,
        val selectedMinutes: () -> Int,
    )

    private data class DurationBand(
        val label: String,
        val minutes: List<Int>,
        val allowsCustom: Boolean = false,
    )

    private fun createDurationPicker(onMinutesSelected: ((Int) -> Unit)? = null): DurationPicker {
        val bands = listOf(
            DurationBand("5分未満", listOf(1, 2, 3, 4)),
            DurationBand("10分未満", listOf(5, 6, 7, 8, 9)),
            DurationBand("15分未満", listOf(10, 11, 12, 13, 14)),
            DurationBand("30分未満", listOf(15, 20, 25), allowsCustom = true),
            DurationBand("30分以上", listOf(30, 45, 60), allowsCustom = true),
        )
        var selectedBand: DurationBand? = null
        var selectedValue = 0
        var customSelected = false
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val bandGroup = RadioGroup(context).apply { orientation = RadioGroup.VERTICAL }
        val concreteContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val customInput = EditText(context).apply {
            hint = "自由入力（1〜180分）"
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            filters = arrayOf(InputFilter.LengthFilter(3))
            visibility = View.GONE
            background = roundedBackground(Color.WHITE, Color.parseColor("#D1D5DB"), dp(16))
            setPadding(dp(16), 0, dp(16), 0)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    text.toString().toIntOrNull()?.takeIf { it in 1..180 }?.let {
                        onMinutesSelected?.invoke(it)
                    }
                    true
                } else false
            }
        }

        fun renderConcreteChoices(band: DurationBand) {
            selectedValue = 0
            customSelected = false
            customInput.setText("")
            customInput.visibility = View.GONE
            concreteContainer.removeAllViews()
            concreteContainer.visibility = View.VISIBLE
            concreteContainer.addView(sectionLabel("具体的な利用時間"), matchWrapParams(top = 12, bottom = 4))
            val concreteGroup = RadioGroup(context).apply { orientation = RadioGroup.VERTICAL }
            band.minutes.forEach { minutes ->
                concreteGroup.addView(radioButton("${minutes}分").apply {
                    id = View.generateViewId()
                    setOnClickListener {
                        selectedValue = minutes
                        customSelected = false
                        customInput.visibility = View.GONE
                        onMinutesSelected?.invoke(minutes)
                    }
                })
            }
            if (band.allowsCustom) {
                concreteGroup.addView(radioButton("自由入力").apply {
                    id = View.generateViewId()
                    setOnClickListener {
                        selectedValue = 0
                        customSelected = true
                        customInput.visibility = View.VISIBLE
                        customInput.requestFocus()
                    }
                })
            }
            concreteContainer.addView(concreteGroup)
            concreteContainer.addView(customInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(8) })
        }

        root.addView(sectionLabel("おおよその利用時間帯"), matchWrapParams(bottom = 4))
        bands.forEach { band ->
            bandGroup.addView(radioButton(band.label).apply {
                id = View.generateViewId()
                setOnClickListener {
                    selectedBand = band
                    renderConcreteChoices(band)
                }
            })
        }
        root.addView(bandGroup)
        root.addView(concreteContainer)
        return DurationPicker(root) {
            if (selectedBand == null) 0
            else if (customSelected) customInput.text.toString().toIntOrNull() ?: 0
            else selectedValue
        }
    }
    private fun buildFullScreenRoot() = BackBlockingFrameLayout(context).apply {
        setBackgroundColor(Color.WHITE); isFocusable = true; isFocusableInTouchMode = true
        post { requestFocus() }
    }

    /** Keeps system Back inside a blocking overlay on both legacy and predictive-Back Android. */
    private inner class BackBlockingFrameLayout(context: Context) : FrameLayout(context) {
        private val backCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OnBackInvokedCallback { consumeBack() }
        } else null

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                findOnBackInvokedDispatcher()?.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                    requireNotNull(backCallback),
                )
            }
        }

        override fun onDetachedFromWindow() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                backCallback?.let { findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(it) }
            }
            super.onDetachedFromWindow()
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode != KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
            if (event.action == KeyEvent.ACTION_UP) consumeBack()
            return true
        }

        private fun consumeBack() {
            onBackConsumed()
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(windowToken, 0)
            requestFocus()
        }
    }
    private fun roundedBackground(fill: Int, stroke: Int, radius: Int) = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = radius.toFloat(); setColor(fill); if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke) }
    private fun matchWrapParams(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top); bottomMargin = dp(bottom) }
    private fun centeredParams(width: Int, height: Int, top: Int = 0) = LinearLayout.LayoutParams(dp(width), dp(height)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(top) }
    private fun centeredOverlayParams() = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply { marginStart = dp(20); marginEnd = dp(20); topMargin = statusBarHeight + dp(12); bottomMargin = dp(12) }
    private fun fullscreenLayoutParams() = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START }
    private fun applyAppTypeface(view: View) {
        if (view is TextView) {
            val desired = if (view.typeface?.isBold == true) zenBold else zenRegular
            if (view.typeface !== desired) view.typeface = desired
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) applyAppTypeface(view.getChildAt(index))
        }
    }
    private fun addWindow(view: View, params: WindowManager.LayoutParams): Boolean = runCatching {
        applyAppTypeface(view)
        // Multi-step overlays replace their children after the window is added.
        // Re-apply on layout so those dynamically created controls use the font too.
        view.viewTreeObserver.addOnGlobalLayoutListener { applyAppTypeface(view) }
        windowManager.addView(view, params)
    }.onFailure { error ->
        Log.e("NanishiniOverlay", "WindowManager.addView failed", error)
    }.isSuccess
    private fun removeWindow(view: View?): Boolean = if (view?.isAttachedToWindow == true) {
        runCatching { windowManager.removeViewImmediate(view) }
            .onFailure { error -> Log.e("NanishiniOverlay", "WindowManager.removeViewImmediate failed", error) }
            .isSuccess
    } else true
    private fun dismissStartOverlay() { removeWindow(startOverlay); startOverlay = null }
    private fun dismissExpirationOverlay() { removeWindow(expirationOverlay); expirationOverlay = null }
    private fun dismissRunningCheckOverlay() { removeWindow(runningCheckOverlay); runningCheckOverlay = null }
    private fun dismissExtensionOverlay() { removeWindow(extensionOverlay); extensionOverlay = null }
    private fun dismissReturnPointOverlay() { removeWindow(returnPointOverlay); returnPointOverlay = null }
    private fun minutesText(ms: Long) = "${(ms / 60_000L).coerceAtLeast(0)}分"
    private fun activityText(session: ActiveSession) = listOfNotNull(session.currentActivityCategoryName?.takeIf { it.isNotBlank() }, session.currentActivityDetail?.takeIf { it.isNotBlank() }).joinToString("：")
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
    private companion object { val fixedPurposes = setOf("特に目的はない", "その他の目的") }
}

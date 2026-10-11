package com.example.nanisini

val defaultPurposeCategories = listOf(
    PurposeCategoryModel("contact", "連絡", 1, true),
    PurposeCategoryModel("search", "調べ事", 2, true),
    PurposeCategoryModel("work", "作業", 3, true),
    PurposeCategoryModel("watch", "見る・聞く", 4, true),
    PurposeCategoryModel("rest", "娯楽・休憩", 5, true),
)

val defaultPurposeItems = listOf(
    PurposeItemModel("purpose-contact-1", "contact", "メッセージを確認する", true, 0L),
    PurposeItemModel("purpose-contact-2", "contact", "返信する", true, 0L),
    PurposeItemModel("purpose-contact-3", "contact", "電話・通話をする", true, 0L),
    PurposeItemModel("purpose-contact-4", "contact", "お知らせ・連絡事項を確認する", true, 0L),
    PurposeItemModel("purpose-search-1", "search", "知りたいことを調べる", true, 0L),
    PurposeItemModel("purpose-search-2", "search", "ニュース・最新情報を確認する", true, 0L),
    PurposeItemModel("purpose-search-3", "search", "場所・経路を調べる", true, 0L),
    PurposeItemModel("purpose-search-4", "search", "商品・サービスを探す", true, 0L),
    PurposeItemModel("purpose-search-5", "search", "予定・日時を確認する", true, 0L),
    PurposeItemModel("purpose-work-1", "work", "投稿する", true, 0L),
    PurposeItemModel("purpose-work-2", "work", "コメント・反応をする", true, 0L),
    PurposeItemModel("purpose-work-3", "work", "写真・動画・ファイルを送る", true, 0L),
    PurposeItemModel("purpose-work-4", "work", "資料・ファイルを確認する", true, 0L),
    PurposeItemModel("purpose-work-5", "work", "作業・編集を進める", true, 0L),
    PurposeItemModel("purpose-work-6", "work", "買い物・支払い・手続きをする", true, 0L),
    PurposeItemModel("purpose-watch-1", "watch", "特定の動画を見る", true, 0L),
    PurposeItemModel("purpose-watch-2", "watch", "音楽・音声を聴く", true, 0L),
    PurposeItemModel("purpose-watch-3", "watch", "保存したものを見る", true, 0L),
    PurposeItemModel("purpose-watch-4", "watch", "配信・ライブを見る", true, 0L),
    PurposeItemModel("purpose-rest-1", "rest", "ゲームをする", true, 0L),
    PurposeItemModel("purpose-rest-2", "rest", "休憩する", true, 0L),
    PurposeItemModel("purpose-rest-3", "rest", "気分転換・暇つぶしをする", true, 0L),
    PurposeItemModel("purpose-any-1", "rest", "特に目的はない", true, 0L),
    PurposeItemModel("purpose-any-2", "rest", "その他の目的", true, 0L),
)

val defaultActivityCategories = listOf(
    ActivityCategoryModel("activity-study", "勉強", 1, true),
    ActivityCategoryModel("activity-work", "仕事", 2, true),
    ActivityCategoryModel("activity-dev", "開発・制作", 3, true),
    ActivityCategoryModel("activity-home", "家事", 4, true),
    ActivityCategoryModel("activity-move", "移動・外出", 5, true),
    ActivityCategoryModel("activity-sport", "運動", 6, true),
    ActivityCategoryModel("activity-meal", "食事", 7, true),
    ActivityCategoryModel("activity-hobby", "趣味", 8, true),
    ActivityCategoryModel("activity-people", "人と過ごす", 9, true),
    ActivityCategoryModel("activity-rest", "休憩", 10, true),
    ActivityCategoryModel("activity-pre-sleep", "就寝前", 11, true),
    ActivityCategoryModel("activity-none", "特に何もしていない", 12, true),
    ActivityCategoryModel("activity-other", "その他", 13, true),
)

const val privacySummaryText = "利用状況の分析のため、アプリ名、利用目的、利用時間、アプリを開く前に入力した内容を端末内に保存します。これらの情報は端末内で処理され、外部サーバーには送信しません。画面上の文章、メッセージ本文、パスワード、Web内容、クリップボードは読み取りません。"

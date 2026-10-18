package com.example.data.model

import java.util.UUID

enum class StreamType(val title: String, val extensionNote: String) {
    M3U8("m3u8", "HLS Live Stream"),
    TS("ts", "MPEG-TS Stream"),
    NORMAL("عادي", "Direct / MP4 Link"),
    EMBED("embed", "كود تضمين / ويب");

    companion object {
        fun fromString(value: String?): StreamType {
            return when (value?.lowercase()?.trim()) {
                "m3u8" -> M3U8
                "ts" -> TS
                "embed", "iframe", "code" -> EMBED
                else -> NORMAL
            }
        }
    }
}

data class ChannelServer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "SD",
    val streamUrl: String = "",
    val streamType: String = "m3u8" // "m3u8", "ts", "normal"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "streamUrl" to streamUrl,
            "streamType" to streamType
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ChannelServer {
            return ChannelServer(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                name = map["name"] as? String ?: "SD",
                streamUrl = map["streamUrl"] as? String ?: "",
                streamType = map["streamType"] as? String ?: "m3u8"
            )
        }
    }
}

data class CustomOverlayBox(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "مربع مخصص",
    val text: String = "",
    val bgColor: String = "#000000",
    val textColor: String = "#FFFFFF",
    val textSize: Int = 13,
    val alignment: String = "top_end", // "top_end", "top_start", "top_center", "center", "bottom_end", "bottom_start", "bottom_center"
    val offsetX: Int = 0, // يمين (+) / يسار (-)
    val offsetY: Int = 0, // تحت (+) / فوق (-)
    val isEnabled: Boolean = true
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "text" to text,
            "bgColor" to bgColor,
            "textColor" to textColor,
            "textSize" to textSize,
            "alignment" to alignment,
            "offsetX" to offsetX,
            "offsetY" to offsetY,
            "isEnabled" to isEnabled
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): CustomOverlayBox {
            return CustomOverlayBox(
                id = (map["id"] as? String)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                title = map["title"] as? String ?: "مربع مخصص",
                text = map["text"] as? String ?: "",
                bgColor = map["bgColor"] as? String ?: "#000000",
                textColor = map["textColor"] as? String ?: "#FFFFFF",
                textSize = (map["textSize"] as? Number)?.toInt() ?: 13,
                alignment = map["alignment"] as? String ?: "top_end",
                offsetX = (map["offsetX"] as? Number)?.toInt() ?: 0,
                offsetY = (map["offsetY"] as? Number)?.toInt() ?: 0,
                isEnabled = (map["isEnabled"] as? Boolean) ?: true
            )
        }
    }
}

data class Channel(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val imageUrl: String = "",
    val category: String = "كرة قدم",
    val branchId: String = "",
    val branchName: String = "",
    val servers: List<ChannelServer> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val views: Long = 0L,
    // Embed code and player editor custom styling
    val isEmbedCode: Boolean = false,
    val embedCode: String = "",
    val strictAdBlock: Boolean = true,
    val topText: String = "",
    val topTextSize: Int = 16,
    val topTextColor: String = "#FFFFFF",
    val topImageUrl: String = "",
    val bottomText: String = "",
    val bottomTextSize: Int = 14,
    val bottomTextColor: String = "#CCCCCC",
    val bottomImageUrl: String = "",
    // Optional Overlay Boxes / Rectangles (مربعات ومستطيلات لتغطية الشعار/الإعلانات ونشر نصوص)
    val topRightText: String = "",
    val topRightBgColor: String = "",
    val topRightTextColor: String = "#FFFFFF",
    val topRightTextSize: Int = 13,
    val topRightEnabled: Boolean = false,
    val topRightOffsetX: Int = 0,
    val topRightOffsetY: Int = 0,
    val topLeftText: String = "",
    val topLeftBgColor: String = "",
    val topLeftTextColor: String = "#FFFFFF",
    val topLeftTextSize: Int = 13,
    val topLeftEnabled: Boolean = false,
    val topLeftOffsetX: Int = 0,
    val topLeftOffsetY: Int = 0,
    val bottomCenterText: String = "",
    val bottomCenterBgColor: String = "",
    val bottomCenterTextColor: String = "#FFFFFF",
    val bottomCenterTextSize: Int = 12,
    val bottomCenterEnabled: Boolean = false,
    val bottomCenterOffsetX: Int = 0,
    val bottomCenterOffsetY: Int = 0,
    val topCenterText: String = "",
    val topCenterBgColor: String = "",
    val topCenterTextColor: String = "#FFFFFF",
    val topCenterTextSize: Int = 13,
    val topCenterEnabled: Boolean = false,
    val topCenterOffsetX: Int = 0,
    val topCenterOffsetY: Int = 0,
    val customBoxes: List<CustomOverlayBox> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "name" to name,
            "imageUrl" to imageUrl,
            "category" to category,
            "servers" to servers.map { it.toMap() },
            "createdAt" to createdAt,
            "views" to views,
            "appVersion" to "1.0.0",
            "isEmbedCode" to isEmbedCode,
            "strictAdBlock" to strictAdBlock,
            "topTextSize" to topTextSize,
            "bottomTextSize" to bottomTextSize
        )
        if (branchId.isNotBlank()) {
            map["branchId"] = branchId
        }
        if (branchName.isNotBlank()) {
            map["branchName"] = branchName
        }
        if (embedCode.isNotBlank()) {
            map["embedCode"] = embedCode
        }
        if (topText.isNotBlank()) {
            map["topText"] = topText
        }
        if (topTextColor.isNotBlank()) {
            map["topTextColor"] = topTextColor
        }
        if (topImageUrl.isNotBlank()) {
            map["topImageUrl"] = topImageUrl
        }
        if (bottomText.isNotBlank()) {
            map["bottomText"] = bottomText
        }
        if (bottomTextColor.isNotBlank()) {
            map["bottomTextColor"] = bottomTextColor
        }
        if (bottomImageUrl.isNotBlank()) {
            map["bottomImageUrl"] = bottomImageUrl
        }
        // Overlay Boxes serialization
        map["topRightEnabled"] = topRightEnabled
        if (topRightText.isNotBlank()) map["topRightText"] = topRightText
        if (topRightBgColor.isNotBlank()) map["topRightBgColor"] = topRightBgColor
        if (topRightTextColor.isNotBlank()) map["topRightTextColor"] = topRightTextColor
        map["topRightTextSize"] = topRightTextSize

        map["topLeftEnabled"] = topLeftEnabled
        if (topLeftText.isNotBlank()) map["topLeftText"] = topLeftText
        if (topLeftBgColor.isNotBlank()) map["topLeftBgColor"] = topLeftBgColor
        if (topLeftTextColor.isNotBlank()) map["topLeftTextColor"] = topLeftTextColor
        map["topLeftTextSize"] = topLeftTextSize

        map["bottomCenterEnabled"] = bottomCenterEnabled
        if (bottomCenterText.isNotBlank()) map["bottomCenterText"] = bottomCenterText
        if (bottomCenterBgColor.isNotBlank()) map["bottomCenterBgColor"] = bottomCenterBgColor
        if (bottomCenterTextColor.isNotBlank()) map["bottomCenterTextColor"] = bottomCenterTextColor
        map["bottomCenterTextSize"] = bottomCenterTextSize

        map["topRightOffsetX"] = topRightOffsetX
        map["topRightOffsetY"] = topRightOffsetY
        map["topLeftOffsetX"] = topLeftOffsetX
        map["topLeftOffsetY"] = topLeftOffsetY
        map["bottomCenterOffsetX"] = bottomCenterOffsetX
        map["bottomCenterOffsetY"] = bottomCenterOffsetY
        map["topCenterOffsetX"] = topCenterOffsetX
        map["topCenterOffsetY"] = topCenterOffsetY

        if (customBoxes.isNotEmpty()) {
            map["customBoxes"] = customBoxes.map { it.toMap() }
        }

        map["topCenterEnabled"] = topCenterEnabled
        if (topCenterText.isNotBlank()) map["topCenterText"] = topCenterText
        if (topCenterBgColor.isNotBlank()) map["topCenterBgColor"] = topCenterBgColor
        if (topCenterTextColor.isNotBlank()) map["topCenterTextColor"] = topCenterTextColor
        map["topCenterTextSize"] = topCenterTextSize
        return map
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): Channel {
            val serversRaw = map["servers"]
            val serverList = mutableListOf<ChannelServer>()

            when (serversRaw) {
                is List<*> -> {
                    for (item in serversRaw) {
                        if (item is Map<*, *>) {
                            serverList.add(ChannelServer.fromMap(item as Map<String, Any?>))
                        }
                    }
                }
                is Map<*, *> -> {
                    for ((_, item) in serversRaw) {
                        if (item is Map<*, *>) {
                            serverList.add(ChannelServer.fromMap(item as Map<String, Any?>))
                        }
                    }
                }
            }

            val customBoxesRaw = map["customBoxes"] as? List<*>
            val customBoxesList = customBoxesRaw?.mapNotNull { item ->
                if (item is Map<*, *>) {
                    CustomOverlayBox.fromMap(item as Map<String, Any?>)
                } else null
            } ?: emptyList()

            return Channel(
                id = (map["id"] as? String)?.takeIf { it.isNotBlank() } ?: id,
                name = map["name"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String ?: "",
                category = map["category"] as? String ?: "كرة قدم",
                branchId = map["branchId"] as? String ?: "",
                branchName = map["branchName"] as? String ?: "",
                servers = serverList,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                views = (map["views"] as? Number)?.toLong() ?: 0L,
                isEmbedCode = (map["isEmbedCode"] as? Boolean) ?: false,
                embedCode = (map["embedCode"] as? String) ?: "",
                strictAdBlock = (map["strictAdBlock"] as? Boolean) ?: true,
                topText = (map["topText"] as? String) ?: "",
                topTextSize = (map["topTextSize"] as? Number)?.toInt() ?: 16,
                topTextColor = (map["topTextColor"] as? String) ?: "#FFFFFF",
                topImageUrl = (map["topImageUrl"] as? String) ?: "",
                bottomText = (map["bottomText"] as? String) ?: "",
                bottomTextSize = (map["bottomTextSize"] as? Number)?.toInt() ?: 14,
                bottomTextColor = (map["bottomTextColor"] as? String) ?: "#CCCCCC",
                bottomImageUrl = (map["bottomImageUrl"] as? String) ?: "",
                topRightText = (map["topRightText"] as? String) ?: "",
                topRightBgColor = (map["topRightBgColor"] as? String) ?: "",
                topRightTextColor = (map["topRightTextColor"] as? String) ?: "#FFFFFF",
                topRightTextSize = (map["topRightTextSize"] as? Number)?.toInt() ?: 13,
                topRightEnabled = (map["topRightEnabled"] as? Boolean) ?: false,
                topRightOffsetX = (map["topRightOffsetX"] as? Number)?.toInt() ?: 0,
                topRightOffsetY = (map["topRightOffsetY"] as? Number)?.toInt() ?: 0,
                topLeftText = (map["topLeftText"] as? String) ?: "",
                topLeftBgColor = (map["topLeftBgColor"] as? String) ?: "",
                topLeftTextColor = (map["topLeftTextColor"] as? String) ?: "#FFFFFF",
                topLeftTextSize = (map["topLeftTextSize"] as? Number)?.toInt() ?: 13,
                topLeftEnabled = (map["topLeftEnabled"] as? Boolean) ?: false,
                topLeftOffsetX = (map["topLeftOffsetX"] as? Number)?.toInt() ?: 0,
                topLeftOffsetY = (map["topLeftOffsetY"] as? Number)?.toInt() ?: 0,
                bottomCenterText = (map["bottomCenterText"] as? String) ?: "",
                bottomCenterBgColor = (map["bottomCenterBgColor"] as? String) ?: "",
                bottomCenterTextColor = (map["bottomCenterTextColor"] as? String) ?: "#FFFFFF",
                bottomCenterTextSize = (map["bottomCenterTextSize"] as? Number)?.toInt() ?: 12,
                bottomCenterEnabled = (map["bottomCenterEnabled"] as? Boolean) ?: false,
                bottomCenterOffsetX = (map["bottomCenterOffsetX"] as? Number)?.toInt() ?: 0,
                bottomCenterOffsetY = (map["bottomCenterOffsetY"] as? Number)?.toInt() ?: 0,
                topCenterText = (map["topCenterText"] as? String) ?: "",
                topCenterBgColor = (map["topCenterBgColor"] as? String) ?: "",
                topCenterTextColor = (map["topCenterTextColor"] as? String) ?: "#FFFFFF",
                topCenterTextSize = (map["topCenterTextSize"] as? Number)?.toInt() ?: 13,
                topCenterEnabled = (map["topCenterEnabled"] as? Boolean) ?: false,
                topCenterOffsetX = (map["topCenterOffsetX"] as? Number)?.toInt() ?: 0,
                topCenterOffsetY = (map["topCenterOffsetY"] as? Number)?.toInt() ?: 0,
                customBoxes = customBoxesList
            )
        }
    }
}

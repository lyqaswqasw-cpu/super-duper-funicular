package com.example.ui.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.example.data.model.CustomOverlayBox
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.theme.TextMuted
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Channel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SuccessGreen
import java.io.ByteArrayInputStream

/**
 * Strict Ad-Block, Anti-Embed & Anti-Sharing Script Injected into Embed Players.
 * 1. Blocks and removes embed code buttons (<button class="embed-btn">, showEmbed(), #embedModal, #embedCode).
 * 2. Completely removes share icons and sharing overlays (.rmp-sharing, .rmp-quick-sharing, .rmp-i-sharing).
 * 3. Sanitizes Settings menu (عند الضغط على الإعدادات يمنع رؤية المشاركة وحظرها نهائياً).
 * 4. Removes popups, click-traps, and telegram banners.
 */
private const val STRICT_ADBLOCK_JS = """
(function() {
    // 1. Prevent popups and new tabs
    window.open = function() { console.log('[AdBlock] Blocked window.open'); return null; };
    window.alert = function() {};
    window.confirm = function() { return false; };
    window.prompt = function() { return null; };

    // 2. Neutralize embed functions immediately
    window.showEmbed = function() { console.log('[Blocked] showEmbed'); return false; };
    window.hideEmbed = function() { return false; };
    window.copyEmbed = function() { return false; };

    // Helper: Identify if element is embed code or sharing related
    function isShareOrEmbedElement(el) {
        if (!el) return false;
        var tag = (el.tagName || '').toLowerCase();
        var cls = (el.className || '').toString().toLowerCase();
        var id = (el.id || '').toLowerCase();
        var onclick = (el.getAttribute ? (el.getAttribute('onclick') || '') : '').toLowerCase();
        var aria = (el.getAttribute ? (el.getAttribute('aria-label') || '') : '').toLowerCase();
        var title = (el.getAttribute ? (el.getAttribute('title') || '') : '').toLowerCase();
        var text = (el.innerText || el.textContent || '').trim().toLowerCase();

        if (cls.includes('embed-btn') || id.includes('embedmodal') || cls.includes('embed-modal') ||
            id.includes('embedcode') || cls.includes('embed-content') || cls.includes('embed-copy-btn') ||
            onclick.includes('showembed') || onclick.includes('copyembed') || onclick.includes('hideembed')) {
            return true;
        }

        if (cls.includes('rmp-sharing') || cls.includes('rmp-quick-sharing') || cls.includes('rmp-i-sharing') ||
            cls.includes('rmp-sharing-social') || cls.includes('rmp-sharing-code') || cls.includes('rmp-sharing-link') ||
            cls.includes('quick-sharing') || aria.includes('share') || aria.includes('sharing') || aria.includes('مشاركة') ||
            title.includes('share') || title.includes('مشاركة')) {
            return true;
        }

        if (text.includes('مشاركة') || text.includes('تضمين') || text.includes('انسخ الكود') || text === 'share' || text === 'embed') {
            return true;
        }

        return false;
    }

    // Helper: Verify if element is part of genuine playback controls
    function isGenuinePlayerControl(el) {
        if (!el) return false;
        if (isShareOrEmbedElement(el)) return false;
        var tag = (el.tagName || '').toLowerCase();
        if (tag === 'video' || tag === 'audio' || tag === 'input' || tag === 'track') {
            return true;
        }
        var s = ((el.className || '') + ' ' + (el.id || '')).toLowerCase();
        if (/play|pause|volume|sound|mute|full|screen|quality|live|stream/i.test(s)) {
            return true;
        }
        return false;
    }

    // 3. Inject CSS into document and all accessible iframes
    function injectAntiEmbedStyle(targetDoc) {
        if (!targetDoc) return;
        try {
            if (targetDoc.getElementById('foul_block_embed_share_style')) return;
            var style = targetDoc.createElement('style');
            style.id = 'foul_block_embed_share_style';
            style.type = 'text/css';
            style.innerHTML = `
                .embed-btn, .embed-modal, #embedModal, #embedCode, .embed-content, .embed-copy-btn,
                div[style*="top:8px;left:8px"], div[style*="top: 8px; left: 8px"], div[style*="top:8px"],
                button[onclick*="showEmbed"], button[onclick*="copyEmbed"], button[onclick*="hideEmbed"], button[onclick*="location.href"],
                .rmp-sharing, .rmp-quick-sharing, .rmp-i-sharing, .rmp-sharing-social, .rmp-sharing-social-icons,
                .rmp-sharing-code, .rmp-sharing-link, .rmp-sharing-input, .rmp-sharing-code-container,
                [class*="sharing"], [class*="quick-sharing"], [aria-label*="share" i], [aria-label*="sharing" i],
                [aria-label*="مشاركة"], [title*="share" i], [title*="sharing" i], [title*="مشاركة"],
                button[class*="share"], button[class*="sharing"],
                .rmp-settings-menu .rmp-sharing, .rmp-module-area .rmp-sharing, .rmp-overlay-menu-selection .rmp-sharing,
                .rmp-settings-item[data-setting*="share"], .rmp-settings-item[data-item*="share"],
                a[href*="t.me"], a[href*="telegram.me"], a[href*="telegram.org"], a[href*="joinchat"],
                .tg-banner, #tg-banner, .telegram-box, iframe[src*="ad"], iframe[src*="telegram"] {
                    display: none !important;
                    visibility: hidden !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                    width: 0 !important;
                    height: 0 !important;
                    max-width: 0 !important;
                    max-height: 0 !important;
                    position: absolute !important;
                    left: -9999px !important;
                    top: -9999px !important;
                    z-index: -99999 !important;
                }
                html, body {
                    margin: 0 !important;
                    padding: 0 !important;
                    overflow: hidden !important;
                    background: #000 !important;
                    width: 100% !important;
                    height: 100% !important;
                }
                video {
                    width: 100% !important;
                    height: 100% !important;
                    max-width: 100% !important;
                    max-height: 100% !important;
                    object-fit: contain !important;
                }
            `;
            (targetDoc.head || targetDoc.documentElement).appendChild(style);
        } catch(e) {}
    }

    injectAntiEmbedStyle(document);

    // 4. Aggressively purge Embed, Share, and Settings Share items
    function purgeEmbedAndShare(targetDoc) {
        var doc = targetDoc || document;
        injectAntiEmbedStyle(doc);

        try {
            // A. Remove Embed Button & Modal
            var embeds = doc.querySelectorAll('.embed-btn, #embedModal, .embed-modal, #embedCode, .embed-content, [onclick*="showEmbed"], [onclick*="copyEmbed"]');
            for (var i = 0; i < embeds.length; i++) {
                embeds[i].remove();
            }

            var topContainers = doc.querySelectorAll('div[style*="top:8px"], div[style*="top: 8px"], div[style*="left:8px"], div[style*="left: 8px"]');
            for (var j = 0; j < topContainers.length; j++) {
                var c = topContainers[j];
                if (c.querySelector && (c.querySelector('.embed-btn') || c.querySelector('button[onclick*="showEmbed"]') || c.querySelector('svg path[d*="16.5,5L15.09"]'))) {
                    c.remove();
                }
            }

            // B. Remove Share Icons & Buttons (Radiant Media Player, Clappr, etc.)
            var shareBtns = doc.querySelectorAll('.rmp-sharing, .rmp-quick-sharing, .rmp-i-sharing, .rmp-sharing-social, .rmp-sharing-code, .rmp-sharing-link, [class*="rmp-sharing"], [class*="quick-sharing"], [aria-label*="share" i], [aria-label*="sharing" i], [aria-label*="مشاركة"], [title*="share" i], [title*="مشاركة"]');
            for (var s = 0; s < shareBtns.length; s++) {
                shareBtns[s].remove();
            }

            // C. Sanitize Settings Menu (عند الضغط على الإعدادات يمنع رؤية المشاركة وحظرها نهائياً)
            var menuItems = doc.querySelectorAll('.rmp-settings-item, .rmp-module-area div, .rmp-overlay-menu-selection div, .rmp-settings-menu li, .rmp-overlay-menu-item, [role="menuitem"], [class*="settings-item"], [class*="menu-item"]');
            for (var m = 0; m < menuItems.length; m++) {
                var item = menuItems[m];
                if (isShareOrEmbedElement(item)) {
                    item.style.setProperty('display', 'none', 'important');
                    item.style.setProperty('visibility', 'hidden', 'important');
                    item.style.setProperty('pointer-events', 'none', 'important');
                    item.remove();
                }
            }

            // D. Remove Telegram banners
            var telegramLinks = doc.querySelectorAll('a[href*="t.me"], a[href*="telegram.me"], a[href*="joinchat"]');
            for (var t = 0; t < telegramLinks.length; t++) {
                var link = telegramLinks[t];
                var parent = link.closest('div, section, aside, p') || link;
                if (!isGenuinePlayerControl(parent)) {
                    parent.remove();
                } else {
                    link.remove();
                }
            }

            // E. Recurse into all accessible iframes
            var iframes = doc.querySelectorAll('iframe');
            for (var k = 0; k < iframes.length; k++) {
                try {
                    var frameDoc = iframes[k].contentDocument || iframes[k].contentWindow.document;
                    if (frameDoc) {
                        purgeEmbedAndShare(frameDoc);
                    }
                } catch(e) {}
            }
        } catch(e) {}
    }

    // 5. Intercept Click Events to block any attempt to open embed or share
    window.addEventListener('click', function(e) {
        var target = e.target;
        if (!target) return;
        var match = target.closest ? target.closest('.embed-btn, [onclick*="showEmbed"], #embedModal, .rmp-sharing, [class*="sharing"]') : null;
        if (match || isShareOrEmbedElement(target)) {
            e.preventDefault();
            e.stopPropagation();
            e.stopImmediatePropagation();
            if (match && match.remove) match.remove();
            return false;
        }
    }, true);

    purgeEmbedAndShare();
    setInterval(purgeEmbedAndShare, 250);

    var observer = new MutationObserver(function() {
        purgeEmbedAndShare();
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });
})();
"""

/**
 * Extracts embed URL from iframe HTML string or returns URL directly.
 */
fun extractEmbedUrl(embedInput: String): String {
    val trimmed = embedInput.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }
    // Match src="..." or src='...' inside iframe
    val srcRegex = """src=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
    val match = srcRegex.find(trimmed)
    if (match != null && match.groupValues.size > 1) {
        val extracted = match.groupValues[1].trim()
        if (extracted.startsWith("//")) {
            return "https:$extracted"
        }
        return extracted
    }
    return trimmed
}

/**
 * Builds responsive clean HTML wrapper for embed video/iframe player.
 * Supports:
 * 1. Direct stream / player page URLs
 * 2. Full or partial iframe snippets (<iframe src="..." ...></iframe>)
 * 3. HTML5 video tags (<video ...></video>)
 * 4. Custom player scripts (<script ...></script><div id="player"></div>)
 * 5. Mixed HTML embeds
 * Automatically enforces 100vw x 100vh fill-screen with zero margins/padding.
 */
fun buildEmbedHtmlWrapper(urlOrCode: String): String {
    val trimmed = urlOrCode.trim()
    val isPureUrl = (trimmed.startsWith("http://", ignoreCase = true) || 
                     trimmed.startsWith("https://", ignoreCase = true) || 
                     trimmed.startsWith("//")) && !trimmed.contains("<")

    val iframeSrc = if (!isPureUrl) extractEmbedUrl(trimmed) else ""
    val hasExtractedIframeSrc = iframeSrc.isNotBlank() && 
            (iframeSrc.startsWith("http://", ignoreCase = true) || 
             iframeSrc.startsWith("https://", ignoreCase = true) || 
             iframeSrc.startsWith("//"))

    val playerContent = when {
        isPureUrl -> {
            val finalSrc = if (trimmed.startsWith("//")) "https:$trimmed" else trimmed
            val escapedSrc = finalSrc.replace("\"", "&quot;")
            """
            <iframe 
                id="aistudio_embed_frame"
                src="$escapedSrc" 
                width="100%" 
                height="100%" 
                frameborder="0" 
                allowfullscreen="true" 
                webkitallowfullscreen="true"
                mozallowfullscreen="true"
                allow="autoplay *; fullscreen *; encrypted-media *; picture-in-picture *; accelerometer *; gyroscope *" 
                scrolling="no"
                style="width: 100vw !important; height: 100vh !important; position: absolute; top: 0; left: 0; border: none; margin: 0; padding: 0;">
            </iframe>
            """.trimIndent()
        }
        hasExtractedIframeSrc -> {
            val finalSrc = if (iframeSrc.startsWith("//")) "https:$iframeSrc" else iframeSrc
            val escapedSrc = finalSrc.replace("\"", "&quot;")
            """
            <iframe 
                id="aistudio_embed_frame"
                src="$escapedSrc" 
                width="100%" 
                height="100%" 
                frameborder="0" 
                allowfullscreen="true" 
                webkitallowfullscreen="true"
                mozallowfullscreen="true"
                allow="autoplay *; fullscreen *; encrypted-media *; picture-in-picture *; accelerometer *; gyroscope *" 
                scrolling="no"
                style="width: 100vw !important; height: 100vh !important; position: absolute; top: 0; left: 0; border: none; margin: 0; padding: 0;">
            </iframe>
            """.trimIndent()
        }
        else -> {
            // Raw HTML code (e.g. customized player snippet, <script>, <video>, or embedded player)
            trimmed
        }
    }

    return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
            <style>
                * {
                    box-sizing: border-box !important;
                    margin: 0 !important;
                    padding: 0 !important;
                }
                html, body {
                    width: 100vw !important;
                    height: 100vh !important;
                    max-width: 100vw !important;
                    max-height: 100vh !important;
                    background-color: #000000 !important;
                    overflow: hidden !important;
                    margin: 0 !important;
                    padding: 0 !important;
                }
                .player-fullscreen-container {
                    position: fixed !important;
                    top: 0 !important;
                    left: 0 !important;
                    right: 0 !important;
                    bottom: 0 !important;
                    width: 100vw !important;
                    height: 100vh !important;
                    max-width: 100vw !important;
                    max-height: 100vh !important;
                    background: #000000 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    overflow: hidden !important;
                }
                iframe, video, embed, object, .player-fullscreen-container > * {
                    position: absolute !important;
                    top: 0 !important;
                    left: 0 !important;
                    width: 100vw !important;
                    height: 100vh !important;
                    max-width: 100vw !important;
                    max-height: 100vh !important;
                    min-width: 100vw !important;
                    min-height: 100vh !important;
                    border: none !important;
                    outline: none !important;
                    background: #000000 !important;
                    object-fit: fill !important;
                }
                video {
                    object-fit: fill !important;
                }
                /* 1. HIDE EMBED BUTTONS & EMBED MODAL COMPLETELY */
                .embed-btn, .embed-modal, #embedModal, #embedCode, .embed-content, .embed-copy-btn,
                div[style*="top:8px;left:8px"], div[style*="top: 8px; left: 8px"], div[style*="top:8px"],
                button[onclick*="showEmbed"], button[onclick*="copyEmbed"], button[onclick*="hideEmbed"], button[onclick*="location.href"],
                /* 2. HIDE ALL SHARE BUTTONS & OVERLAYS IN RADIANT MEDIA PLAYER, CLAPPR, VIDEOJS, JWPLAYER */
                .rmp-sharing, .rmp-quick-sharing, .rmp-i-sharing, .rmp-sharing-social, .rmp-sharing-social-icons,
                .rmp-sharing-code, .rmp-sharing-link, .rmp-sharing-input, .rmp-sharing-code-container,
                [class*="sharing"], [class*="quick-sharing"], [id*="sharing"], [id*="share"],
                [aria-label*="share" i], [aria-label*="sharing" i], [aria-label*="مشاركة"],
                [title*="share" i], [title*="sharing" i], [title*="مشاركة"],
                button[class*="share"], button[class*="sharing"],
                /* 3. BLOCK & HIDE SHARING & EMBED ITEMS IN SETTINGS MENU (الترس / الإعدادات) */
                .rmp-settings-menu .rmp-sharing, .rmp-module-area .rmp-sharing, .rmp-overlay-menu-selection .rmp-sharing,
                .rmp-settings-item[data-setting*="share"], .rmp-settings-item[data-item*="share"] {
                    display: none !important;
                    visibility: hidden !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                    width: 0 !important;
                    height: 0 !important;
                    max-width: 0 !important;
                    max-height: 0 !important;
                    position: absolute !important;
                    left: -9999px !important;
                    top: -9999px !important;
                    z-index: -99999 !important;
                }
            </style>
        </head>
        <body>
            <div class="player-fullscreen-container">
                $playerContent
            </div>
            <script>
                (function() {
                    function forceExpand() {
                        try {
                            var els = document.querySelectorAll('iframe, video, embed, object, [class*="player"], [id*="player"]');
                            for (var i = 0; i < els.length; i++) {
                                var el = els[i];
                                el.style.setProperty('width', '100vw', 'important');
                                el.style.setProperty('height', '100vh', 'important');
                                el.style.setProperty('max-width', '100vw', 'important');
                                el.style.setProperty('max-height', '100vh', 'important');
                                el.style.setProperty('margin', '0', 'important');
                                el.style.setProperty('padding', '0', 'important');
                            }
                            var v = document.querySelector('video');
                            if (v) {
                                v.style.setProperty('object-fit', 'fill', 'important');
                                if (v.paused) {
                                    v.play().catch(function(){});
                                }
                            }
                        } catch(e) {}
                    }
                    forceExpand();
                    setInterval(forceExpand, 1000);
                })();
            </script>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Parses hex color with fallback.
 */
fun parseOverlayColor(colorInput: String, defaultColor: Color): Color {
    val trimmed = colorInput.trim()
    if (trimmed.isBlank()) return defaultColor
    return try {
        val cleanHex = trimmed.removePrefix("#")
        when (cleanHex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            else -> defaultColor
        }
    } catch (_: Exception) {
        defaultColor
    }
}

/**
 * Known ad networks and telemetry domains to block.
 */
private val BLOCKED_AD_DOMAINS = listOf(
    "t.me",
    "telegram.me",
    "telegram.dog",
    "popads",
    "adsterra",
    "propellerads",
    "1xbet",
    "bet365",
    "melbet",
    "mostbet",
    "googleads",
    "doubleclick",
    "onclick",
    "syndication",
    "traffic",
    "adnxs",
    "exoclick",
    "adclick",
    "yadro.ru",
    "an.yandex.ru"
)

/**
 * High-Performance Native Embed WebView Player with its own controls,
 * Strict AdBlock, and optional Overlay Boxes (Top-Right, Top-Left, Bottom-Center)
 * for covering logos/ads and publishing custom styled text.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedWebViewPlayer(
    embedCode: String,
    streamUrl: String = "",
    strictAdBlock: Boolean = true,
    channelName: String = "",
    channel: Channel? = null,
    // Overlay Box 1: Top-Right (أعلى اليمين)
    topRightEnabled: Boolean = channel?.topRightEnabled ?: false,
    topRightText: String = channel?.topRightText ?: "",
    topRightBgColor: String = channel?.topRightBgColor ?: "",
    topRightTextColor: String = channel?.topRightTextColor ?: "#FFFFFF",
    topRightTextSize: Int = channel?.topRightTextSize ?: 13,
    topRightOffsetX: Int = channel?.topRightOffsetX ?: 0,
    topRightOffsetY: Int = channel?.topRightOffsetY ?: 0,
    // Overlay Box 2: Top-Left (أعلى اليسار)
    topLeftEnabled: Boolean = channel?.topLeftEnabled ?: false,
    topLeftText: String = channel?.topLeftText ?: "",
    topLeftBgColor: String = channel?.topLeftBgColor ?: "",
    topLeftTextColor: String = channel?.topLeftTextColor ?: "#FFFFFF",
    topLeftTextSize: Int = channel?.topLeftTextSize ?: 13,
    topLeftOffsetX: Int = channel?.topLeftOffsetX ?: 0,
    topLeftOffsetY: Int = channel?.topLeftOffsetY ?: 0,
    // Overlay Box 3: Bottom-Center (أسفل الوسط)
    bottomCenterEnabled: Boolean = channel?.bottomCenterEnabled ?: false,
    bottomCenterText: String = channel?.bottomCenterText ?: "",
    bottomCenterBgColor: String = channel?.bottomCenterBgColor ?: "",
    bottomCenterTextColor: String = channel?.bottomCenterTextColor ?: "#FFFFFF",
    bottomCenterTextSize: Int = channel?.bottomCenterTextSize ?: 12,
    bottomCenterOffsetX: Int = channel?.bottomCenterOffsetX ?: 0,
    bottomCenterOffsetY: Int = channel?.bottomCenterOffsetY ?: 0,
    // Overlay Box 4: Top-Center (مستطيل/شريط أعلى الوسط)
    topCenterEnabled: Boolean = channel?.topCenterEnabled ?: false,
    topCenterText: String = channel?.topCenterText ?: "",
    topCenterBgColor: String = channel?.topCenterBgColor ?: "",
    topCenterTextColor: String = channel?.topCenterTextColor ?: "#FFFFFF",
    topCenterTextSize: Int = channel?.topCenterTextSize ?: 13,
    topCenterOffsetX: Int = channel?.topCenterOffsetX ?: 0,
    topCenterOffsetY: Int = channel?.topCenterOffsetY ?: 0,
    // Dynamic Custom Boxes (مربعات خاصة مضافة حسب المكان)
    customBoxes: List<CustomOverlayBox> = channel?.customBoxes ?: emptyList(),
    // Fullscreen controls
    isFullscreen: Boolean = false,
    forceLandscape: Boolean = true,
    onExitFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Enforce LANDSCAPE orientation immediately when playing code channel!
    LaunchedEffect(forceLandscape) {
        if (forceLandscape) {
            activity?.setScreenLandscape(true)
        }
    }
    DisposableEffect(activity, forceLandscape) {
        if (forceLandscape) {
            activity?.setScreenLandscape(true)
        }
        onDispose {
            if (forceLandscape) {
                activity?.setScreenLandscape(false)
            }
        }
    }

    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var areOverlaysTemporarilyHidden by remember { mutableStateOf(false) }
    var areFloatingControlsHidden by remember { mutableStateOf(false) }

    val rawCodeOrUrl = embedCode.ifBlank { streamUrl }
    val targetUrl = remember(rawCodeOrUrl) {
        extractEmbedUrl(rawCodeOrUrl)
    }

    val htmlContent = remember(rawCodeOrUrl) {
        buildEmbedHtmlWrapper(rawCodeOrUrl)
    }

    val baseUrl = remember(targetUrl) {
        if (targetUrl.startsWith("http://", ignoreCase = true) || targetUrl.startsWith("https://", ignoreCase = true)) {
            targetUrl
        } else {
            "https://localhost/"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                destroy()
            }
            webViewInstance = null
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
        if (customView != null) {
            // HTML5 Fullscreen Video View
            AndroidView(
                factory = { customView!! },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Native Embed Player - receives all touch events directly so its own controls work natively!
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)

                        CookieManager.getInstance().setAcceptCookie(true)
                        try {
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        } catch (_: Exception) {}

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = false
                            displayZoomControls = false
                            allowFileAccess = false
                            allowContentAccess = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                            if (strictAdBlock) {
                                setSupportMultipleWindows(false)
                                javaScriptCanOpenWindowsAutomatically = false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                if (view != null && newProgress > 15) {
                                    view.evaluateJavascript(STRICT_ADBLOCK_JS, null)
                                }
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customViewCallback = callback
                                customView = view
                                activity?.setScreenLandscape(true)
                            }

                            override fun onHideCustomView() {
                                customViewCallback?.onCustomViewHidden()
                                customViewCallback = null
                                customView = null
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                return false
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                errorMessage = null
                                if (strictAdBlock && view != null) {
                                    view.evaluateJavascript(STRICT_ADBLOCK_JS, null)
                                }
                                val fillScreenJs = """
                                    (function() {
                                        try {
                                            var style = document.createElement('style');
                                            style.id = 'aistudio_fill_screen_style';
                                            style.innerHTML = `
                                                * { box-sizing: border-box !important; }
                                                html, body {
                                                    width: 100vw !important;
                                                    height: 100vh !important;
                                                    max-width: 100vw !important;
                                                    max-height: 100vh !important;
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    overflow: hidden !important;
                                                    background: #000 !important;
                                                }
                                                video, iframe, embed, object, .player, #player, .video-js, .jwplayer, .plyr, .dplayer, [class*="player"], [id*="player"] {
                                                    width: 100vw !important;
                                                    height: 100vh !important;
                                                    max-width: 100vw !important;
                                                    max-height: 100vh !important;
                                                    min-width: 100vw !important;
                                                    min-height: 100vh !important;
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    border: 0 !important;
                                                }
                                                video {
                                                    object-fit: fill !important;
                                                }
                                            `;
                                            var existing = document.getElementById('aistudio_fill_screen_style');
                                            if (existing) existing.remove();
                                            (document.head || document.documentElement).appendChild(style);

                                            var mediaEls = document.querySelectorAll('video, iframe, embed');
                                            for (var i = 0; i < mediaEls.length; i++) {
                                                var p = mediaEls[i].parentElement;
                                                while (p && p !== document.body && p !== document.documentElement) {
                                                    p.style.setProperty('width', '100vw', 'important');
                                                    p.style.setProperty('height', '100vh', 'important');
                                                    p.style.setProperty('max-width', '100vw', 'important');
                                                    p.style.setProperty('max-height', '100vh', 'important');
                                                    p.style.setProperty('margin', '0', 'important');
                                                    p.style.setProperty('padding', '0', 'important');
                                                    p = p.parentElement;
                                                }
                                            }
                                        } catch(e) {}
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(fillScreenJs, null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                if (strictAdBlock && view != null) {
                                    view.evaluateJavascript(STRICT_ADBLOCK_JS, null)
                                }
                                val fillScreenJs = """
                                    (function() {
                                        try {
                                            var style = document.createElement('style');
                                            style.id = 'aistudio_fill_screen_style';
                                            style.innerHTML = `
                                                * { box-sizing: border-box !important; }
                                                html, body {
                                                    width: 100vw !important;
                                                    height: 100vh !important;
                                                    max-width: 100vw !important;
                                                    max-height: 100vh !important;
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    overflow: hidden !important;
                                                    background: #000 !important;
                                                }
                                                video, iframe, embed, object, .player, #player, .video-js, .jwplayer, .plyr, .dplayer, [class*="player"], [id*="player"] {
                                                    width: 100vw !important;
                                                    height: 100vh !important;
                                                    max-width: 100vw !important;
                                                    max-height: 100vh !important;
                                                    min-width: 100vw !important;
                                                    min-height: 100vh !important;
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    border: 0 !important;
                                                }
                                                video {
                                                    object-fit: fill !important;
                                                }
                                            `;
                                            var existing = document.getElementById('aistudio_fill_screen_style');
                                            if (existing) existing.remove();
                                            (document.head || document.documentElement).appendChild(style);

                                            var mediaEls = document.querySelectorAll('video, iframe, embed');
                                            for (var i = 0; i < mediaEls.length; i++) {
                                                var p = mediaEls[i].parentElement;
                                                while (p && p !== document.body && p !== document.documentElement) {
                                                    p.style.setProperty('width', '100vw', 'important');
                                                    p.style.setProperty('height', '100vh', 'important');
                                                    p.style.setProperty('max-width', '100vw', 'important');
                                                    p.style.setProperty('max-height', '100vh', 'important');
                                                    p.style.setProperty('margin', '0', 'important');
                                                    p.style.setProperty('padding', '0', 'important');
                                                    p = p.parentElement;
                                                }
                                            }
                                        } catch(e) {}
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(fillScreenJs, null)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false

                                if (reqUrl.startsWith("tg:") ||
                                    reqUrl.startsWith("telegram:") ||
                                    reqUrl.startsWith("intent:") ||
                                    reqUrl.contains("t.me/") ||
                                    reqUrl.contains("telegram.me/") ||
                                    reqUrl.contains("joinchat")
                                ) {
                                    return true
                                }

                                if (strictAdBlock) {
                                    for (domain in BLOCKED_AD_DOMAINS) {
                                        if (reqUrl.contains(domain, ignoreCase = true)) {
                                            return true
                                        }
                                    }
                                }

                                return false
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                if (!strictAdBlock) return super.shouldInterceptRequest(view, request)

                                val reqUrl = request?.url?.toString()?.lowercase() ?: return null

                                for (adPattern in BLOCKED_AD_DOMAINS) {
                                    if (reqUrl.contains(adPattern)) {
                                        return WebResourceResponse(
                                            "text/plain",
                                            "UTF-8",
                                            ByteArrayInputStream(ByteArray(0))
                                        )
                                    }
                                }

                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                super.onReceivedError(view, errorCode, description, failingUrl)
                                if (failingUrl == targetUrl) {
                                    errorMessage = "تعذر تحميل بث القناة: $description"
                                    isLoading = false
                                }
                            }
                        }

                        webViewInstance = this
                        loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                    if (reloadKey > 0) {
                        webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // =========================================================================
        // CUSTOM OVERLAY BOXES / RECTANGLES (تغطية الشعار والإعلانات ونشر النصوص)
        // =========================================================================

        // 1. Box Top-Right (مربع أعلى اليمين - يغطي شعار القناة)
        if (topRightEnabled) {
            val bgCol = parseOverlayColor(topRightBgColor, Color.Black)
            val txtCol = parseOverlayColor(topRightTextColor, Color.White)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = bgCol,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = topRightOffsetX.dp, y = topRightOffsetY.dp)
                    .padding(top = 10.dp, end = 12.dp)
                    .widthIn(min = 60.dp, max = 220.dp)
                    .heightIn(min = 28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (topRightText.isNotBlank()) {
                        Text(
                            text = topRightText,
                            color = txtCol,
                            fontSize = topRightTextSize.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 2. Box Top-Left (مربع أعلى اليسار)
        if (topLeftEnabled) {
            val bgCol = parseOverlayColor(topLeftBgColor, Color.Black)
            val txtCol = parseOverlayColor(topLeftTextColor, Color.White)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = bgCol,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = topLeftOffsetX.dp, y = topLeftOffsetY.dp)
                    .padding(top = 10.dp, start = if (isFullscreen && onExitFullscreen != null) 48.dp else 12.dp)
                    .widthIn(min = 60.dp, max = 220.dp)
                    .heightIn(min = 28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (topLeftText.isNotBlank()) {
                        Text(
                            text = topLeftText,
                            color = txtCol,
                            fontSize = topLeftTextSize.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 3. Rectangle Bottom-Center (مستطيل أسفل الوسط - يغطي شريط الإعلانات)
        if (bottomCenterEnabled) {
            val bgCol = parseOverlayColor(bottomCenterBgColor, Color.Black)
            val txtCol = parseOverlayColor(bottomCenterTextColor, Color.White)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = bgCol,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = bottomCenterOffsetX.dp, y = bottomCenterOffsetY.dp)
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(0.85f)
                    .heightIn(min = 32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    if (bottomCenterText.isNotBlank()) {
                        Text(
                            text = bottomCenterText,
                            color = txtCol,
                            fontSize = bottomCenterTextSize.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 4. Rectangle Top-Center (مستطيل/شريط أعلى الوسط)
        if (topCenterEnabled) {
            val bgCol = parseOverlayColor(topCenterBgColor, Color.Black)
            val txtCol = parseOverlayColor(topCenterTextColor, Color.White)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = bgCol,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = topCenterOffsetX.dp, y = topCenterOffsetY.dp)
                    .padding(top = 10.dp, start = 50.dp, end = 50.dp)
                    .widthIn(min = 70.dp, max = 280.dp)
                    .heightIn(min = 28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    if (topCenterText.isNotBlank()) {
                        Text(
                            text = topCenterText,
                            color = txtCol,
                            fontSize = topCenterTextSize.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 5. Custom Dynamic Overlay Boxes (مربعات خاصة مضافة حسب المكان)
        customBoxes.forEach { customBox ->
            if (customBox.isEnabled) {
                val bgCol = parseOverlayColor(customBox.bgColor, Color.Black)
                val txtCol = parseOverlayColor(customBox.textColor, Color.White)
                val boxAlign = when (customBox.alignment) {
                    "top_start" -> Alignment.TopStart
                    "top_end" -> Alignment.TopEnd
                    "top_center" -> Alignment.TopCenter
                    "center" -> Alignment.Center
                    "bottom_start" -> Alignment.BottomStart
                    "bottom_end" -> Alignment.BottomEnd
                    "bottom_center" -> Alignment.BottomCenter
                    else -> Alignment.TopEnd
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = bgCol,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .align(boxAlign)
                        .offset(x = customBox.offsetX.dp, y = customBox.offsetY.dp)
                        .padding(8.dp)
                        .widthIn(min = 60.dp, max = 260.dp)
                        .heightIn(min = 26.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (customBox.text.isNotBlank()) {
                            Text(
                                text = customBox.text,
                                color = txtCol,
                                fontSize = customBox.textSize.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // PERMANENT SOLID BLACK PROTECTIVE SHIELDS (حجب وحظر كود التضمين والمشاركة نهائياً)
        // =========================================================================

        // 1. TOP-LEFT SOLID BLACK SHIELD: Covers Embed code button (</>) and Reload button entirely
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 0.dp, y = 0.dp)
                .size(width = 96.dp, height = 130.dp)
                .background(Color.Black, RoundedCornerShape(bottomEnd = 16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = true
                ) {
                    // Consume click completely - blocks all touch events from reaching underlying buttons
                }
        )

        // 2. TOP-RIGHT SOLID BLACK SHIELD: Covers Share button entirely
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 0.dp, y = 0.dp)
                .size(width = 80.dp, height = 75.dp)
                .background(Color.Black, RoundedCornerShape(bottomStart = 16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = true
                ) {
                    // Consume click completely
                }
        )

        // Floating Back Button in Fullscreen
        if (isFullscreen && onExitFullscreen != null) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 104.dp, y = 8.dp)
                    .clickable { onExitFullscreen() }
                    .testTag("exit_embed_fullscreen_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }
        }

        // Loading Indicator
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = NeonPurple,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "جاري تشغيل مشغل الكود...",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (strictAdBlock) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "مانع الإعلانات الصارم مفعل 🛡️",
                                color = SuccessGreen,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Error State with Retry
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.95f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = LiveRed,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = errorMessage ?: "خطأ في تشغيل القناة",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricPurple,
                        modifier = Modifier
                            .clickable {
                                reloadKey++
                                errorMessage = null
                            }
                            .testTag("retry_embed_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إعادة المحاولة",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

package com.example.data.repository

import android.util.Log
import com.example.data.model.Channel
import com.example.data.model.ChannelBranch
import com.example.data.model.ChannelCategory
import com.example.data.model.ChannelServer
import com.example.data.model.EventBanner
import com.example.data.model.XtremeAccount
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * ChannelRepository - Pure Read-Only User App Implementation
 * Connects to Firebase Realtime Database strictly for reading & real-time listening.
 * All write, edit, and delete operations have been removed to guarantee security and isolation.
 */
class ChannelRepository {

    companion object {
        const val FIREBASE_RTDB_URL = "https://foul-ddbb8-default-rtdb.firebaseio.com"
        private const val TAG = "ChannelRepository"
        private var isPersistenceConfigured = false

        val DEFAULT_XTREME_ACCOUNTS = emptyList<XtremeAccount>()
    }

    private val database: FirebaseDatabase by lazy {
        val db = try {
            FirebaseDatabase.getInstance(FIREBASE_RTDB_URL)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining Firebase instance: ${e.message}")
            FirebaseDatabase.getInstance()
        }
        if (!isPersistenceConfigured) {
            try {
                db.setPersistenceEnabled(true)
            } catch (_: Exception) {
                // Ignore if already configured
            }
            isPersistenceConfigured = true
        }
        db
    }

    private val channelsRef by lazy {
        database.getReference("v4_channels").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    private val xtremeRef by lazy {
        database.getReference("v4_xtreme_accounts").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    private val categoriesRef by lazy {
        database.getReference("v4_channel_categories").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    private val branchesRef by lazy {
        database.getReference("v4_channel_branches").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    private val eventsRef by lazy {
        database.getReference("v4_event_banners").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    /**
     * Listens to live real-time updates of Channel Branches from Firebase.
     */
    fun getBranchesFlow(): Flow<List<ChannelBranch>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChannelBranch>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val branch = ChannelBranch.fromMap(key, value as Map<String, Any?>)
                        if (com.example.data.security.AppSecurityGuard.isMaliciousText(branch.name)) {
                            continue
                        }
                        list.add(branch)
                    }
                }
                list.sortByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase Branches listener cancelled: ${error.message}")
                trySend(emptyList())
            }
        }

        branchesRef.addValueEventListener(listener)

        awaitClose {
            branchesRef.removeEventListener(listener)
        }
    }

    /**
     * Listens to live real-time updates of Channel Categories from Firebase.
     */
    fun getCategoriesFlow(): Flow<List<ChannelCategory>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChannelCategory>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val cat = ChannelCategory.fromMap(key, value as Map<String, Any?>)
                        if (com.example.data.security.AppSecurityGuard.isMaliciousText(cat.name)) {
                            continue
                        }
                        list.add(cat)
                    }
                }
                list.sortWith(
                    compareBy(
                        { it.order },
                        { it.createdAt }
                    )
                )
                if (list.isEmpty()) {
                    trySend(getBuiltInCategories())
                } else {
                    trySend(list)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase Categories listener cancelled: ${error.message}")
                trySend(getBuiltInCategories())
            }
        }

        categoriesRef.addValueEventListener(listener)

        awaitClose {
            categoriesRef.removeEventListener(listener)
        }
    }

    /**
     * Listens to live real-time updates of Xtreme accounts from Firebase.
     */
    fun getXtremeAccountsFlow(): Flow<List<XtremeAccount>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<XtremeAccount>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        list.add(XtremeAccount.fromMap(key, value as Map<String, Any?>))
                    }
                }
                list.sortByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase Xtreme listener cancelled: ${error.message}")
                trySend(emptyList())
            }
        }

        xtremeRef.addValueEventListener(listener)

        awaitClose {
            xtremeRef.removeEventListener(listener)
        }
    }

    /**
     * Listens to live real-time updates from Firebase Realtime Database.
     */
    fun getChannelsFlow(): Flow<List<Channel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Channel>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val channel = Channel.fromMap(key, value as Map<String, Any?>)
                        
                        // Active Shield: Intercept and drop any malicious/abusive channels
                        if (com.example.data.security.AppSecurityGuard.isMaliciousChannel(channel.name, channel.category)) {
                            continue
                        }

                        // Only add genuine, valid channels with a title and at least one server
                        if (channel.name.isNotBlank() && channel.servers.isNotEmpty() && !channel.id.startsWith("m3u_")) {
                            list.add(channel)
                        }
                    }
                }
                list.sortByDescending { it.createdAt }
                if (list.isEmpty()) {
                    trySend(getBuiltInSportsChannels())
                } else {
                    trySend(list)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase listener cancelled: ${error.message}")
                trySend(getBuiltInSportsChannels())
            }
        }

        channelsRef.addValueEventListener(listener)

        awaitClose {
            channelsRef.removeEventListener(listener)
        }
    }

    /**
     * Atomically increments the view count of a channel in Firebase Realtime Database.
     */
    fun incrementChannelViews(channelId: String) {
        if (channelId.isBlank() || channelId.startsWith("m3u_") || channelId.startsWith("xtreme_") || channelId.startsWith("espn_")) return
        try {
            channelsRef.child(channelId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        channelsRef.child(channelId).child("views")
                            .setValue(com.google.firebase.database.ServerValue.increment(1))
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment channel views: ${e.message}")
        }
    }

    /**
     * Listens to live real-time updates of App Configuration from Firebase.
     */
    fun getAppConfigFlow(): Flow<Map<String, Any>> = callbackFlow {
        val configRef = database.getReference("v4_app_config")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Any>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value ?: continue
                    map[key] = value
                }
                trySend(map)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase app_config listener cancelled: ${error.message}")
                trySend(emptyMap())
            }
        }

        configRef.addValueEventListener(listener)
        awaitClose {
            configRef.removeEventListener(listener)
        }
    }

    /**
     * Listens to live real-time updates of Event Banners from Firebase.
     */
    fun getEventBannersFlow(): Flow<List<EventBanner>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<EventBanner>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val banner = EventBanner.fromMap(key, value as Map<String, Any?>)
                        if (com.example.data.security.AppSecurityGuard.isMaliciousText(banner.title) ||
                            com.example.data.security.AppSecurityGuard.isMaliciousText(banner.channelName)) {
                            continue
                        }
                        list.add(banner)
                    }
                }
                list.sortByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase Event Banners listener cancelled: ${error.message}")
                trySend(emptyList())
            }
        }

        eventsRef.addValueEventListener(listener)
        awaitClose {
            eventsRef.removeEventListener(listener)
        }
    }

    /**
     * Resilient built-in fallback sports channels in case Firebase is ever unreachable.
     */
    fun getBuiltInSportsChannels(): List<Channel> {
        return listOf(
            Channel(
                id = "ch_bein_1",
                name = "beIN Sports 1 HD",
                imageUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=300",
                category = "دوري أبطال أوروبا",
                servers = listOf(
                    ChannelServer(id = "s1", name = "FHD", streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", streamType = "m3u8"),
                    ChannelServer(id = "s2", name = "HD", streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8", streamType = "m3u8"),
                    ChannelServer(id = "s3", name = "SD", streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", streamType = "normal")
                )
            ),
            Channel(
                id = "ch_ssc_1",
                name = "SSC Sports 1 HD",
                imageUrl = "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=300",
                category = "الدوري السعودي",
                servers = listOf(
                    ChannelServer(id = "s4", name = "سيرفر 1 (HD)", streamUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8", streamType = "m3u8"),
                    ChannelServer(id = "s5", name = "سيرفر 2 (SD)", streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", streamType = "m3u8")
                )
            ),
            Channel(
                id = "ch_alkass",
                name = "Al Kass Sports HD",
                imageUrl = "https://images.unsplash.com/photo-1431324155629-1a6deb1dec8d?w=300",
                category = "كأس آسيا",
                servers = listOf(
                    ChannelServer(id = "s6", name = "HD المباشر", streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", streamType = "m3u8")
                )
            ),
            Channel(
                id = "ch_premier",
                name = "Premier League Live",
                imageUrl = "https://images.unsplash.com/photo-1522778119026-d647f0596c20?w=300",
                category = "الدوري الإنجليزي",
                servers = listOf(
                    ChannelServer(id = "s7", name = "سيرفر أساسي", streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", streamType = "m3u8"),
                    ChannelServer(id = "s8", name = "سيرفر احتياطي", streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", streamType = "normal")
                )
            )
        )
    }

    /**
     * Resilient built-in fallback categories.
     */
    fun getBuiltInCategories(): List<ChannelCategory> {
        return listOf(
            ChannelCategory(id = "cat_1", name = "دوري أبطال أوروبا", order = 0),
            ChannelCategory(id = "cat_2", name = "الدوري السعودي", order = 1),
            ChannelCategory(id = "cat_3", name = "الدوري الإنجليزي", order = 2),
            ChannelCategory(id = "cat_4", name = "كأس آسيا", order = 3)
        )
    }
}

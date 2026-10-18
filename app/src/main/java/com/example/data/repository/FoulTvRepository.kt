package com.example.data.repository

import android.util.Log
import com.example.data.model.FoulComment
import com.example.data.model.FoulTvGlobalConfig
import com.example.data.model.FoulVideo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * FoulTvRepository - Pure User App Implementation
 * Connects to Firebase Realtime Database for streaming video feeds, comments, and community interactions.
 * Administrative publishing and video management functions have been completely removed.
 */
class FoulTvRepository {

    companion object {
        const val FIREBASE_RTDB_URL = "https://foul-ddbb8-default-rtdb.firebaseio.com"
        private const val TAG = "FoulTvRepository"
    }

    private val database: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(FIREBASE_RTDB_URL)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining Firebase instance: ${e.message}")
            FirebaseDatabase.getInstance()
        }
    }

    private val videosRef by lazy {
        database.getReference("v4_foul_tv/videos").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    private val commentsRef by lazy {
        database.getReference("v4_foul_tv/comments").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    private val configRef by lazy {
        database.getReference("v4_foul_tv/config").apply {
            try { keepSynced(true) } catch (_: Exception) {}
        }
    }

    /**
     * Realtime flow of all FOUL TV videos sorted by creation timestamp
     */
    fun getVideosFlow(): Flow<List<FoulVideo>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FoulVideo>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val video = FoulVideo.fromMap(key, value as Map<String, Any?>)
                        if (com.example.data.security.AppSecurityGuard.isMaliciousText(video.title) ||
                            com.example.data.security.AppSecurityGuard.isMaliciousText(video.description)) {
                            continue
                        }
                        list.add(video)
                    }
                }
                list.sortByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Foul TV videos listener error: ${error.message}")
                trySend(emptyList())
            }
        }
        videosRef.addValueEventListener(listener)
        awaitClose { videosRef.removeEventListener(listener) }
    }

    /**
     * Realtime flow of comments for a specific video
     */
    fun getCommentsFlow(videoId: String): Flow<List<FoulComment>> = callbackFlow {
        if (videoId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val videoCommentsRef = commentsRef.child(videoId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<FoulComment>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val value = child.value
                    if (value is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val comment = FoulComment.fromMap(key, value as Map<String, Any?>)
                        list.add(comment)
                    }
                }
                list.sortByDescending { it.createdAt }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Foul TV comments listener error: ${error.message}")
                trySend(emptyList())
            }
        }
        videoCommentsRef.addValueEventListener(listener)
        awaitClose { videoCommentsRef.removeEventListener(listener) }
    }

    /**
     * Realtime flow of FOUL TV global settings
     */
    fun getGlobalConfigFlow(): Flow<FoulTvGlobalConfig> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hideViews = snapshot.child("hide_views_all").getValue(Boolean::class.java) ?: false
                val hideLikes = snapshot.child("hide_likes_all").getValue(Boolean::class.java) ?: false
                val disableComments = snapshot.child("disable_comments_all").getValue(Boolean::class.java) ?: false
                trySend(FoulTvGlobalConfig(hideViewsAll = hideViews, hideLikesAll = hideLikes, disableCommentsAll = disableComments))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(FoulTvGlobalConfig())
            }
        }
        configRef.addValueEventListener(listener)
        awaitClose { configRef.removeEventListener(listener) }
    }

    fun incrementVideoViews(videoId: String) {
        if (videoId.isBlank()) return
        videosRef.child(videoId).child("viewsCount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Long::class.java) ?: 0L
                currentData.value = current + 1
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.w(TAG, "Error incrementing views: ${error.message}")
                }
            }
        })
    }

    fun toggleVideoLike(videoId: String, username: String) {
        if (videoId.isBlank() || username.isBlank()) return
        videosRef.child(videoId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val rawLiked = currentData.child("likedUsernames").value
                val likedList = mutableListOf<String>()
                if (rawLiked is List<*>) {
                    likedList.addAll(rawLiked.filterIsInstance<String>())
                }

                val currentLikes = currentData.child("likesCount").getValue(Long::class.java) ?: 0L

                if (likedList.contains(username)) {
                    likedList.remove(username)
                    currentData.child("likesCount").value = (currentLikes - 1).coerceAtLeast(0L)
                } else {
                    likedList.add(username)
                    currentData.child("likesCount").value = currentLikes + 1
                }
                currentData.child("likedUsernames").value = likedList
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.w(TAG, "Error toggling like: ${error.message}")
                }
            }
        })
    }

    suspend fun addComment(comment: FoulComment): Result<Unit> = suspendCancellableCoroutine { cont ->
        val ref = commentsRef.child(comment.videoId).child(comment.id)
        ref.setValue(comment.toMap()).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Increment commentsCount on the video
                videosRef.child(comment.videoId).child("commentsCount").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val current = currentData.getValue(Long::class.java) ?: 0L
                        currentData.value = current + 1
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
                })
                cont.resume(Result.success(Unit))
            } else {
                cont.resume(Result.failure(task.exception ?: Exception("Failed to post comment")))
            }
        }
    }

    suspend fun deleteComment(videoId: String, commentId: String): Result<Unit> = suspendCancellableCoroutine { cont ->
        commentsRef.child(videoId).child(commentId).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Decrement commentsCount on the video
                videosRef.child(videoId).child("commentsCount").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val current = currentData.getValue(Long::class.java) ?: 0L
                        currentData.value = (current - 1).coerceAtLeast(0L)
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
                })
                cont.resume(Result.success(Unit))
            } else {
                cont.resume(Result.failure(task.exception ?: Exception("Failed to delete comment")))
            }
        }
    }
}

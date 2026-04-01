package org.thoughtcrime.securesms.groups

import android.util.Log
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.net.SignalNetwork
import org.thoughtcrime.securesms.recipients.GextTag
import org.whispersystems.signalservice.api.NetworkResult
import org.whispersystems.signalservice.api.groupsv2.GroupLinkNotActiveException
import org.whispersystems.signalservice.api.push.exceptions.AuthorizationFailedException
import java.util.concurrent.ConcurrentHashMap

object GExtGroupHelper {
  private val TAG  = "GExtGroupHelper"

  private const val FETCH_COOLDOWN_MS = 300_000L // 300 秒内同一 groupId 不重复请求
  private val lastFetchTime = ConcurrentHashMap<String, Long>()

  fun fetchAndStoreGroupTags(groupId: String) {
    val now = System.currentTimeMillis()
    val last = lastFetchTime[groupId] ?: 0L
    if (now - last < FETCH_COOLDOWN_MS) {
      Log.d(TAG, "fetchAndStoreGroupTags: skipping groupId=$groupId, last fetch ${now - last}ms ago")
      return
    }
    lastFetchTime[groupId] = now
    try {
      val serverGroupId = groupId.substringAfter("!")
      val result = SignalNetwork.gExtGroup.getGroupProfile(serverGroupId)
      when (result) {
        is NetworkResult.Success -> {
          //Log.i(TAG, "Network response for group $groupId: ${result.result.extTags.size} tags received")
          result.result.extTags.forEachIndexed { index, tag ->
          //Log.i(TAG, "Tag[$index]: tagId=${tag.tagId}, tagType=${tag.tagType}, text=${tag.text}")
          }
          val tags = result.result.extTags.map { serverTag ->
            GextTag(
              tagId = serverTag.tagId,
              tagType = serverTag.tagType,
              text = serverTag.text,
              imgBase64 = serverTag.imgBase64,
              cssBackgroundColor = serverTag.cssBackgroundColor,
              cssColor = serverTag.cssColor,
              cssOpacity = serverTag.cssOpacity,
              cssBorderWidth = serverTag.cssBorderWidth,
              cssBorderRadius = serverTag.cssBorderRadius,
              cssBorderColor = serverTag.cssBorderColor,
              cssBorderStyle = serverTag.cssBorderStyle
            )
          }
          SignalDatabase.gExtGroups.setGroupTags(groupId, tags)
          AppDependencies.databaseObserver.notifyGroupTagsChanged(groupId)
          Log.i(TAG, "Successfully fetched and stored ${tags.size} tags for group $groupId")

//          val db = SignalDatabase.rawDatabase
//          db.query("gext_groups", null, "group_id = ?", arrayOf(groupId), null, null, null).use { cursor ->
//            System.out.println("[GExtGroupHelper] DB query for groupId=$groupId, found=${cursor.count} row(s)")
//            if (cursor.moveToFirst()) {
//              for (i in 0 until cursor.columnCount) {
//                val colName = cursor.getColumnName(i)
//                val value = when (colName) {
//                  "tags" -> cursor.getBlob(i)?.let { "BLOB(${it.size} bytes)" } ?: "NULL"
//                  else -> cursor.getString(i)
//                }
//                System.out.println("[GExtGroupHelper] DB col[$i] $colName = $value")
//              }
//            }
//          }
        }
        is NetworkResult.StatusCodeError -> {
          Log.w(TAG, "Failed to fetch group tags: HTTP ${result.code}")
          lastFetchTime.remove(groupId)
        }
        is NetworkResult.NetworkError -> {
          Log.w(TAG, "Network error fetching group tags", result.exception)
          lastFetchTime.remove(groupId)
        }
        is NetworkResult.ApplicationError -> {
          Log.w(TAG, "Application error fetching group tags", result.throwable)
          lastFetchTime.remove(groupId)
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Exception fetching group tags for $groupId", e)
      lastFetchTime.remove(groupId)
    }
  }
}

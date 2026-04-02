package org.thoughtcrime.securesms.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.fasterxml.jackson.core.type.TypeReference
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.recipients.GextTag
import org.thoughtcrime.securesms.util.JsonUtils

class GExtGroupsTable(context: Context, databaseHelper: SignalDatabase) : DatabaseTable(context, databaseHelper) {

  companion object {
    private val TAG = Log.tag(GExtGroupsTable::class.java)

    const val TABLE_NAME = "gext_groups"

    private const val ID = "_id"
    const val GROUP_ID = "group_id"
    const val TAGS = "tags"
    const val LAST_UPDATED = "last_updated"

    private val TAGS_LIST_TYPE = object : TypeReference<List<GextTag>>() {}

    const val CREATE_TABLE = """
      CREATE TABLE $TABLE_NAME (
        $ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $GROUP_ID VARCHAR(64) NOT NULL,
        $TAGS BLOB NOT NULL,
        $LAST_UPDATED INTEGER NOT NULL,
        UNIQUE($GROUP_ID)
      )
    """
  }

  fun setGroupTags(groupId: String, tags: List<GextTag>) {
    val tagsBlob = JsonUtils.getMapper().writeValueAsBytes(tags)
    val values = ContentValues().apply {
      put(GROUP_ID, groupId)
      put(TAGS, tagsBlob)
      put(LAST_UPDATED, System.currentTimeMillis())
    }
    val rowId = writableDatabase.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    if (rowId >= 0) {
      Log.i(TAG, "setGroupTags: success groupId=$groupId, tagCount=${tags.size}")
    } else {
      Log.w(TAG, "setGroupTags: failed groupId=$groupId")
    }
  }

  fun getGroupTags(groupId: String): List<GextTag> {
    return readableDatabase
      .query(TABLE_NAME, arrayOf(TAGS), "$GROUP_ID = ?", arrayOf(groupId), null, null, null)
      .use { cursor ->
        if (cursor.moveToFirst()) {
          val blob = cursor.getBlob(0) ?: return emptyList()
          try {
            JsonUtils.getMapper().readValue(blob, TAGS_LIST_TYPE)
          } catch (e: Exception) {
            Log.w(TAG, "getGroupTags: deserialization error groupId=$groupId", e)
            emptyList()
          }
        } else {
          emptyList()
        }
      }
  }
}

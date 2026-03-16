package org.thoughtcrime.securesms.recipients

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.fasterxml.jackson.core.type.TypeReference
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.database.DatabaseTable
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.util.JsonUtils

class GExtRecipientTable(context: Context, databaseHelper: SignalDatabase) : DatabaseTable(context, databaseHelper) {

  companion object {
    private val TAG = Log.tag(GExtRecipientTable::class.java)

    const val TABLE_NAME = "gext_recipient"

    private const val ID = "_id"
    const val ACI = "aci"
    const val TAGS = "tags"
    const val LAST_UPDATED = "last_updated"

    private val TAGS_LIST_TYPE = object : TypeReference<List<GextTag>>() {}

    // _id stores RecipientTable._id (no AUTOINCREMENT)
    const val CREATE_TABLE = """
      CREATE TABLE $TABLE_NAME (
        $ID INTEGER NOT NULL,
        $ACI VARCHAR(32) NOT NULL,
        $TAGS BLOB NOT NULL,
        $LAST_UPDATED INTEGER NOT NULL,
        PRIMARY KEY($ID),
        UNIQUE($ACI)
      )
    """
  }

  fun setGextTags(recipientId: RecipientId, aci: String, tags: List<GextTag>) {
    Log.d(TAG, "setGextTags: serializing ${tags.size} tag(s) to BLOB for recipientId=${recipientId.toLong()}, aci=$aci")
    val tagsBlob = JsonUtils.getMapper().writeValueAsBytes(tags)
    Log.d(TAG, "setGextTags: serialized blob size=${tagsBlob.size} bytes")

    val values = ContentValues().apply {
      put(ID, recipientId.toLong())
      put(ACI, aci)
      put(TAGS, tagsBlob)
      put(LAST_UPDATED, System.currentTimeMillis())
    }

    Log.d(TAG, "setGextTags: executing INSERT OR REPLACE into $TABLE_NAME for recipientId=${recipientId.toLong()}")
    val rowId = writableDatabase.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)

    if (rowId >= 0) {
      Log.i(TAG, "setGextTags: [OK] upsert success rowId=$rowId, recipientId=${recipientId.toLong()}, aci=$aci, tagCount=${tags.size}")
    } else {
      Log.w(TAG, "setGextTags: [FAILED] insertWithOnConflict returned rowId=$rowId for recipientId=${recipientId.toLong()}")
    }
  }

  fun getGextTags(recipientId: RecipientId): List<GextTag> {
    Log.d(TAG, "getGextTags: querying $TABLE_NAME for recipientId=${recipientId.toLong()}")
    return readableDatabase
      .query(TABLE_NAME, arrayOf(TAGS), "$ID = ?", arrayOf(recipientId.serialize()), null, null, null)
      .use { cursor ->
        if (cursor.moveToFirst()) {
          val blob = cursor.getBlob(0)
          if (blob == null) {
            Log.w(TAG, "getGextTags: BLOB is null in DB for recipientId=${recipientId.toLong()}")
            return emptyList()
          }
          Log.d(TAG, "getGextTags: found BLOB size=${blob.size} bytes for recipientId=${recipientId.toLong()}, deserializing...")
          try {
            val result = JsonUtils.getMapper().readValue(blob, TAGS_LIST_TYPE)
            Log.i(TAG, "getGextTags: [OK] deserialized ${result.size} tag(s) for recipientId=${recipientId.toLong()}, tagIds=${result.map { it.tagId }}")
            result
          } catch (e: Exception) {
            Log.w(TAG, "getGextTags: [FAILED] deserialization error for recipientId=${recipientId.toLong()}", e)
            emptyList()
          }
        } else {
          Log.d(TAG, "getGextTags: no row found for recipientId=${recipientId.toLong()}")
          emptyList()
        }
      }
  }
}

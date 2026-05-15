package org.thoughtcrime.securesms.recipients

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.fasterxml.jackson.core.type.TypeReference
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.database.DatabaseTable
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.util.JsonUtils

class GExtRecipientTable(context: Context, databaseHelper: SignalDatabase) : DatabaseTable(context, databaseHelper) {

  companion object {
    private val TAG = Log.tag(GExtRecipientTable::class.java)

    const val TABLE_NAME = "gext_recipient"

    private const val ID = "_id"
    const val ACI = "aci"
    const val TAGS = "tags"
    const val LAST_UPDATED = "last_updated"
    const val ROBOT = "robot"

    private val TAGS_LIST_TYPE = object : TypeReference<List<GextTag>>() {}
    private val ROBOT_TYPE = object : TypeReference<GextRobot>() {}

    // _id stores RecipientTable._id (no AUTOINCREMENT)
    const val CREATE_TABLE = """
      CREATE TABLE $TABLE_NAME (
        $ID INTEGER NOT NULL,
        $ACI VARCHAR(32) NOT NULL,
        $TAGS BLOB NOT NULL,
        $LAST_UPDATED INTEGER NOT NULL,
        $ROBOT BLOB DEFAULT NULL,
        PRIMARY KEY($ID),
        UNIQUE($ACI)
      )
    """
  }

  fun setGextTags(recipientId: RecipientId, aci: String, tags: List<GextTag>) {
    val tagsBlob = JsonUtils.getMapper().writeValueAsBytes(tags)

    // 先尝试 UPDATE，只动 tags / aci / last_updated，避免冲掉 robot 列。
    val updateValues = ContentValues().apply {
      put(ACI, aci)
      put(TAGS, tagsBlob)
      put(LAST_UPDATED, System.currentTimeMillis())
    }
    val updated = writableDatabase.update(TABLE_NAME, updateValues, "$ID = ?", arrayOf(recipientId.serialize()))
    if (updated > 0) {
      Log.i(TAG, "setGextTags: [OK] update rows=$updated, recipientId=${recipientId.toLong()}, aci=$aci, tagCount=${tags.size}")
      AppDependencies.databaseObserver.notifyRecipientTagsChanged(recipientId)
      return
    }

    // 行不存在：插入，ROBOT 列保持默认 NULL。
    val insertValues = ContentValues().apply {
      put(ID, recipientId.toLong())
      put(ACI, aci)
      put(TAGS, tagsBlob)
      put(LAST_UPDATED, System.currentTimeMillis())
    }
    val rowId = writableDatabase.insertWithOnConflict(TABLE_NAME, null, insertValues, SQLiteDatabase.CONFLICT_REPLACE)
    if (rowId >= 0) {
      Log.i(TAG, "setGextTags: [OK] insert rowId=$rowId, recipientId=${recipientId.toLong()}, aci=$aci, tagCount=${tags.size}")
      AppDependencies.databaseObserver.notifyRecipientTagsChanged(recipientId)
    } else {
      Log.w(TAG, "setGextTags: [FAILED] insertWithOnConflict returned rowId=$rowId for recipientId=${recipientId.toLong()}")
    }
  }

  fun getGextTags(recipientId: RecipientId): List<GextTag> {
    return readableDatabase
      .query(TABLE_NAME, arrayOf(TAGS), "$ID = ?", arrayOf(recipientId.serialize()), null, null, null)
      .use { cursor ->
        if (cursor.moveToFirst()) {
          val blob = cursor.getBlob(0)
          if (blob == null) {
            return emptyList()
          }
          try {
            val result = JsonUtils.getMapper().readValue(blob, TAGS_LIST_TYPE)
            result
          } catch (e: Exception) {
            emptyList()
          }
        } else {
          emptyList()
        }
      }
  }

  /** 仅清空 tags 列（写为空数组 JSON），不动 robot 列。若行不存在则 no-op。 */
  fun clearGextTags(recipientId: RecipientId) {
    Log.d(TAG, "clearGextTags: clearing tags column for recipientId=${recipientId.toLong()}")
    val emptyTagsBlob = JsonUtils.getMapper().writeValueAsBytes(emptyList<GextTag>())
    val values = ContentValues().apply {
      put(TAGS, emptyTagsBlob)
      put(LAST_UPDATED, System.currentTimeMillis())
    }
    val updated = writableDatabase.update(TABLE_NAME, values, "$ID = ?", arrayOf(recipientId.serialize()))
    if (updated > 0) {
      Log.i(TAG, "clearGextTags: [OK] cleared rows=$updated for recipientId=${recipientId.toLong()}")
      AppDependencies.databaseObserver.notifyRecipientTagsChanged(recipientId)
    } else {
      Log.d(TAG, "clearGextTags: no row to clear for recipientId=${recipientId.toLong()}")
    }
  }

  fun setRobot(recipientId: RecipientId, aci: String, robot: GextRobot) {
    Log.d(TAG, "setRobot: serializing GextRobot to BLOB for recipientId=${recipientId.toLong()}, aci=$aci")
    val robotBlob = JsonUtils.getMapper().writeValueAsBytes(robot)
    Log.d(TAG, "setRobot: serialized blob size=${robotBlob.size} bytes")

    // 先尝试 UPDATE，只动 robot / aci / last_updated，避免冲掉 tags 列。
    val updateValues = ContentValues().apply {
      put(ACI, aci)
      put(ROBOT, robotBlob)
      put(LAST_UPDATED, System.currentTimeMillis())
    }

    Log.d(TAG, "setRobot: executing UPDATE/INSERT into $TABLE_NAME for recipientId=${recipientId.toLong()}")
    val updated = writableDatabase.update(TABLE_NAME, updateValues, "$ID = ?", arrayOf(recipientId.serialize()))

    if (updated > 0) {
      Log.i(TAG, "setRobot: [OK] update rows=$updated, recipientId=${recipientId.toLong()}, aci=$aci, robot=${robot.robot}")
      AppDependencies.databaseObserver.notifyRecipientTagsChanged(recipientId)
      return
    }

    // 行不存在：插入新行，TAGS 字段是 NOT NULL，给一个空数组的 JSON 字节填充
    val emptyTagsBlob = JsonUtils.getMapper().writeValueAsBytes(emptyList<GextTag>())
    val insertValues = ContentValues().apply {
      put(ID, recipientId.toLong())
      put(ACI, aci)
      put(TAGS, emptyTagsBlob)
      put(ROBOT, robotBlob)
      put(LAST_UPDATED, System.currentTimeMillis())
    }
    val rowId = writableDatabase.insertWithOnConflict(TABLE_NAME, null, insertValues, SQLiteDatabase.CONFLICT_REPLACE)
    if (rowId >= 0) {
      Log.i(TAG, "setRobot: [OK] insert rowId=$rowId, recipientId=${recipientId.toLong()}, aci=$aci, robot=${robot.robot}")
      AppDependencies.databaseObserver.notifyRecipientTagsChanged(recipientId)
    } else {
      Log.w(TAG, "setRobot: [FAILED] insertWithOnConflict returned rowId=$rowId for recipientId=${recipientId.toLong()}")
    }
  }

  fun clearRobot(recipientId: RecipientId) {
    Log.d(TAG, "clearRobot: clearing robot column for recipientId=${recipientId.toLong()}")
    val values = ContentValues().apply {
      putNull(ROBOT)
      put(LAST_UPDATED, System.currentTimeMillis())
    }
    val updated = writableDatabase.update(TABLE_NAME, values, "$ID = ?", arrayOf(recipientId.serialize()))
    if (updated > 0) {
      Log.i(TAG, "clearRobot: [OK] cleared rows=$updated for recipientId=${recipientId.toLong()}")
      AppDependencies.databaseObserver.notifyRecipientTagsChanged(recipientId)
    } else {
      Log.d(TAG, "clearRobot: no row to clear for recipientId=${recipientId.toLong()}")
    }
  }

  fun getRobot(recipientId: RecipientId): GextRobot? {
    Log.d(TAG, "getRobot: querying $TABLE_NAME for recipientId=${recipientId.toLong()}")
    return readableDatabase
      .query(TABLE_NAME, arrayOf(ROBOT), "$ID = ?", arrayOf(recipientId.serialize()), null, null, null)
      .use { cursor ->
        if (cursor.moveToFirst()) {
          val blob = cursor.getBlob(0)
          if (blob == null) {
            Log.d(TAG, "getRobot: ROBOT BLOB is null for recipientId=${recipientId.toLong()}")
            return null
          }
          Log.d(TAG, "getRobot: found BLOB size=${blob.size} bytes for recipientId=${recipientId.toLong()}, deserializing...")
          try {
            val result: GextRobot = JsonUtils.getMapper().readValue(blob, ROBOT_TYPE)
            Log.i(TAG, "getRobot: [OK] deserialized robot=${result.robot} for recipientId=${recipientId.toLong()}")
            result
          } catch (e: Exception) {
            Log.w(TAG, "getRobot: [FAILED] deserialization error for recipientId=${recipientId.toLong()}", e)
            null
          }
        } else {
          Log.d(TAG, "getRobot: no row found for recipientId=${recipientId.toLong()}")
          null
        }
      }
  }

  fun getGextTagsByAci(aci: String): List<GextTag> {
    Log.d(TAG, "getGextTagsByAci: querying $TABLE_NAME for aci=$aci")
    return readableDatabase
      .query(TABLE_NAME, arrayOf(TAGS), "$ACI = ?", arrayOf(aci), null, null, null)
      .use { cursor ->
        if (cursor.moveToFirst()) {
          val blob = cursor.getBlob(0)
          if (blob == null) {
            Log.w(TAG, "getGextTagsByAci: BLOB is null for aci=$aci")
            return emptyList()
          }
          try {
            val result = JsonUtils.getMapper().readValue(blob, TAGS_LIST_TYPE)
            Log.i(TAG, "getGextTagsByAci: [OK] deserialized ${result.size} tag(s) for aci=$aci")
            result
          } catch (e: Exception) {
            Log.w(TAG, "getGextTagsByAci: deserialization error for aci=$aci", e)
            emptyList()
          }
        } else {
          Log.d(TAG, "getGextTagsByAci: no row found for aci=$aci")
          emptyList()
        }
      }
  }
}

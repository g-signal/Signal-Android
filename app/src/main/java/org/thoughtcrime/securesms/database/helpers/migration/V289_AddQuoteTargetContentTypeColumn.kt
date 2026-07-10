/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.database.helpers.migration

import android.app.Application
import androidx.core.content.contentValuesOf
import org.signal.core.util.Base64
import org.signal.core.util.forEach
import org.signal.core.util.logging.Log
import org.signal.core.util.requireLong
import org.signal.core.util.requireNonNullString
import org.thoughtcrime.securesms.database.SQLiteDatabase

/**
 * Adds the quote_target_content_type column to attachments and migrates existing quote attachments
 * to populate this field with their current content_type.
 */
@Suppress("ClassName")
object V289_AddQuoteTargetContentTypeColumn : SignalDatabaseMigration {
  private val TAG = Log.tag(V289_AddQuoteTargetContentTypeColumn::class)

  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    // branch_from_tag_v7.58.0 分支V286_FixRemoteKeyEncoding
    var updated = 0
    db.query("SELECT _id, remote_key FROM attachment WHERE remote_key is not null AND LENGTH(remote_key) = 86").forEach {
      val id = it.requireLong("_id")
      val remoteKey = Base64.encodeWithPadding(Base64.decode(it.requireNonNullString("remote_key")))

      updated += db.update(
        "attachment",
        contentValuesOf("remote_key" to remoteKey),
        "_id = ? AND remote_key != ?",
        arrayOf(id.toString(), remoteKey)
      )
    }
    Log.i(TAG, "Updated $updated attachment remote_keys")
    //branch_from_tag_v7.58.0 分支 V287_FixInvalidArchiveState
    db.execSQL("UPDATE attachment SET archive_cdn = null, archive_transfer_state = 0 WHERE remote_key IS NULL AND archive_transfer_state = 3")

    //branch_from_tag_v7.58.0 分支 V288_CopyStickerDataHashStartToEnd
    db.execSQL(
      "UPDATE attachment SET data_hash_end = data_hash_start WHERE sticker_pack_id IS NOT NULL AND data_hash_start IS NOT NULL AND data_hash_end IS NULL AND transfer_state = 0"
    )
    db.execSQL("ALTER TABLE attachment ADD COLUMN quote_target_content_type TEXT DEFAULT NULL;")
    db.execSQL("UPDATE attachment SET quote_target_content_type = content_type WHERE quote != 0;")
  }
}

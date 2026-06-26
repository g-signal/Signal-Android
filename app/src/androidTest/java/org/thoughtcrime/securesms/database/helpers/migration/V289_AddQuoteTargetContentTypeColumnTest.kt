package org.thoughtcrime.securesms.database.helpers.migration

import android.app.Application
import androidx.core.content.contentValuesOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.signal.core.util.requireInt
import org.signal.core.util.requireLong
import org.signal.core.util.requireString
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.testing.SignalDatabaseRule
import org.thoughtcrime.securesms.database.SQLiteDatabase as SignalSQLiteDatabase

/**
 * Tests for V289_AddQuoteTargetContentTypeColumn migration.
 *
 * This migration combines three upstream migrations (V286/V287/V288) plus its own column addition:
 * 1. V286_FixRemoteKeyEncoding - Fix base64 padding on remote_key
 * 2. V287_FixInvalidArchiveState - Clear archive state when remote_key is NULL
 * 3. V288_CopyStickerDataHashStartToEnd - Copy data_hash_start to data_hash_end for stickers
 * 4. Add quote_target_content_type column
 *
 * Related: docs/merge-fix-linkdevice-api-migration.md
 */
@RunWith(AndroidJUnit4::class)
class V289_AddQuoteTargetContentTypeColumnTest {

  @get:Rule
  val harness = SignalDatabaseRule(startVersion = 288, endVersion = 289)

  private val application: Application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

  /**
   * Test upstream V286: Fix remote_key base64 padding
   *
   * Attachments with remote_key length 86 (missing padding) should be re-encoded to 88 with padding
   */
  @Test
  fun v286_fixRemoteKeyEncoding_addsPaddingTo86CharKeys() {
    // GIVEN: Attachment with 86-char remote_key (no padding)
    val unpadded86CharKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkw" // 86 chars
    val attachmentId = insertAttachment(remoteKey = unpadded86CharKey)

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: remote_key should be 88 chars (with padding)
    db.query("SELECT remote_key FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      val remoteKey = cursor.requireString("remote_key")
      assertEquals("Should add padding to make 88 chars", 88, remoteKey.length)
      assertEquals("Should end with padding characters", "==", remoteKey.takeLast(2))
    }
  }

  @Test
  fun v286_fixRemoteKeyEncoding_leaves88CharKeysUnchanged() {
    // GIVEN: Attachment with 88-char remote_key (already has padding)
    val padded88CharKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI==" // 88 chars
    val attachmentId = insertAttachment(remoteKey = padded88CharKey)

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: remote_key should remain unchanged
    db.query("SELECT remote_key FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      val remoteKey = cursor.requireString("remote_key")
      assertEquals(padded88CharKey, remoteKey)
    }
  }

  @Test
  fun v286_fixRemoteKeyEncoding_ignoresNullRemoteKey() {
    // GIVEN: Attachment with NULL remote_key
    val attachmentId = insertAttachment(remoteKey = null)

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: remote_key should still be NULL
    db.query("SELECT remote_key FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertNull(cursor.getString(cursor.getColumnIndexOrThrow("remote_key")))
    }
  }

  /**
   * Test upstream V287: Fix invalid archive state
   *
   * Attachments with NULL remote_key but archive_transfer_state=3 should be reset
   */
  @Test
  fun v287_fixInvalidArchiveState_clearsStateWhenRemoteKeyIsNull() {
    // GIVEN: Attachment with NULL remote_key but archive_transfer_state=3
    val attachmentId = insertAttachment(
      remoteKey = null,
      archiveCdn = 2,
      archiveTransferState = 3
    )

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: archive_cdn should be NULL and archive_transfer_state should be 0
    db.query("SELECT archive_cdn, archive_transfer_state FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertNull("archive_cdn should be NULL", cursor.getString(cursor.getColumnIndexOrThrow("archive_cdn")))
      assertEquals("archive_transfer_state should be 0", 0, cursor.requireInt("archive_transfer_state"))
    }
  }

  @Test
  fun v287_fixInvalidArchiveState_preservesStateWhenRemoteKeyExists() {
    // GIVEN: Attachment with remote_key AND archive_transfer_state=3
    val attachmentId = insertAttachment(
      remoteKey = "validkey123",
      archiveCdn = 2,
      archiveTransferState = 3
    )

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: archive state should be preserved
    db.query("SELECT archive_cdn, archive_transfer_state FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertEquals("archive_cdn should be preserved", 2, cursor.requireInt("archive_cdn"))
      assertEquals("archive_transfer_state should be preserved", 3, cursor.requireInt("archive_transfer_state"))
    }
  }

  @Test
  fun v287_fixInvalidArchiveState_ignoresOtherTransferStates() {
    // GIVEN: Attachment with NULL remote_key but archive_transfer_state=0 (not 3)
    val attachmentId = insertAttachment(
      remoteKey = null,
      archiveCdn = null,
      archiveTransferState = 0
    )

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: No changes (already in correct state)
    db.query("SELECT archive_cdn, archive_transfer_state FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertNull(cursor.getString(cursor.getColumnIndexOrThrow("archive_cdn")))
      assertEquals(0, cursor.requireInt("archive_transfer_state"))
    }
  }

  /**
   * Test upstream V288: Copy sticker data_hash_start to data_hash_end
   *
   * Stickers with data_hash_start but NULL data_hash_end should be copied
   */
  @Test
  fun v288_copyStickerDataHash_copiesHashForCompletedStickers() {
    // GIVEN: Sticker attachment with data_hash_start but NULL data_hash_end, transfer_state=0
    val attachmentId = insertAttachment(
      stickerPackId = "test-sticker-pack",
      dataHashStart = "hash123",
      dataHashEnd = null,
      transferState = 0
    )

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: data_hash_end should equal data_hash_start
    db.query("SELECT data_hash_start, data_hash_end FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor →
      cursor.moveToFirst()
      val hashStart = cursor.requireString("data_hash_start")
      val hashEnd = cursor.requireString("data_hash_end")
      assertEquals("data_hash_end should match data_hash_start", hashStart, hashEnd)
    }
  }

  @Test
  fun v288_copyStickerDataHash_ignoresNonStickerAttachments() {
    // GIVEN: Non-sticker attachment with data_hash_start
    val attachmentId = insertAttachment(
      stickerPackId = null,
      dataHashStart = "hash123",
      dataHashEnd = null,
      transferState = 0
    )

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: data_hash_end should still be NULL
    db.query("SELECT data_hash_end FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertNull("data_hash_end should remain NULL for non-stickers", cursor.getString(cursor.getColumnIndexOrThrow("data_hash_end")))
    }
  }

  @Test
  fun v288_copyStickerDataHash_ignoresInProgressTransfers() {
    // GIVEN: Sticker with transfer_state=1 (in progress)
    val attachmentId = insertAttachment(
      stickerPackId = "test-sticker-pack",
      dataHashStart = "hash123",
      dataHashEnd = null,
      transferState = 1
    )

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: data_hash_end should still be NULL
    db.query("SELECT data_hash_end FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertNull("data_hash_end should remain NULL for in-progress transfers", cursor.getString(cursor.getColumnIndexOrThrow("data_hash_end")))
    }
  }

  @Test
  fun v288_copyStickerDataHash_preservesExistingHashEnd() {
    // GIVEN: Sticker with both data_hash_start and data_hash_end already set
    val attachmentId = insertAttachment(
      stickerPackId = "test-sticker-pack",
      dataHashStart = "hash123",
      dataHashEnd = "different_hash",
      transferState = 0
    )

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: data_hash_end should remain unchanged
    db.query("SELECT data_hash_end FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertEquals("different_hash", cursor.requireString("data_hash_end"))
    }
  }

  /**
   * Test V289 itself: Add quote_target_content_type column
   */
  @Test
  fun v289_addQuoteTargetContentTypeColumn_addsColumnSuccessfully() {
    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: Column should exist
    val columnExists = db.query("PRAGMA table_info(attachment)").use { cursor ->
      var found = false
      while (cursor.moveToNext()) {
        if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "quote_target_content_type") {
          found = true
          break
        }
      }
      found
    }
    assertEquals("quote_target_content_type column should exist", true, columnExists)
  }

  @Test
  fun v289_addQuoteTargetContentTypeColumn_populatesForQuoteAttachments() {
    // GIVEN: Attachment with quote=1 and content_type
    val attachmentId = insertAttachment(
      contentType = "image/jpeg",
      quote = 1
    )

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: quote_target_content_type should be populated
    db.query("SELECT quote_target_content_type FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertEquals("image/jpeg", cursor.requireString("quote_target_content_type"))
    }
  }

  @Test
  fun v289_addQuoteTargetContentTypeColumn_leavesNullForNonQuoteAttachments() {
    // GIVEN: Attachment with quote=0
    val attachmentId = insertAttachment(
      contentType = "image/png",
      quote = 0
    )

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: quote_target_content_type should be NULL
    db.query("SELECT quote_target_content_type FROM attachment WHERE _id = ?", arrayOf(attachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertNull("Non-quote attachments should have NULL quote_target_content_type",
                 cursor.getString(cursor.getColumnIndexOrThrow("quote_target_content_type")))
    }
  }

  /**
   * Integration test: All four migrations work together
   */
  @Test
  fun v289_allMigrations_workTogetherWithoutConflicts() {
    // GIVEN: Complex scenario with multiple attachment types
    val quoteAttachmentId = insertAttachment(contentType = "image/jpeg", quote = 1)
    val stickerAttachmentId = insertAttachment(
      stickerPackId = "sticker-pack",
      dataHashStart = "sticker_hash",
      dataHashEnd = null,
      transferState = 0
    )
    val brokenArchiveAttachmentId = insertAttachment(
      remoteKey = null,
      archiveCdn = 2,
      archiveTransferState = 3
    )
    val unpaddedKeyAttachmentId = insertAttachment(
      remoteKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkw" // 86 chars
    )

    // WHEN: Run migration
    val db = harness.writableDatabase
    V289_AddQuoteTargetContentTypeColumn.migrate(application, db, 288, 289)

    // THEN: All fixes should be applied
    // 1. Quote attachment has quote_target_content_type
    db.query("SELECT quote_target_content_type FROM attachment WHERE _id = ?", arrayOf(quoteAttachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertEquals("image/jpeg", cursor.requireString("quote_target_content_type"))
    }

    // 2. Sticker has data_hash_end copied
    db.query("SELECT data_hash_end FROM attachment WHERE _id = ?", arrayOf(stickerAttachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertEquals("sticker_hash", cursor.requireString("data_hash_end"))
    }

    // 3. Broken archive state is cleared
    db.query("SELECT archive_cdn, archive_transfer_state FROM attachment WHERE _id = ?", arrayOf(brokenArchiveAttachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      assertNull(cursor.getString(cursor.getColumnIndexOrThrow("archive_cdn")))
      assertEquals(0, cursor.requireInt("archive_transfer_state"))
    }

    // 4. Remote key has padding
    db.query("SELECT remote_key FROM attachment WHERE _id = ?", arrayOf(unpaddedKeyAttachmentId.toString())).use { cursor ->
      cursor.moveToFirst()
      val remoteKey = cursor.requireString("remote_key")
      assertEquals(88, remoteKey.length)
    }
  }

  /**
   * Helper: Insert test attachment with specified fields
   */
  private fun insertAttachment(
    contentType: String = "image/jpeg",
    quote: Int = 0,
    remoteKey: String? = null,
    archiveCdn: Int? = null,
    archiveTransferState: Int = 0,
    stickerPackId: String? = null,
    dataHashStart: String? = null,
    dataHashEnd: String? = null,
    transferState: Int = 0
  ): Long {
    val db = harness.writableDatabase
    val values = contentValuesOf(
      "content_type" to contentType,
      "quote" to quote,
      "transfer_state" to transferState,
      "archive_transfer_state" to archiveTransferState
    )

    if (remoteKey != null) values.put("remote_key", remoteKey)
    if (archiveCdn != null) values.put("archive_cdn", archiveCdn)
    if (stickerPackId != null) values.put("sticker_pack_id", stickerPackId)
    if (dataHashStart != null) values.put("data_hash_start", dataHashStart)
    if (dataHashEnd != null) values.put("data_hash_end", dataHashEnd)

    return db.insert("attachment", null, values)
  }
}

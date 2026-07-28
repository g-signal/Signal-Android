package org.thoughtcrime.securesms.database.helpers.migration

import android.app.Application
import org.signal.core.util.SqlUtil
import org.thoughtcrime.securesms.database.SQLiteDatabase

@Suppress("ClassName")
object V288_AddGExtRecipientTableRobotFiled : SignalDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (!SqlUtil.columnExists(db, "gext_recipient", "robot")) {
      db.execSQL("ALTER TABLE gext_recipient ADD COLUMN robot BLOB DEFAULT NULL")
    }
  }
}

package org.thoughtcrime.securesms.database.helpers.migration

import android.app.Application
import org.thoughtcrime.securesms.database.SQLiteDatabase
import org.thoughtcrime.securesms.recipients.GExtRecipientTable

@Suppress("ClassName")
object V286_AddGExtRecipientTable : SignalDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(GExtRecipientTable.CREATE_TABLE)
  }
}

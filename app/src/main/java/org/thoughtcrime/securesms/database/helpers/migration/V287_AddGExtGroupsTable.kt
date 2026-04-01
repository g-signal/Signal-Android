package org.thoughtcrime.securesms.database.helpers.migration

import android.app.Application
import org.thoughtcrime.securesms.database.GExtGroupsTable
import org.thoughtcrime.securesms.database.SQLiteDatabase

@Suppress("ClassName")
object V287_AddGExtGroupsTable : SignalDatabaseMigration {
  override fun migrate(context: Application, db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    db.execSQL(GExtGroupsTable.CREATE_TABLE)
  }
}

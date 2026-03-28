package org.thoughtcrime.securesms.database

import android.content.Context
import org.thoughtcrime.securesms.database.DatabaseTable
import org.thoughtcrime.securesms.database.SignalDatabase

class GExtRecipientTable(context: Context, databaseHelper: SignalDatabase) : DatabaseTable(context, databaseHelper) {

  companion object {
    const val TABLE_NAME = "gext_recipient"

    private const val ID = "_id"
    const val ACI = "aci"
    const val TAGS = "tags"
    const val LAST_UPDATED = "last_updated"

    const val CREATE_TABLE = """
      CREATE TABLE $TABLE_NAME (
        $ID INTEGER PRIMARY KEY ,
        $ACI VARCHAR(32) NOT NULL,
        $TAGS BLOB NOT NULL,
        $LAST_UPDATED INTEGER NOT NULL,
        UNIQUE($ACI)
      )
    """
  }
}

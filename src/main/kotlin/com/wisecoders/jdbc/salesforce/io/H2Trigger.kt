package com.wisecoders.jdbc.salesforce.io

import com.sforce.soap.partner.PartnerConnection
import com.wisecoders.jdbc.salesforce.SalesforceConnection
import com.wisecoders.jdbc.salesforce.schema.Table
import java.sql.Connection
import java.sql.SQLException
import org.h2.api.Trigger

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/salesforce-jdbc-driver).
 */
class H2Trigger : Trigger {
    private var table: Table? = null

    @Throws(SQLException::class)
    override fun init(
        conn: Connection,
        schemaName: String,
        triggerName: String,
        tableName: String,
        before: Boolean,
        type: Int
    ) {
        val schema = SalesforceConnection.getSchema(schemaName)
        if (schema != null) {
            table = schema.getTable(tableName)
        }
    }

    @Throws(SQLException::class)
    override fun fire(
        conn: Connection,
        oldRow: Array<Any>?,
        newRow: Array<Any>?
    ) {
        if (table != null && partnerConnection != null) {
            if (oldRow != null && newRow != null) {
                val sb = StringBuilder("INSERT INTO \"").append(table!!.name).append("\"( ").append(
                    table!!.columnList
                ).append("\" VALUES (")
                for (i in newRow.indices) {
                    if (i > 0) sb.append(",")
                    sb.append("?")
                }
                sb.append(")")
            }
        }
    }

    @Throws(SQLException::class)
    override fun close() {
    }

    @Throws(SQLException::class)
    override fun remove() {
    }

    companion object {
        @JvmField
        var partnerConnection: PartnerConnection? = null
    }
}

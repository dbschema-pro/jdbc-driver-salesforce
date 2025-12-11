package com.wisecoders.jdbc.salesforce.io

import com.sforce.soap.partner.QueryResult
import com.sforce.ws.bind.XmlObject
import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.salesforce.SalesforceConnection
import com.wisecoders.jdbc.salesforce.schema.Table
import java.sql.SQLException
import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors
import org.apache.commons.collections4.IteratorUtils

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/salesforce-jdbc-driver).
 */
class TransferReader(private val salesforceConnection: SalesforceConnection) {
    @Throws(SQLException::class)
    fun transferSchema() {
        LOGGER.atInfo().setMessage(("Transfer schema...")).log()
        salesforceConnection.ensureColumnsAreLoaded()
        val schemaDef = salesforceConnection.schemaDef
        if (schemaDef != null) {
            for (table in schemaDef.tables) {
                val writer = TransferWriter(table, salesforceConnection)
                writer.createTable()
            }
        }
    }

    @Throws(SQLException::class)
    fun transferAllData() {
        LOGGER.atInfo().setMessage(("Transfer all data...")).log()
        salesforceConnection.ensureColumnsAreLoaded()
        val schemaDef = salesforceConnection.schemaDef
        if (schemaDef != null) {
            for (table in schemaDef.tables) {
                transferData(table)
            }
        }
    }

    @Throws(SQLException::class)
    fun transferData(table: Table) {
        val writer = TransferWriter(table, salesforceConnection)
        salesforceConnection.ensureColumnsAreLoaded()
        LOGGER.atInfo().setMessage(("Transfer '$table' data...")).log()
        Thread.dumpStack()
        writer.createTable()
        if (table.isQueriable && !table.columns.isEmpty()) {
            try {
                val start = Instant.now()
                val sql = "SELECT " + table.columnList + " FROM " + table
                var queryResult: QueryResult? = null
                var rows = 0
                do {
                    queryResult =
                        if (queryResult == null) salesforceConnection.partnerConnection.query(sql) else salesforceConnection.partnerConnection.queryMore(
                            queryResult.queryLocator
                        )
                    val records = queryResult.records
                    for (row in records) {
                        val clearRow = removeServiceInfo(row)
                        writer.transferRecord(clearRow)
                    }

                    rows += records.size
                } while (!queryResult!!.isDone)
                LOGGER.atInfo().setMessage(
                    "Transferred '$table' $rows rows in " + Duration.between(
                        start,
                        Instant.now()
                    ).seconds + " sec"
                ).log()
            } catch (ex: Throwable) {
                LOGGER.atError().setMessage("Error transferring data").setCause(ex).log()
                ex.printStackTrace()
                //throw new SQLException(ex);
            }
        }
    }


    private fun removeServiceInfo(rows: Iterator<XmlObject>): List<List<*>> {
        return removeServiceInfo(IteratorUtils.toList(rows))
    }

    private fun removeServiceInfo(rows: List<XmlObject>): List<List<*>> {
        return rows.stream()
            .filter { `object`: XmlObject -> this.isDataObjectType(`object`) }
            .map { row: XmlObject -> this.removeServiceInfo(row) }
            .collect(Collectors.toList())
    }

    private fun removeServiceInfo(row: XmlObject): List<ForceResultField> {
        val obj: List<*> = IteratorUtils.toList(row.children).stream()
            .filter { `object`: XmlObject -> this.isDataObjectType(`object`) }
            .skip(1) // Removes duplicate Id from SF Partner API response
            // (https://developer.salesforce.com/forums/?id=906F00000008kciIAA)
            .map { field: XmlObject ->
                if (isNestedResultSet(field))
                    removeServiceInfo(field.children)
                else
                    toForceResultField(field)
            }
            .collect(Collectors.toList())
        return obj as List<ForceResultField>
    }

    private fun toForceResultField(field: XmlObject): ForceResultField {
        var field = field
        val fieldType = if (field.xmlType != null) field.xmlType.localPart else null
        if ("sObject".equals(fieldType, ignoreCase = true)) {
            val children: MutableList<XmlObject> = ArrayList()
            field.children.forEachRemaining { e: XmlObject -> children.add(e) }
            field = children[2]
        }
        val name = field.name.localPart
        val value = field.value
        return ForceResultField(null, name, value)
    }

    private fun isNestedResultSet(obj: XmlObject): Boolean {
        return obj.xmlType != null && "QueryResult" == obj.xmlType.localPart
    }

    private fun isDataObjectType(`object`: XmlObject): Boolean {
        return !SOAP_RESPONSE_SERVICE_OBJECT_TYPES.contains(`object`.name.localPart)
    }


    companion object {
        val LOGGER = slf4jLogger()

        private val SOAP_RESPONSE_SERVICE_OBJECT_TYPES: List<String> =
            mutableListOf("type", "done", "queryLocator", "size")
    }
}

package com.wisecoders.jdbc.salesforce.schema

import com.sforce.soap.partner.Field
import com.sforce.soap.partner.PartnerConnection
import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import java.sql.SQLException

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/salesforce-jdbc-driver).
 */
class Schema(private val showTables: ShowTables) {

    val tables: MutableList<Table> = ArrayList()

    @Throws(SQLException::class)
    fun refreshTables(connection: PartnerConnection) {
        LOGGER.atInfo().setMessage(("Load schema tables...")).log()
        try {
            val loadedTables = mutableListOf<Table>()
            for (desc in connection.describeGlobal().sobjects) {
                if (desc.isQueryable &&
                    (showTables == ShowTables.ALL ||
                            (showTables == ShowTables.CUSTOM && desc.isCustom) ||
                            (showTables == ShowTables.INTERN && !desc.isCustom))
                ) {
                    loadedTables.add(Table(desc.name, desc.isQueryable, desc.label))
                }
            }
            tables.clear()
            tables.addAll(loadedTables)
        } catch (ex: Throwable) {
            throw SQLException(ex)
        }
    }


    @Throws(SQLException::class)
    fun refreshColumns(connection: PartnerConnection) {
        LOGGER.atInfo().setMessage(("Load schema columns...")).log()
        try {
            for (table in tables) {
                val result = connection.describeSObject(table.name)
                for (field in result.fields) {
                    val column = table.createColumn(
                        field.name, getType(field),
                        field.length, field.digits, field.scale, field.isNillable, field.isAutoNumber, field.label
                    )
                    column.isCalculated = field.isCalculated || field.isAutoNumber
                    val referenceTos = field.referenceTo
                    if (referenceTos != null) {
                        for (referenceTo in referenceTos) {
                            val pkTable = getTable(referenceTo)
                            if (pkTable != null) {
                                table.createForeignKey(column, pkTable)
                            }
                        }
                    }
                }
            }
        } catch (ex: Throwable) {
            throw SQLException(ex)
        }
    }

    @Throws(SQLException::class)
    fun ensureTablesAreLoaded(partnerConnection: PartnerConnection) {
        if (tables.isEmpty()) {
            refreshTables(partnerConnection)
        }
    }

    @Throws(SQLException::class)
    fun ensureColumnsAreLoaded(partnerConnection: PartnerConnection) {
        ensureTablesAreLoaded(partnerConnection)
        for (table in tables) {
            if (table.columns.isNotEmpty()) return
        }
        refreshColumns(partnerConnection)
    }

    fun getTable(name: String): Table? {
        for (table in tables) {
            if (name == table.name) {
                return table
            }
        }
        return null
    }

    companion object {
        private val LOGGER = slf4jLogger()


        private fun getType(field: Field): String {
            var s = field.type.toString()
            if (s.startsWith("_")) {
                s = s.substring("_".length)
            }
            return if (s.equals("double", ignoreCase = true)) "decimal" else s
        }
    }
}

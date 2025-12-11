package com.wisecoders.jdbc.salesforce.io

import com.wisecoders.common_lib.common_slf4j.slf4jLogger
import com.wisecoders.jdbc.salesforce.SalesforceConnection
import com.wisecoders.jdbc.salesforce.schema.Table
import java.sql.SQLException
import java.sql.Types

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/salesforce-jdbc-driver).
 */
class TransferWriter(
    private val table: Table,
    private val salesforceConnection: SalesforceConnection,
) {
    private var insertSql: String? = null


    @Throws(SQLException::class)
    fun createTable() {
        LOGGER.atInfo().setMessage("Transfer table '" + table.name + "'").log()
        val createSb =
            StringBuilder("create table ").append(QUOTE_CHAR).append(table.name).append(QUOTE_CHAR).append("(\n")
        val insertSb =
            StringBuilder("insert into ").append(QUOTE_CHAR).append(table.name).append(QUOTE_CHAR).append("(")
        val insertValuesSb = StringBuilder("values(")
        var appendComma = false
        for (column in table.columns) {
            if (appendComma) {
                createSb.append(",\n")
                insertSb.append(",")
                insertValuesSb.append(",")
            }
            createSb.append("\t").append(QUOTE_CHAR).append(column).append(QUOTE_CHAR).append(" ")
            insertSb.append(QUOTE_CHAR).append(column).append(QUOTE_CHAR)
            insertValuesSb.append("?")
            createSb.append(column.h2Type)
            appendComma = true
        }
        createSb.append(")")
        insertSb.append(")")
        insertValuesSb.append(")")

        val dropTableSQL = "drop table if exists " + QUOTE_CHAR + table.name + QUOTE_CHAR
        //LOGGER.log(Level.INFO, dropTableSQL);
        salesforceConnection.h2Connection.prepareStatement(dropTableSQL).execute()
        salesforceConnection.h2Connection.commit()


        //LOGGER.log(Level.INFO, createSb.toString());
        salesforceConnection.h2Connection.prepareStatement(createSb.toString()).execute()
        salesforceConnection.h2Connection.commit()

        /*
        THIS CAN BE USED TO WRITE DATA BACK TO SALESFORCE
        String createTriggerSQL = "CREATE TRIGGER " + QUOTE_CHAR + "trg_" + table.name + QUOTE_CHAR +
                "BEFORE UPDATE, INSERT, DELETE ON " + QUOTE_CHAR + table.name + QUOTE_CHAR +
                " FOR EACH ROW\n" +
                " CALL \"com.wisecoders.dbschema.salesforce.io.H2Trigger\"";

        salesforceConnection.h2Connection.prepareStatement( createTriggerSQL ).execute();
        salesforceConnection.h2Connection.commit();
        */
        this.insertSql = insertSb.toString() + insertValuesSb.toString()
    }


    @Throws(Exception::class)
    fun transferRecord(fields: List<ForceResultField>) {
        val stInsert = salesforceConnection.h2Connection.prepareStatement(insertSql)
        for (field in fields) {
            val value = field.value
            val column = table.getColumn(field.name)
            val i = table.columns.indexOf(column)
            if (value == null) {
                stInsert.setNull(i + 1, Types.VARCHAR)
            } else {
                stInsert.setObject(i + 1, value)
            }
        }
        stInsert.execute()
        stInsert.close()
        salesforceConnection.h2Connection.commit()
    }


    companion object {

        val LOGGER = slf4jLogger()
        const val QUOTE_CHAR: Char = '"'
    }
}

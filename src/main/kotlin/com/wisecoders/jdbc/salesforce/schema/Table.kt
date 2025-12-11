package com.wisecoders.jdbc.salesforce.schema

import java.io.Serializable
import java.util.regex.Pattern

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/salesforce-jdbc-driver).
 */
class Table(
    val name: String,
    val isQueriable: Boolean,
    val comment: String
) : Serializable {

    val findNamePattern: Pattern = Pattern.compile("\\W$name\\W|\\W$name$", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
    val columns: MutableList<Column> = ArrayList()
    val foreignKeys: MutableList<ForeignKey> = ArrayList()
    var isLoaded: Boolean = false

    fun createColumn(
        name: String,
        type: String,
        length: Int,
        digits: Int,
        scale: Int,
        nullable: Boolean,
        autoIncrement: Boolean,
        comment: String
    ): Column {
        val column = Column(this, name, type, length, digits, scale, nullable, autoIncrement, comment)
        columns.add(column)
        return column
    }

    fun createForeignKey(
        fromColumn: Column,
        targetTable: Table
    ): ForeignKey {
        val fk = ForeignKey(fromColumn, targetTable)
        foreignKeys.add(fk)
        return fk
    }

    val columnList: String
        get() {
            val sb = StringBuilder()
            for (column in columns) {
                if (sb.isNotEmpty()) sb.append(", ")
                sb.append(column)
            }
            return sb.toString()
        }

    fun getColumn(name: String): Column? {
        for (column in columns) {
            if (name == column.name) return column
        }
        return null
    }

    override fun toString(): String {
        return name
    }
}

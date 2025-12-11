package com.wisecoders.jdbc.salesforce.schema

import java.io.Serializable
import java.sql.Types

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/salesforce-jdbc-driver).
 */
class Column(
    val table: Table,
    val name: String,
    val type: String,
    var length: Int,
    var digits: Int,
    var scale: Int,
    var nullable: Boolean,
    var autoIncrement: Boolean,
    val comment: String
) :
    Serializable {
    var isCalculated: Boolean = false


    val javaType: Int
        get() {
            for (info in TypeInfo.SALESFORCE_TYPES) {
                if (info.typeName.equals(type, ignoreCase = true)) {
                    return info.javaSqlType
                }
            }
            return Types.OTHER
        }

    val h2Type: String
        get() = "varchar"


    override fun toString(): String {
        return name
    }
}

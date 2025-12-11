package com.wisecoders.jdbc.salesforce.schema

import java.sql.Types

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/salesforce-jdbc-driver).
 */
class TypeInfo(
    var typeName: String,
    var javaSqlType: Int,
) {
    companion object {
        var OTHER_TYPE_INFO: TypeInfo = TypeInfo("other", Types.OTHER)

        var SALESFORCE_TYPES: Array<TypeInfo> = arrayOf(
            TypeInfo("id", Types.VARCHAR),
            TypeInfo("masterrecord", Types.VARCHAR),
            TypeInfo("reference", Types.VARCHAR),
            TypeInfo("string", Types.VARCHAR),
            TypeInfo("encryptedstring", Types.VARCHAR),
            TypeInfo("email", Types.VARCHAR),
            TypeInfo("phone", Types.VARCHAR),
            TypeInfo("url", Types.VARCHAR),
            TypeInfo("textarea", Types.LONGVARCHAR),
            TypeInfo("base64", Types.BLOB),
            TypeInfo("boolean", Types.BOOLEAN),
            TypeInfo("_boolean", Types.BOOLEAN),
            TypeInfo("byte", Types.VARBINARY),
            TypeInfo("_byte", Types.VARBINARY),
            TypeInfo("int", Types.INTEGER),
            TypeInfo("_int", Types.INTEGER),
            TypeInfo("decimal", Types.DECIMAL),
            TypeInfo("double", Types.DOUBLE),
            TypeInfo("_double", Types.DOUBLE),
            TypeInfo("percent", Types.DOUBLE),
            TypeInfo("currency", Types.DOUBLE),
            TypeInfo("date", Types.DATE),
            TypeInfo("time", Types.TIME),
            TypeInfo("datetime", Types.TIMESTAMP),
            TypeInfo("picklist", Types.ARRAY),
            TypeInfo("multipicklist", Types.ARRAY),
            TypeInfo("combobox", Types.ARRAY),
            TypeInfo("anyType", Types.OTHER),
        )
    }
}

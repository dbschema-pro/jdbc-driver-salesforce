package com.wisecoders.jdbc.salesforce.io

/**
 * Licensed under [CC BY-ND 4.0 DEED](https://creativecommons.org/licenses/by-nd/4.0/), copyright [Wise Coders GmbH](https://wisecoders.com), used by [DbSchema Database Designer](https://dbschema.com).
 * Code modifications allowed only as pull requests to the [public GIT repository](https://github.com/wise-coders/salesforce-jdbc-driver).
 */
class ForceResultField(
    private val entityType: String?,
    val name: String,
    var value: Any?
) {

    override fun toString(): String {
        return name + "=" + value + (if (entityType != null) " [entityType=$entityType" else "")
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (entityType?.hashCode() ?: 0)
        result = prime * result + (name.hashCode() )
        result = prime * result + (if (value == null) 0 else value.hashCode())
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val _other = other as ForceResultField
        if (entityType == null) {
            if (_other.entityType != null) return false
        } else if (entityType != _other.entityType) return false
        else if (name != _other.name) return false
        return if (value == null) {
            _other.value == null
        } else value == _other.value
    }
}

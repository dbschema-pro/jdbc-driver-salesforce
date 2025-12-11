plugins {
    alias(libs.plugins.wisecoders.commonGradle.jdbcDriver)
}

group = "com.wisecoders.jdbc-drivers"

jdbcDriver {
    dbId = "Salesforce"
}

dependencies {
    implementation(libs.wisecoders.commonLib.commonSlf4j)
    implementation(libs.wisecoders.commonJdbc.commonJdbcJvm)

    implementation(libs.slf4j.api)
    implementation(libs.salesforce.partnerApi)
    implementation(libs.h2)
    implementation(libs.commons.collections)

    runtimeOnly(libs.logback.classic)
}

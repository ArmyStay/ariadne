plugins {
    kotlin("jvm") version "2.4.21"
    id("app.cash.sqldelight") version "2.4.1"
}

kotlin { jvmToolchain(21) }

sqldelight {
    databases {
        create("AriadneDb") {
            packageName.set("com.ariadne.store.db")
            // The schema is the app's, not a copy: it is linked, so it cannot drift.
            srcDirs.setFrom(file("../../core/store/src/commonMain/sqldelight"))
        }
    }
}

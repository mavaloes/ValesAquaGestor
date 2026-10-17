// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // O AGP 9 já traz o Kotlin embutido (KGP 2.2.10). Esta linha usa a versão do catálogo,
        // como indica a nota de versão do AGP 9.0 ("upgrade to a higher KGP version").
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}

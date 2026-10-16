import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.valoes.valesaquagestor"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.valoes.valesaquagestor"
        // specs/01-visao-e-stack.md: minSdk 26 (java.time nativo no domínio).
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Servidor Supabase (decisão 🔶 de specs/11-autenticacao.md). A URL e a chave pública do projeto ficam
        // em local.properties (fora do git): supabase.url=... e supabase.anonKey=... (passo a passo em
        // backend/supabase/README.md). Sem elas, o app usa o FakeAuthRepository (teste@teste.com / Teste1234)
        // e não sincroniza.
        val localProperties = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        val supabaseUrl = localProperties.getProperty("supabase.url", "").trim().trimEnd('/')
        val supabaseAnonKey = localProperties.getProperty("supabase.anonKey", "").trim()
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("boolean", "USE_FAKE_AUTH", (supabaseUrl.isEmpty() || supabaseAnonKey.isEmpty()).toString())
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        // O Robolectric lê os assets do build debug; é de lá que o MigrationTestHelper pega os schemas
        // exportados do Room. Só o debug leva os JSONs; o release não.
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
    testOptions {
        // Robolectric (testes de DAO e repositório com Room em memória, sem emulador).
        unitTests.isIncludeAndroidResources = true
        // Robolectric em JDK 17+ precisa abrir classes internas do JDK (https://robolectric.org/getting-started/).
        unitTests.all {
            it.jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--enable-native-access=ALL-UNNAMED",
            )
        }
    }
}

room {
    // Schema exportado e versionado no git (base dos testes de migração).
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // QR Code: ZXing gera o código do tanque; a tela de leitura usa a câmera (sem serviços do Google, funciona offline).
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    implementation(libs.androidx.print)

    // Sincronização (Fase 09): servidor Supabase por HTTP e envio em segundo plano com tentativa de novo.
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

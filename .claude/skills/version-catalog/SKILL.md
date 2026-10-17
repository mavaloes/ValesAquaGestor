---
name: version-catalog
description: Use ao criar ou editar libs.versions.toml e build.gradle.kts do módulo app.
---

# Version Catalog

1. Consulte a documentação oficial para a versão estável mais recente de cada lib (não use versões de memória).
2. Em `libs.versions.toml`, separe `[versions]`, `[libraries]` e `[plugins]`.
3. No `build.gradle.kts`, use apenas aliases (`libs.xxx`, `alias(libs.plugins.xxx)`).
4. Use `ksp(...)` para Room e Hilt. Nunca `kapt`.
5. Inclua o plugin de Serialization (rotas type-safe).
6. Ao final, liste as versões usadas.
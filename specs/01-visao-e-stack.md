# Visão e Stack — Valões AquaGestor

## Objetivo
App Android nativo para gestão de fazendas de piscicultura.
Uso no campo, à beira do tanque, muitas vezes sem internet.

## Requisito principal
Funcionar 100% offline, com dados salvos localmente.

## Stack obrigatória
- Kotlin (estável mais recente) + Jetpack Compose (Material 3)
- MVVM + Clean Architecture (camadas data, domain, presentation)
- Estado: StateFlow + UiState
- DI: Hilt
- Banco: Room com KSP (não KAPT)
- Navegação: Navigation Compose com rotas type-safe (@Serializable)
- minSdk 26; targetSdk/compileSdk na versão estável mais recente
- Dependências via Version Catalog (libs.versions.toml)
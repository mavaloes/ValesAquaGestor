---
name: room-entity
description: Use ao criar ou alterar Entities, TypeConverters e relacionamentos (@Relation) do Room neste projeto.
---

# Criar Entity do Room

1. Leia `specs/02-dominio.md` para ver campos e relacionamentos.
2. Crie a classe em `data/local/entity/` com `@Entity`, nomes de colunas em português.
3. Use `Long` com `@PrimaryKey(autoGenerate = true)` para o id.
4. Para cada FK: declare `@ForeignKey` com `onDelete` justificado e `@Index` na coluna.
5. Datas como `Long` (epoch millis). Enums como `String` via TypeConverter.
6. Relacionamentos 1—N: crie uma classe `XxxComYyy` com `@Embedded` + `@Relation`.
7. Escreva KDoc em português; nomes de classes/funções em inglês.
8. Rode `./gradlew :app:kspDebugKotlin` para validar.

## Exemplo
`Lote` com FK para `Tanque`: `foreignKeys = [ForeignKey(Tanque::class, ["id"], ["tanqueId"], onDelete = RESTRICT)]`, `indices = [Index("tanqueId")]`.
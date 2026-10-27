package com.ifpr.androidapptemplatear.ui

/** Nomes amigáveis (com acento) para os arquivos .glb conhecidos. */
private val nomesConhecidos = mapOf(
    "morsa_bebe" to "Morsa bebê",
    "leao_marinho" to "Leão-marinho",
    "elefante_marinho" to "Elefante-marinho",
    "foca_idiota" to "Foca idiota",
    "neutrofilo" to "Neutrófilo",
    "sofa" to "Sofá",
    "plane" to "Plano"
)

/** "leao_marinho.glb" -> "Leão-marinho". Arquivos desconhecidos: "meu_modelo.glb" -> "Meu modelo". */
fun String.toModelTitle(): String {
    val base = substringBeforeLast('.').lowercase()
    return nomesConhecidos[base]
        ?: base.replace("_", " ").replaceFirstChar { it.titlecase() }
}

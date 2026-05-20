package com.caiana.talks.domain.model

enum class CefrLevel(val label: String, val description: String) {
    A1(
        "A1 — Iniciante",
        "Você consegue usar expressões básicas e frases simples."
    ),
    A2(
        "A2 — Elementar",
        "Você se comunica em situações cotidianas familiares."
    ),
    B1(
        "B1 — Intermediário",
        "Você lida com a maioria das situações em viagens ao exterior."
    ),
    B2(
        "B2 — Intermediário Avançado",
        "Você interage com falantes nativos com fluência razoável."
    ),
    C1(
        "C1 — Avançado",
        "Você se expressa com fluência e espontaneidade."
    ),
    C2(
        "C2 — Proficiente",
        "Você compreende e se expressa com precisão em situações complexas."
    )
}

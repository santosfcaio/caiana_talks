package com.caiana.talks.domain.model

enum class ConversationTheme(val id: String, val displayLabel: String) {
    RESTAURANT("restaurant", "Restaurantes"),
    AIRPORT("airport", "Aeroportos e viagens"),
    HOTEL("hotel", "Hotéis e hospedagem"),
    JOB_INTERVIEW("job_interview", "Entrevistas de emprego"),
    SHOPPING("shopping", "Compras"),
    TOURISM("tourism", "Turismo e pontos turísticos"),
    HEALTH("health", "Saúde e consultas médicas"),
    WORK_MEETINGS("work_meetings", "Reuniões de trabalho"),
    SOCIAL("social", "Vida social e amizades"),
    TECHNOLOGY("technology", "Tecnologia e gadgets")
}

package it.bbnss.moneta.core.model

/**
 * Tema scelto dall'utente.
 *
 * Sta nel modello e non nel design system perché è una preferenza da
 * persistere, che il livello dati deve poter leggere e scrivere senza tirarsi
 * dietro Compose.
 *
 * [OLED] esiste separatamente da [DARK] perché sui pannelli AMOLED il nero puro
 * spegne i pixel invece di illuminarli: su un'app che si consulta proprio
 * quando la batteria scarseggia, la differenza si sente.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK, OLED }

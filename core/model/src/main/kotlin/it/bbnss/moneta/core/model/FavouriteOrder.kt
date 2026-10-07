package it.bbnss.moneta.core.model

object FavouriteOrder {
    /** Moves one visible row, skipping the base currency hidden by the board. */
    fun move(currencies: List<Currency>, currency: Currency, base: Currency, direction: Int): List<Currency> {
        val visible = currencies.filter { it != base }
        val index = visible.indexOf(currency)
        if (index < 0 || direction == 0) return currencies
        val target = (index + if (direction > 0) 1 else -1).coerceIn(0, visible.lastIndex)
        if (target == index) return currencies
        val neighbour = visible[target]
        val ordered = currencies.toMutableList()
        ordered.remove(currency)
        val position = ordered.indexOf(neighbour) + if (direction > 0) 1 else 0
        ordered.add(position, currency)
        return ordered
    }
}

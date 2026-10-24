package com.andeva.atelier.mobile.features.tasks.domain

/**
 * `HoldReason` de docs/api/README.md §8. BREAK y END_OF_SHIFT son pausas del mecánico (las levanta él con
 * `/start`); el resto es una suspensión (solo la levanta un supervisor con `/resume`, docs/03-traceability.md US29 E3).
 */
enum class HoldReason {
    BREAK, END_OF_SHIFT, DEFECTIVE_PART, WAITING_PARTS, WAITING_FOR_PARTS;

    val isMechanicPause: Boolean get() = this == BREAK || this == END_OF_SHIFT
}

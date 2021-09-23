package io.github.wulkanowy.utils

import io.github.wulkanowy.data.db.entities.Semester
import java.time.LocalDate.now

inline val Semester.isNow: Boolean
    get() = now() in start..end

fun List<Semester>.getCurrentOrLast(): Semester {
    if (isEmpty()) throw RuntimeException("Empty semester list")

    // when there is selected semester in settings
    singleOrNull { it.current }?.let { return it }

    // when there is only one current semester
    singleOrNull { it.isNow }?.let { return it }

    // when there is more than one current semester - find one with higher id
    singleOrNull { semester -> semester.semesterId == maxByOrNull { it.semesterId }?.semesterId }?.let { return it }

    throw IllegalArgumentException("Duplicated last semester! Semesters: ${joinToString(separator = "\n")}")
}

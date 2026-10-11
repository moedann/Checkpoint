package com.example.nanisini

import kotlin.random.Random

class ReviewQuestionSelector(
    private val nextIndex: (Int) -> Int = { size -> Random.nextInt(size) },
) {
    fun select(questions: List<ReviewQuestionModel>, previousId: String?): ReviewQuestionModel? {
        val enabled = questions.filter { it.isEnabled }
        if (enabled.isEmpty()) return null
        val candidates = enabled.filterNot { enabled.size > 1 && it.id == previousId }
        return candidates[nextIndex(candidates.size).coerceIn(0, candidates.lastIndex)]
    }

    fun selectMany(questions: List<ReviewQuestionModel>, count: Int): List<ReviewQuestionModel> =
        questions.filter { it.isEnabled }.shuffled().take(count.coerceAtLeast(0))
}

object ReviewAnswerEvaluator {
    fun isCorrect(input: String, answer: String): Boolean = normalize(input) == normalize(answer)
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ").lowercase()
}

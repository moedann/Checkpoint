package com.example.nanisini

import org.junit.Assert.*
import org.junit.Test

class ReviewQuestionSelectorTest {
    private fun question(id: String, enabled: Boolean = true) = ReviewQuestionModel(id, "book", "question-$id", "answer-$id", 0L, 0L, enabled)

    @Test fun selectsAnEnabledQuestion() {
        val selected = ReviewQuestionSelector { 0 }.select(listOf(question("off", false), question("on")), null)
        assertEquals("on", selected?.id)
    }

    @Test fun avoidsTheImmediatelyPreviousQuestionWhenPossible() {
        val selector = ReviewQuestionSelector { 0 }
        val selected = selector.select(listOf(question("first"), question("second")), "first")
        assertEquals("second", selected?.id)
    }

    @Test fun extensionSelectionGetsANewQuestion() {
        val selector = ReviewQuestionSelector { 0 }
        val questions = listOf(question("initial"), question("extension"))
        val initial = selector.select(questions, null)!!
        val extension = selector.select(questions, initial.id)!!
        assertNotEquals(initial.id, extension.id)
    }

    @Test fun emptyQuestionListFallsBackWithoutBlocking() {
        assertNull(ReviewQuestionSelector { 0 }.select(emptyList(), null))
    }

    @Test fun answerEvaluationTrimsWhitespaceAndIgnoresAsciiCase() {
        assertTrue(ReviewAnswerEvaluator.isCorrect("  Tokyo ", "tokyo"))
        assertFalse(ReviewAnswerEvaluator.isCorrect("大阪", "東京"))
    }
}

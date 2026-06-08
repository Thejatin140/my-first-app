package com.example.data

data class PracticePhrase(
    val id: Int,
    val category: String,
    val level: String,
    val english: String,
    val definition: String, // Context clue or meaning
    val suggestion: String  // Pro pronunciation tip
)

object DialogueData {
    val phrases = listOf(
        // Cafe & Food
        PracticePhrase(
            id = 1,
            category = "☕ Cafe & Diner",
            level = "Beginner",
            english = "I would like to order a large iced coffee, please.",
            definition = "Standard polite way to order coffee.",
            suggestion = "Focus on the liaison between 'would' and 'like'."
        ),
        PracticePhrase(
            id = 2,
            category = "☕ Cafe & Diner",
            level = "Intermediate",
            english = "Could you tell me if this blueberry muffin has dairy in it?",
            definition = "Asking about food allergens politely.",
            suggestion = "Ensure you pronounce the 'l' in 'could' silently - it sounds like 'kud'."
        ),
        PracticePhrase(
            id = 3,
            category = "☕ Cafe & Diner",
            level = "Advanced",
            english = "We'd like to rent a table for four under the balcony, and check if separate checks are permitted.",
            definition = "Making complex dining requests.",
            suggestion = "The contraction 'We'd' sounds like 'weed'. Connect 'balcony' to 'and'."
        ),

        // Travel & Transit
        PracticePhrase(
            id = 4,
            category = "✈️ Airport & Travel",
            level = "Beginner",
            english = "Here is my passport and flight reservation confirmation.",
            definition = "Handing credentials to custom agents.",
            suggestion = "Keep 'passport' flat, do not overemphasize the 'o' sound."
        ),
        PracticePhrase(
            id = 5,
            category = "✈️ Airport & Travel",
            level = "Intermediate",
            english = "I would highly prefer securing a window seat on the left side, if possible.",
            definition = "Requesting custom seating arrangements.",
            suggestion = "Pronounce 'prefer' with emphasis on the second syllable: pre-FER."
        ),
        PracticePhrase(
            id = 6,
            category = "✈️ Airport & Travel",
            level = "Advanced",
            english = "Could you clarify whether my checked bags will automatically transfer to my connection flight?",
            definition = "Asking complex transit luggage questions.",
            suggestion = "The word 'automatically' has a soft 'd' sound for the double 't'."
        ),

        // Job Interview
        PracticePhrase(
            id = 7,
            category = "💼 Career & Office",
            level = "Beginner",
            english = "I am excited about the opportunity to join your creative team.",
            definition = "Expressing professional enthusiasm.",
            suggestion = "Stress the first syllable of 'creative': cre-A-tive."
        ),
        PracticePhrase(
            id = 8,
            category = "💼 Career & Office",
            level = "Intermediate",
            english = "My core strength lies in solving critical engineering bugs under tight deadlines.",
            definition = "Explaining job skills confidently.",
            suggestion = "Connect 'strength lies' and ensure 'under' flow is smooth."
        ),
        PracticePhrase(
            id = 9,
            category = "💼 Career & Office",
            level = "Advanced",
            english = "I excel at driving collaboration across multi-functional stakeholders to deliver key business projects.",
            definition = "Pitching executive management capabilities.",
            suggestion = "Pronounce 'collaboration' steadily. Accent is on 'ra': co-lab-o-RA-tion."
        )
    )
}

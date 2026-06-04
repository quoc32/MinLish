package com.example.minlishapp.ui.screens

sealed class AppRoute(val route: String) {
    object Splash : AppRoute("splash")
    object Welcome : AppRoute("welcome")
    object Login : AppRoute("login")
    object LanguageSelection : AppRoute("language_selection")
    object OnboardingGoals : AppRoute("onboarding_goals")
    object OnboardingDailyWords : AppRoute("onboarding_daily_words")
    object Dashboard : AppRoute("dashboard")
    object VocabDecks : AppRoute("vocab_decks")
    object Flashcards : AppRoute("flashcards")
    object LessonComplete : AppRoute("lesson_complete")
    object Stats : AppRoute("stats")
    object Profile : AppRoute("profile")
    object ResetPassword : AppRoute("reset_password")
    object AiTutor : AppRoute("ai_tutor")

    companion object {
        /** Bottom bar screens */
        val bottomBarRoutes = listOf(
            Dashboard.route,
            VocabDecks.route,
            Stats.route,
            Profile.route
        )
    }
}

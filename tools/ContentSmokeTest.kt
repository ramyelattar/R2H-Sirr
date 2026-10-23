import com.r2h.sirr.core.*
import kotlin.random.Random

/** Run with plain kotlinc/kotlin; not an Android build. */
fun main() {
    check(ExpandedQuizzes.packs.size == 7)
    val allQuestions = ExpandedQuizzes.packs.flatMap { it.questions }
    check(allQuestions.size == 140)
    check(allQuestions.map { it.text }.distinct().size == 140)
    check(allQuestions.all { it.choices.size == 3 })
    var tested = 0
    for (mode in listOf(false, true)) for (level in Intensity.entries) {
        val deck=Challenges.deck(level,mode)
        check(deck.size >= (if(mode)12 else 10))
        check(deck.map {it.text}.distinct().size == deck.size)
        val names=if(mode)listOf("أ","ب","ج","د") else listOf("أ","ب")
        val session=SessionFactory.create(names,level,mode,Random(17))
        check(session.turns.size == (if(mode)12 else 10))
        check(session.turns.map {it.prompt.text}.distinct().size==session.turns.size)
        var game=session
        while(!game.finished) game=game.resolve(completed=true)
        check(game.scores.sum()==session.turns.size)
        check(game.scores.maxOrNull()!!-game.scores.minOrNull()!!<=1)
        tested++
    }
    val custom=listOf("اسأل عن ذكرى خفية مفرحة","اكتب عبارة غزل مميزة من خيالك")
    val session=SessionFactory.create(listOf("أ","ب"),Intensity.CUSTOM,false,Random(10),custom)
    check(custom.all {text->session.turns.any {it.prompt.text==text} })
    check(runCatching { SessionFactory.create(listOf("أ","ب"),Intensity.SOFT,false,Random(1),custom) }.isFailure)
    check(runCatching { SessionFactory.create(listOf("أ","ب"),Intensity.CUSTOM,false,Random(1),listOf("مكرر مضبوط", "مكرر مضبوط")) }.isFailure)
    println("PASS: 140 distinct Arabic questions; 10 intensity/mode game combinations, no repeats; score fairness; custom validation")
}

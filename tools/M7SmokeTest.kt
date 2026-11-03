import com.r2h.sirr.core.*
import kotlin.random.Random

/** Source-only verification; no Android Gradle build. */
fun main() {
    val names=listOf("شريك ١","شريك ٢")
    for (n in listOf(6,10,12,18)) for (mode in listOf(false,true)) {
        val players=if(mode)names+listOf("ضيف ٣","ضيف ٤") else names
        val game=SessionFactory.create(players,Intensity.HOT,mode,Random(17),
            roundCount=n,variety=ChallengeVariety.MIX_WITH_LOWER)
        check(game.turns.size==n)
        check(game.turns.map { it.prompt.text }.distinct().size==n)
        check(game.turns.count { it.prompt.level==Intensity.HOT } >= (n+1)/2)
        val turnsByPlayer=game.turns.groupingBy {it.actorIndex}.eachCount().values
        check(turnsByPlayer.max()-turnsByPlayer.min()<=1)
    }
    val first=SessionFactory.create(names,Intensity.SOFT,false,Random(2),roundCount=6)
    val recent=first.turns.map {it.prompt.text}.toSet()
    val next=SessionFactory.create(names,Intensity.SOFT,false,Random(2),roundCount=6,recentlySeen=recent)
    check(next.turns.none { it.prompt.text in recent })
    check(runCatching { SessionFactory.create(names,Intensity.SOFT,false,roundCount=20) }.isFailure)
    val summary=QuizInsights.summary(listOf(0,1,2,2,1,1,0,2))
    check(summary.counts==listOf(2,3,3))
    check(summary.dominantOption==null)
    check(summary.percentage(0)==25)
    val compare=QuizInsights.compare(listOf(0,1,2,0),listOf(0,0,2,2))
    check(compare.agreement==2 && compare.agreementPercent==50)
    check(runCatching {QuizInsights.summary(listOf(0,3))}.isFailure)
    check(runCatching {QuizInsights.compare(listOf(0),listOf(1,2))}.isFailure)
    println("PASS M7: configurable rounds, balanced difficulty mix, recent challenge avoidance, response summaries")
}

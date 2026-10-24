package com.r2h.sirr.core

import kotlin.random.Random

enum class Intensity(val arabic: String) {
    SOFT("هادئ"), NORMAL("متوازن"), SEXY("جريء"), HOT("أكثر جرأة"), HARD("تحديات متقدمة"), CUSTOM("مخصص")
}

data class Challenge(val id: String, val level: Intensity, val text: String)

/** Independent original Arabic challenges; no imported third-party content. */
object Challenges {
    /** Local and remote use the same canonical Arabic challenge catalog (v2). */
    fun deck(level: Intensity, group: Boolean): List<Challenge> {
        val catalog = if (group) AdultChallengesV2.party else AdultChallengesV2.couple
        val selected = if (level == Intensity.CUSTOM) {
            (catalog.getValue(Intensity.SOFT).take(4) +
             catalog.getValue(Intensity.NORMAL).take(4) +
             catalog.getValue(Intensity.SEXY).take(4) +
             catalog.getValue(Intensity.HOT).take(4) +
             catalog.getValue(Intensity.HARD).take(4))
        } else catalog.getValue(level)
        return selected.mapIndexed { index, prompt ->
            Challenge("${if (group) "party" else "couple"}-${level.name}-${index + 1}", level, prompt)
        }
    }
}

/** Stable round and scoring rules, independent of Compose. */
data class Turn(val roundIndex: Int, val actorIndex: Int, val prompt: Challenge)

data class LocalGame(
    val playerNames: List<String>, val level: Intensity, val group: Boolean,
    val turns: List<Turn>, val round: Int = 0,
    val scores: List<Int> = List(playerNames.size) { 0 },
    val skipped: List<Int> = List(playerNames.size) { 0 },
    val ended: Boolean = false
) {
    val finished: Boolean get() = ended || round >= turns.size
    fun resolve(completed: Boolean): LocalGame {
        require(!finished)
        val current = turns[round]
        val scoresNext = scores.toMutableList()
        val skippedNext = skipped.toMutableList()
        if (completed) scoresNext[current.actorIndex] += 1 else skippedNext[current.actorIndex] += 1
        return copy(round = round + 1, scores = scoresNext, skipped = skippedNext)
    }
    fun leave() = copy(ended = true)
}

enum class ChallengeVariety(val arabic: String) {
    ONE_LEVEL("مستوى واحد"), MIX_WITH_LOWER("تنويع مع المستوى الأقل")
}

object SessionFactory {
    /** No duplicates within a session. Recent challenges are avoided where enough alternatives exist. */
    fun create(names: List<String>, level: Intensity, group: Boolean,
        random: Random = Random.Default, customPrompts: List<String> = emptyList(),
        roundCount: Int = if (group) 12 else 10,
        variety: ChallengeVariety = ChallengeVariety.ONE_LEVEL,
        recentlySeen: Set<String> = emptySet()): LocalGame {
        require(names.size in 2..4 && (group || names.size == 2))
        require(names.all { it.isNotBlank() && it.length <= 32 })
        require(roundCount in 6..18)
        require(customPrompts.size <= 60 && customPrompts.all { it.trim().length in 6..200 })
        require(customPrompts.map { it.trim() }.distinct().size == customPrompts.size)
        require(level == Intensity.CUSTOM || customPrompts.isEmpty())
        val levels = listOf(Intensity.SOFT, Intensity.NORMAL, Intensity.SEXY, Intensity.HOT, Intensity.HARD)
        val customs = customPrompts.mapIndexed { i, prompt ->
            Challenge("custom-${i + 1}", Intensity.CUSTOM, prompt.trim())
        }
        fun prioritize(items: List<Challenge>): List<Challenge> {
            val unique = items.distinctBy { it.text }
            fun freshFirst(pool: List<Challenge>): List<Challenge> {
                val featured = pool.filter {
                    (it.id.substringAfterLast('-').toIntOrNull() ?: 999) in 1..10
                }
                return featured.shuffled(random) + pool.filterNot { it in featured }.shuffled(random)
            }
            return freshFirst(unique.filterNot { it.id in recentlySeen || it.text in recentlySeen }) +
                freshFirst(unique.filter { it.id in recentlySeen || it.text in recentlySeen })
        }
        val selection = when {
            level == Intensity.CUSTOM -> (customs.shuffled(random) +
                prioritize(Challenges.deck(level, group)).filterNot { c -> customs.any { it.text == c.text } })
                .distinctBy { it.text }.take(roundCount)
            variety == ChallengeVariety.ONE_LEVEL -> prioritize(Challenges.deck(level, group)).take(roundCount)
            else -> {
                val primary = prioritize(Challenges.deck(level, group))
                val index = levels.indexOf(level)
                val lower = prioritize(Challenges.deck(levels[(index - 1).coerceAtLeast(0)], group))
                    .filterNot { alt -> primary.any { it.text == alt.text } }
                val required = (roundCount + 1) / 2
                val top = primary.take(required)
                val remainder = (lower + primary.drop(required)).distinctBy { it.text }
                (top + remainder.take(roundCount - required))
            }
        }
        require(selection.size == roundCount) { "لا توجد تحديات كافية لجلسة بلا تكرار" }
        val deck = selection.shuffled(random)
        val start = random.nextInt(names.size)
        val turns = deck.mapIndexed { i, prompt -> Turn(i, (start + i) % names.size, prompt) }
        return LocalGame(names, level, group, turns)
    }
}

/** Distinct customized dice with reproducible, validated probabilistic selections. */
data class DieFace(val text: String, val weight: Int = 0, val imageId: String? = null)
data class DiceConfiguration(val dice: List<List<DieFace>>, val durationSeconds: Int = 7, val sound: Boolean = false) {
    fun validate() {
        require(dice.size == 3 && dice.all { die -> die.size == 6 && die.sumOf { it.weight } == 100 &&
            die.all { it.weight >= 0 && it.text.length <= 72 } })
        require(durationSeconds in listOf(2, 3, 5, 7, 10, 13, 20))
    }
    fun roll(pick: (Int) -> Int = { Random.nextInt(it) }): List<Int> {
        validate()
        return dice.map { die -> GameLogic.weightedFace(die.map { it.weight }, pick) }
    }
}

object DiceCatalog {
    private val base = listOf(
        listOf("قل", "اسأل", "اقترح", "اختر", "اعترف", "شارك"),
        listOf("مجاملة", "أمنية", "ذكرى", "رسالة", "سؤالًا", "فكرة"),
        listOf("بهدوء", "بصراحة", "بابتسامة", "في دقيقة", "بثلاث كلمات", "الآن")
    )
    private val variations = mapOf(
        Intensity.SOFT to base,
        Intensity.NORMAL to listOf(base[0], listOf("ذكرى", "تفضيلًا", "مجاملة", "رغبة", "سرًا صغيرًا", "سؤالًا"), base[2]),
        Intensity.SEXY to listOf(listOf("غازل", "صارح", "همس", "اكتب", "اسأل", "اختر"), listOf("إطراء", "رسالة حب", "تفضيلًا", "لحظة قرب", "فكرة رومانسية", "أمنية"), base[2]),
        Intensity.HOT to listOf(listOf("صارح", "اسأل", "اقترح", "غازل", "تبادل", "صف"), listOf("حدًا شخصيًا", "تفضيلًا خاصًا", "سؤالًا جريئًا", "رسالة سرية", "موقف انجذاب", "فكرة جديدة"), base[2]),
        Intensity.HARD to listOf(listOf("ناقش", "اسأل", "شارك", "أفصح", "اختر", "اكتب"), listOf("حدودك", "توقعاتك", "تفضيلًا", "فكرة حميمة", "كلمة أمان", "رسالة خاصة"), base[2]),
        Intensity.CUSTOM to base
    )
    fun preset(intensity: Intensity) = DiceConfiguration(variations.getValue(intensity).map { labels ->
        labels.mapIndexed { index, label -> DieFace(label, GameLogic.evenWeights[index]) }
    })
}

/** One-time view is an application-side policy, not prevention of external recording. */
data class EphemeralEnvelope(val createdAtMillis: Long, val expiresAtMillis: Long, val viewed: Boolean = false) {
    fun available(nowMillis: Long) = !viewed && nowMillis >= createdAtMillis && nowMillis < expiresAtMillis
    fun consume(nowMillis: Long): EphemeralEnvelope {
        require(available(nowMillis))
        return copy(viewed = true)
    }
}

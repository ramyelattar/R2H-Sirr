package com.r2h.sirr.core

import kotlin.random.Random

/** Pure functions: no Android dependencies. All content is original Arabic text. */
object GameLogic {
    val evenWeights = listOf(17, 17, 17, 17, 16, 16)

    /** Index 0..5, with strict distribution validation to prevent hidden bias. */
    fun weightedFace(weights: List<Int>, pick: (Int) -> Int = { Random.nextInt(it) }): Int {
        require(weights.size == 6 && weights.all { it >= 0 } && weights.sum() == 100) {
            "Probabilities must be six nonnegative percentages totaling 100"
        }
        val randomValue = pick(100)
        require(randomValue in 0 until 100)
        var sum = 0
        weights.forEachIndexed { index, weight ->
            sum += weight
            if (randomValue < sum) return index
        }
        error("Invalid weighted selection")
    }

    fun score(answers: List<Int>): Int {
        require(answers.all { it in 0..2 })
        return answers.sum()
    }

    fun interpretation(score: Int, max: Int): String {
        require(max > 0 && score in 0..max)
        return when {
            score * 3 < max -> "تفضّل الهدوء والتدرّج وتقدّر الحدود الواضحة."
            score * 3 < max * 2 -> "تحب الموازنة بين التجديد والألفة والتفاهم."
            else -> "تميل إلى المبادرة والتجديد ضمن اتفاق واضح ومتبادل."
        }
    }
}

data class Question(val text: String, val choices: List<String>) {
    init { require(choices.size == 3) }
}

data class Quiz(val name: String, val questions: List<Question>)

/** Archived pre-v4 sample data. Retained for history, not read by current game screens. */
@Deprecated("Legacy sample content; use ExpandedQuizzes.packs, Challenges.deck and DiceCatalog.preset.")
object OriginalContent {
    val quizPacks = listOf(
        Quiz("الهوية والمشاعر", listOf(
            Question("عندما تحتاج إلى وقت لنفسك، كيف توضح ذلك؟", listOf("أتردد في الحديث", "ألمّح بهدوء", "أعبّر بوضوح")),
            Question("كيف تحب بدء حوار شخصي؟", listOf("أنتظر المبادرة", "أتدرّج في الحديث", "أفتح الموضوع مباشرة")),
            Question("إذا اختلفت مع شخص مقرّب، ماذا تفعل؟", listOf("أنسحب قليلًا", "أناقش بعد الهدوء", "أتحدث فورًا باحترام"))
        )),
        Quiz("الحب والارتباط", listOf(
            Question("ما أكثر ما يعزز شعورك بالقرب؟", listOf("المواقف الهادئة", "وقت مشترك", "مبادرات متجددة")),
            Question("كيف تعبر عن الاهتمام؟", listOf("بالاستماع", "بمساعدة الطرف الآخر", "بالمبادرة بلفتة خاصة")),
            Question("عند التخطيط لأمسية، تفضل...", listOf("روتينًا مريحًا", "خطة بسيطة", "فكرة غير متوقعة"))
        )),
        Quiz("الانسجام والتوافق", listOf(
            Question("عند اختلاف التفضيلات، كيف تتصرف؟", listOf("أؤجل القرار", "نختار حلًا وسطًا", "أقترح تجربة متفقًا عليها")),
            Question("قبل مشاركة نشاط جديد، هل...", listOf("أحتاج وقتًا", "أتبادل الآراء", "أبادر بسؤال الحدود")),
            Question("ما مدى أهمية الاتفاق المسبق؟", listOf("أفضل العفوية", "يعتمد على النشاط", "ضروري للشعور بالراحة"))
        )),
        Quiz("الجاذبية والمبادرة", listOf(
            Question("كيف ترى المبادرة في العلاقة؟", listOf("أفضل أن تأتي من الآخر", "نتبادلها", "أحب المبادرة أحيانًا")),
            Question("ما أسلوب المجاملة الأقرب لك؟", listOf("كلمات قصيرة", "مواقف لطيفة", "رسالة شخصية مبتكرة")),
            Question("كيف تتعامل مع فكرة جديدة؟", listOf("أفكر قبل الموافقة", "أنا أشاور الطرف الآخر", "أقترح حدودها بوضوح"))
        )),
        Quiz("الرغبات والتفضيلات", listOf(
            Question("عند مشاركة تفضيل خاص، تفضل...", listOf("الكتابة", "محادثة هادئة", "حديثًا مباشرًا وواضحًا")),
            Question("ما دور التجديد بالنسبة لك؟", listOf("أحب الاعتياد", "أحب بعض التنوع", "أحب التجربة المتفق عليها")),
            Question("كيف تتأكد من راحة الطرف الآخر؟", listOf("أراقب الإشارات", "أسأل أحيانًا", "أسأل بوضوح وأحترم الإجابة"))
        )),
        Quiz("الحدود والثقة", listOf(
            Question("إذا لم ترتح لتحدٍّ ما، ماذا تفعل؟", listOf("أتجاوز الأمر بصمت", "أقترح بديلًا", "أوقفه وأوضح السبب")),
            Question("كيف تحدد حدودك الخاصة؟", listOf("أحتاج لاكتشافها", "أذكر الأساسيات", "أعبّر عنها صراحة")),
            Question("إذا غيّر الطرف الآخر رأيه؟", listOf("أستفسر عمّا حدث", "أبحث عن بديل", "أحترم الرفض فورًا"))
        )),
        Quiz("الجرأة والتفاهم", listOf(
            Question("ما أكثر ما يشجعك على تجربة لعبة للكبار؟", listOf("الفضول", "التواصل", "الثقة والاتفاق")),
            Question("في التحديات المبتكرة تفضل...", listOf("مستوى هادئًا", "مستوى متوسطًا", "تدرجًا يختاره الطرفان")),
            Question("كيف تفضّل إنهاء اللعبة؟", listOf("بحديث هادئ", "بمراجعة ممتعة", "بتبادل الانطباعات والحدود"))
        ))
    )

    val couplePrompts = listOf(
        "اختر عبارة تقدير صادقة وقلها للطرف الآخر.",
        "اسأل شريكك عن أكثر موقف مشترك أسعده.",
        "شارك أمنية بسيطة لأمسية قادمة.",
        "دع الطرف الآخر يختار أغنية معبّرة عن هذه اللحظة.",
        "اذكر ثلاث صفات تعجبك في الطرف الآخر.",
        "قدّم سؤالًا جديدًا تحب سماع إجابته.",
        "اتفقا على نشاط لطيف خارج الروتين.",
        "صف ذكرى مشتركة بثلاث كلمات.",
        "قل شيئًا تتمنى أن تفعلاه معًا مستقبلًا.",
        "استمع لدقيقة كاملة دون مقاطعة إلى قصة يختارها الآخر.",
        "اقترح مفاجأة بسيطة واسمح للطرف الآخر برفضها.",
        "اختارا كلمة سر مرحة تخص هذه الجلسة."
    )

    val partyPrompts = listOf(
        "شارك موقفًا مضحكًا لا يحرج أحدًا.",
        "اختر لاعبًا لتبادل سؤالين اختياريين.",
        "تحدّث عن أكثر مغامرة ممتعة خضتها.",
        "اطرح سؤالًا خفيفًا واترك حق الامتناع للجميع.",
        "صف أمسية مثالية بكلمتين.",
        "اختر لاعبًا ليقترح تحديًا بديلًا آمنًا.",
        "قل عبارة مجاملة لشخص تختاره.",
        "دع المجموعة تختار موضوع حديث لدقيقة.",
        "شارك عادة محببة لديك.",
        "احكِ عن أفضل مفاجأة تلقيتها.",
        "اختر شخصًا ليشارك نصيحة في التواصل.",
        "ليصف كل مشارك مزاجه بكلمة واحدة."
    )

    val diceTemplates = listOf(
        listOf("قل", "اسأل", "اختر", "تذكر", "اقترح", "شارك"),
        listOf("كلمة لطيفة", "ذكرى مميزة", "أمنية", "سؤالًا", "مجاملة", "فكرة جديدة"),
        listOf("الآن", "بهدوء", "بابتسامة", "خلال دقيقة", "بثلاث كلمات", "بصراحة"),
    )
}

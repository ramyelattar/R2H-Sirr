package com.r2h.sirr.core

/**
 * Original, offline, non-graphic couple activity pack.
 *
 * This is a separate local-session catalog. It never silently changes the
 * shared legacy/remote JSON protocol or the historical party game.
 * Personal, partner-authored activities remain available via CUSTOM mode.
 */
enum class CoupleChallengeKind(val arabic: String) {
    TALK("كلام بينكم"),
    WHISPER("همسة"),
    CHOICE("اختيار"),
    CLOSENESS("قرب"),
    TOGETHER("سوا"),
    SURPRISE("مفاجأة"),
    PERSONAL("من عندكم")
}

data class CoupleActivity(
    val id: String,
    val level: Intensity,
    val kind: CoupleChallengeKind,
    val title: String,
    val instruction: String,
    val seconds: Int = 300
) {
    init {
        require(id.isNotBlank() && title.isNotBlank() && instruction.isNotBlank())
        require(seconds in 30..300)
    }
    fun toChallenge(): Challenge = Challenge(
        id = id, level = level, text = instruction, title = title,
        kind = kind, durationSeconds = seconds
    )
}

object CoupleChallengeCatalog {
    private fun list(level: Intensity, entries: List<Triple<CoupleChallengeKind, String, String>>): List<CoupleActivity> =
        entries.mapIndexed { i, (kind, title, instruction) ->
            CoupleActivity("couple2-${level.name}-${i + 1}", level, kind, title, instruction,
                seconds = if (level == Intensity.HOT && i == 0) 300 else when (kind) {
                    CoupleChallengeKind.TALK, CoupleChallengeKind.TOGETHER -> 180
                    CoupleChallengeKind.WHISPER -> 90
                    CoupleChallengeKind.CHOICE, CoupleChallengeKind.CLOSENESS,
                    CoupleChallengeKind.SURPRISE -> 120
                    CoupleChallengeKind.PERSONAL -> 300
                })
        }

    private val soft = list(Intensity.SOFT, listOf(
        Triple(CoupleChallengeKind.CLOSENESS,"نَفَس قريب","قربوا من بعض لحد ما تحسوا بنَفَس بعض. بصّوا في عيون بعض عشر ثواني، وكل واحد يقول كلمة حلوة من قلبه."),
        Triple(CoupleChallengeKind.WHISPER,"كلمة ليك","اهمس في ودن شريكك بحاجة بتحبها فيه ومقلتهاش من فترة. خليه يرد بكلمة واحدة."),
        Triple(CoupleChallengeKind.TOGETHER,"دقيقة هدوء","امسكوا إيدين بعض واقعدوا دقيقة هادية من غير موبايل. بعدين احكوا إيه اللي حسيتوا بيه."),
        Triple(CoupleChallengeKind.CHOICE,"على مزاجك","سيب شريكك يختار: أغنية هادية ترقصوا عليها، ولا حضن دافي لو الاتنين حابين."),
        Triple(CoupleChallengeKind.SURPRISE,"ابتسامة مخصوص","اعمل حركة صغيرة تضحّك شريكك. لو ضحك، خليه يختارلك رد لطيف."),
        Triple(CoupleChallengeKind.TALK,"أحلى حاجة","كل واحد يحكي للتاني عن أحلى حاجة حصلت بينكم الأسبوع ده، مهما كانت بسيطة."),
        Triple(CoupleChallengeKind.CLOSENESS,"نظرة طويلة","اقعدوا قصاد بعض وخلّوا الكلام بالنظرات لمدة نص دقيقة. بعدها قولوا إيه اللي لفتكم."),
        Triple(CoupleChallengeKind.WHISPER,"بصوت واطي","قرب من ودن شريكك وقوله ليه وجوده جنبك دلوقتي مريحك."),
        Triple(CoupleChallengeKind.TOGETHER,"خطوة واحدة","اتفقوا على حاجة بسيطة تعملوها سوا بكرة علشان تفضلوا قريبين."),
        Triple(CoupleChallengeKind.CHOICE,"اختار المكان","شريكك يختار ركن مريح تقعدوا فيه سوا لمدة دقيقتين وتتكلموا براحتكم."),
        Triple(CoupleChallengeKind.SURPRISE,"هدية من غير فلوس","فاجئ شريكك بجملة تقدير مخصوصة ليه، مش جملة محفوظة."),
        Triple(CoupleChallengeKind.TALK,"فاكر اللحظة؟","قول لشريكك موقف صغير خلاك تحس إنه سند ليك، واطلب منه يحكي موقف شبهه."),
        Triple(CoupleChallengeKind.CLOSENESS,"إيد في إيد","لو الاتنين موافقين، شبكوا إيدينكم شوية واتكلموا عن أكتر حاجة بتطمنكم."),
        Triple(CoupleChallengeKind.WHISPER,"سر بسيط","قول لشريكك في ودنه صفة فيه بتخليك تبتسم كل ما تفتكرها."),
        Triple(CoupleChallengeKind.TOGETHER,"دورك ودوري","كل واحد يقول للتاني حاجة عايز يعملها معاه في وقت هادي من غير أي مسؤوليات."),
        Triple(CoupleChallengeKind.CHOICE,"ضحكة ولا كلام","سيب شريكك يختار: تحكيله موقف مضحك ولا تقولّه مجاملة ما سمعهاش قبل كده."),
        Triple(CoupleChallengeKind.SURPRISE,"من غير مقدمات","فاجئ شريكك بكلمتين تقدير، وبعدها اسمع منه رد من غير مقاطعة."),
        Triple(CoupleChallengeKind.TALK,"سؤال خاص بينكم","اسأل شريكك عن حاجة بتفرّحه في قعدتكم سوا. اسمع الإجابة وكملوا فيها."),
        Triple(CoupleChallengeKind.CLOSENESS,"قرب براحة","اقربوا واقعدوا جنب بعض، وكل واحد يحدد بنفسه القرب اللي يريحه."),
        Triple(CoupleChallengeKind.TOGETHER,"وعد صغير","اتفقوا على عادة لطيفة لمدة أسبوع، واختاروا إزاي تفكّروا بعض بيها.")
    ))

    private val normal = list(Intensity.NORMAL, listOf(
        Triple(CoupleChallengeKind.WHISPER,"أنا وحشتني","اهمس لشريكك بإيه أكتر حاجة وحشتك في قعدتكم لوحدكم، وخليه يختار يرد إزاي."),
        Triple(CoupleChallengeKind.CLOSENESS,"أقرب شوية","اقربوا من بعض براحتكم، وكل واحد يقول للتاني الحركة البسيطة اللي بتطمنه."),
        Triple(CoupleChallengeKind.CHOICE,"اختيار الشريك","شريكك يختار: بوسة رقيقة أو حضن دافي أو كلام غزل، بشرط موافقة الاتنين."),
        Triple(CoupleChallengeKind.TALK,"صراحة حلوة","قول لشريكك حاجة فيه بتلفت نظرك حتى بعد سنين من معرفتكم ببعض."),
        Triple(CoupleChallengeKind.SURPRISE,"مجاملة مش متوقعة","فاجئ شريكك بمجاملة عن تفصيلة صغيرة فيه، وسيبه يقولك رد فعله."),
        Triple(CoupleChallengeKind.TOGETHER,"الليلة لينا","اختاروا سوا حاجة بسيطة تخلي قعدتكم الليلة مختلفة عن المعتاد."),
        Triple(CoupleChallengeKind.WHISPER,"في ودنك","قول لشريكك بصوت واطي إيه أكتر لحظة بتشتاق فيها لقربه."),
        Triple(CoupleChallengeKind.CLOSENESS,"نظرة وابتسامة","بص لشريكك مدة أطول من المعتاد وابتسم. لو حابب، قرب منه وامسك إيده."),
        Triple(CoupleChallengeKind.CHOICE,"أول خطوة","خلي شريكك يختار مين يبدأ بكلمة غزل، ومين يرد عليها بإجابة صريحة."),
        Triple(CoupleChallengeKind.TALK,"أحلى عادة","كل واحد يقول للتاني عادة صغيرة عنده بتخليه يحس إنه مرغوب ومهم."),
        Triple(CoupleChallengeKind.SURPRISE,"اعتراف لطيف","افتح كلام مع شريكك بجملة: «أنا لسه بتكسف أقولك...» وكمل بحاجة حقيقية."),
        Triple(CoupleChallengeKind.TOGETHER,"دقيقتين لينا","اقفلوا أي مشتتات واقعدوا جنب بعض دقيقتين، وتكلموا عن اللي نفسكم فيه الليلة."),
        Triple(CoupleChallengeKind.WHISPER,"بلاش استعجال","اهمس: «أنا مستمتع إني جنبك، ومش مستعجل على حاجة». واسأل شريكك عن إحساسه."),
        Triple(CoupleChallengeKind.CLOSENESS,"لمسة إيد","لو الاتنين موافقين، امسك إيد شريكك بلطف واسأله إيه اللفتة اللي بيحبها."),
        Triple(CoupleChallengeKind.CHOICE,"المفاجأة منك","سيب شريكك يختار نشاط رومانسي بسيط تعملوه سوا خلال الخمس دقايق."),
        Triple(CoupleChallengeKind.TALK,"إحنا اتغيرنا إزاي","كل واحد يقول للتاني حاجة حلوة اتغيرت في علاقتكم مع الوقت."),
        Triple(CoupleChallengeKind.SURPRISE,"من قلبك","قول لشريكك كلام غزل من تأليفك في عشرين ثانية، من غير اقتباسات جاهزة."),
        Triple(CoupleChallengeKind.TOGETHER,"خطوة جديدة","اختاروا مع بعض طريقة جديدة لطيفة تقضوا بيها وقت خاص الأسبوع ده."),
        Triple(CoupleChallengeKind.WHISPER,"إشارة بينكم","اتفقوا على كلمة سر لطيفة معناها «محتاج أقعد معاك شوية لوحدنا»."),
        Triple(CoupleChallengeKind.CHOICE,"مين يبدأ","خلي شريكك يختار مين يحكي أول اعتراف رومانسي، والتاني يسمع للآخر.")
    ))

    private val sexy = list(Intensity.SEXY, listOf(
        Triple(CoupleChallengeKind.CLOSENESS,"قرب في هدوء","لو الاتنين مرتاحين، اقربوا من بعض وخلي أول خطوة حضن أو بوسة يختارها شريكك."),
        Triple(CoupleChallengeKind.WHISPER,"مش قادر أخبي","اهمس لشريكك: «وحشني قربك، ونفسي الليلة نخصص وقت لينا إحنا الاتنين»."),
        Triple(CoupleChallengeKind.CHOICE,"سيبله الاختيار","اسأل شريكك يفضل حضن طويل، ولا بوسة هادية، ولا كلام غزل قريب."),
        Triple(CoupleChallengeKind.TALK,"إيه بيشدك فيا","كل واحد يقول للتاني إيه أكتر حاجة بتشدّه في شريكه، من غير إجابات محفوظة."),
        Triple(CoupleChallengeKind.SURPRISE,"دور الغزل","قول لشريكك 3 جمل غزل مختلفين، وخليه يختار أكتر واحدة عجبتّه."),
        Triple(CoupleChallengeKind.TOGETHER,"لحظة من غير كلام","اتفقوا على دقيقة قرب وهدوء، من غير كلام، وبالمسافة اللي تريحكم."),
        Triple(CoupleChallengeKind.WHISPER,"بكل جرأة","قول لشريكك في ودنه أكتر كلمة غزل نفسك يسمعها منك الليلة."),
        Triple(CoupleChallengeKind.CLOSENESS,"بوسة باختيارك","لو الاتنين حابين، شريكك يختار بين بوسة على الخد أو الجبهة أو إيديه."),
        Triple(CoupleChallengeKind.CHOICE,"إنت تختار","شريكك يختار مبادرة لطيفة من عنده، وإنت تقول بصراحة موافق عليها ولا لأ."),
        Triple(CoupleChallengeKind.TALK,"من غير كسوف","اتكلم مع شريكك عن حاجة بسيطة بتزود إحساسك بالقرب منه."),
        Triple(CoupleChallengeKind.SURPRISE,"جملة في ودنك","حضّر جملة غزل خاصة بشريكك، وقولها بصوت واطي من غير ما تضحك."),
        Triple(CoupleChallengeKind.TOGETHER,"اختيار واحد","كل واحد يختار حاجة رومانسية بسيطة يحب يعملها والتاني يختار واحدة منهم."),
        Triple(CoupleChallengeKind.WHISPER,"المرة الجاية","اهمس لشريكك بفكرة لطيفة لنشاط خاص بينكم في يوم مختلف."),
        Triple(CoupleChallengeKind.CLOSENESS,"قرب على مهل","اقعد جنب شريكك وخليه يختار المسافة والوتيرة اللي تريحه."),
        Triple(CoupleChallengeKind.CHOICE,"دلّعني بكلمة","خلي شريكك يختار كلمة غزل معينة، وإنت حطها في جملة من قلبك."),
        Triple(CoupleChallengeKind.TALK,"أكتر وقت","اسأل شريكك عن أكتر وقت بيحب فيه يكون قريب منك وليه."),
        Triple(CoupleChallengeKind.SURPRISE,"من غير توقع","فاجئ شريكك بمبادرة رومانسية غير متوقعة، بعد ما تتأكد إنه مرتاح."),
        Triple(CoupleChallengeKind.TOGETHER,"إنت وأنا","اختاروا لحظة من اللي فاتت نفسكم تعيشوها تاني، وقولوا ليه."),
        Triple(CoupleChallengeKind.WHISPER,"عايز أقولك","قول لشريكك: «بحب وجودك قريب مني، وعايز أسمع منك اللي بيريّحك»."),
        Triple(CoupleChallengeKind.CHOICE,"تبديل الأدوار","مرة إنت تقول جملة غزل، ومرة شريكك يختار جملة ترد بيها.")
    ))

    private val hot = list(Intensity.HOT, listOf(
        Triple(CoupleChallengeKind.WHISPER,"الليلة لينا إحنا الاتنين","قربوا من بعض لحد ما تحسوا بنَفَس بعض. اهمس في ودن شريكك: «أنا الليلة هنا علشان أبسطك وأخليك مبسوط معايا. قولّي نفسك في إيه، وإيه اللي تحب نعمله سوا؟». شريكك يختار طلب واحد، ولو الاتنين موافقين عليه، نفذوه خلال خمس دقايق."),
        Triple(CoupleChallengeKind.CHOICE,"على اختيارك","شريكك يختار نوع القرب اللي بيريحه دلوقتي: حضن طويل، بوسة، أو كلام خاص بينكم."),
        Triple(CoupleChallengeKind.CLOSENESS,"خطوة على مهل","اقربوا من بعض واتفقوا على خطوة رومانسية واحدة تبدأوا بيها، من غير استعجال."),
        Triple(CoupleChallengeKind.TALK,"رغبة في القرب","كل واحد يقول للتاني بصراحة إيه اللي يخليه يحس إنه مرغوب ومحبوب الليلة."),
        Triple(CoupleChallengeKind.SURPRISE,"مفاجأة الشريك","خلي شريكك يختار مفاجأة رومانسية بسيطة يحبها، ونفذوها لو الاتنين موافقين."),
        Triple(CoupleChallengeKind.TOGETHER,"إشارة سرية","اتفقوا على إشارة معناها «أنا عايز وقت خاص معاك». جربوها دلوقتي وشوفوا رد بعض."),
        Triple(CoupleChallengeKind.WHISPER,"قريب منك","اهمس لشريكك: «أنا مبسوط إني معاك لوحدنا. قلّي إيه أحلى بداية على مزاجك»."),
        Triple(CoupleChallengeKind.CLOSENESS,"قعدة أقرب","اختاروا وضع مريح للقعدة جنب بعض، وقولوا لبعض إيه اللي بيعجبكم في اللحظة."),
        Triple(CoupleChallengeKind.CHOICE,"اختياران","اعرض على شريكك اختيارين لطيفين تقرّبوا بيهم من بعض، وخليه يختار براحة."),
        Triple(CoupleChallengeKind.TALK,"صراحة من القلب","قول لشريكك حاجة جريئة عن مشاعرك كنت بتتردد تقولها، من غير أي ضغط عليه."),
        Triple(CoupleChallengeKind.SURPRISE,"حكاية جديدة","ابدأوا الليلة بلحظة خاصة غير المعتادة: كلمة غزل، أو موسيقى، أو قرب على مزاجكم."),
        Triple(CoupleChallengeKind.TOGETHER,"خطوة مشتركة","اختاروا مع بعض فعل رومانسي واحد، وحددوا وقت قصير تنفذوه فيه."),
        Triple(CoupleChallengeKind.WHISPER,"قولي اللي تحبيه","اهمس لشريكك: «اختار اللي يريحك وإحنا سوا، وأنا هقولك لو مرتاح له»."),
        Triple(CoupleChallengeKind.CLOSENESS,"دقيقة لبعض","لو الاتنين حابين، اقربوا من بعض في حضن هادي وخدوا دقيقة من غير كلام."),
        Triple(CoupleChallengeKind.CHOICE,"مين له القرار","خلي شريكك يختار بين جملة غزل أو حضن أو بوسة، وخدوا قراركم سوا."),
        Triple(CoupleChallengeKind.TALK,"اعتراف خفيف","اتكلم عن حركة بسيطة من شريكك بتخليك تشتاق لقربه."),
        Triple(CoupleChallengeKind.SURPRISE,"مختلف عن كل مرة","فاجئ شريكك باقتراح رومانسي جديد قابل للتنفيذ دلوقتي، واسمع رأيه."),
        Triple(CoupleChallengeKind.TOGETHER,"ليلتنا بطريقتنا","اختاروا أغنية خاصة بيكم، واقعدوا قريب من بعض وإنتوا بتسمعوها."),
        Triple(CoupleChallengeKind.WHISPER,"من غير خجل","قول لشريكك بصوت واطي إيه اللي بتحبه في لحظة القرب بينكم."),
        Triple(CoupleChallengeKind.CHOICE,"إنت البداية","سيب شريكك يختار أول مبادرة، وإنت اختار المبادرة اللي بعدها بالاتفاق.")
    ))

    private val hard = list(Intensity.HARD, listOf(
        Triple(CoupleChallengeKind.CHOICE,"مساحة لجرأتنا","كل واحد يختار فكرة جريئة من عنده لقعدتكم الخاصة، وبعدين اتفقوا على اللي يناسبكم."),
        Triple(CoupleChallengeKind.WHISPER,"من غير لف","اهمس لشريكك بحاجة نفسك تطلبها منه، بصراحة ومن غير ما تعتبر الكلام موافقة مسبقة."),
        Triple(CoupleChallengeKind.TALK,"أنا بحب إيه","كل واحد يقول للتاني بوضوح أكتر نوع اهتمام بيحبه في وقتكم الخاص."),
        Triple(CoupleChallengeKind.CLOSENESS,"إشارة القرب","اتفقوا على إشارة للقرب وإشارة للتوقف. جرّبوا تستخدموهم في لحظة رومانسية هادية."),
        Triple(CoupleChallengeKind.SURPRISE,"اختيار مش متوقع","اعرض اقتراحين رومانسيين مختلفين على شريكك، وخليه يختار أو يقترح غيرهم."),
        Triple(CoupleChallengeKind.TOGETHER,"تفاهم من غير حرج","قولوا لبعض حاجة جديدة نفسكم تجربوها سوا، واتفقوا على حدودها لو حبيتوا."),
        Triple(CoupleChallengeKind.WHISPER,"قرب أكتر","قول لشريكك بصوت واطي إيه اللي نفسك تحس بيه وإنت جنبه دلوقتي."),
        Triple(CoupleChallengeKind.CHOICE,"القرار بين إيديكم","خلي شريكك يقترح فكرة خاصة بيكم، وقرروا سوا إذا تحبوا تجربوها النهارده."),
        Triple(CoupleChallengeKind.TALK,"ليلة مختلفة","احكوا لبعض عن أكتر تفصيلة بتخلي قعدتكم الخاصة مميزة عن أي وقت تاني."),
        Triple(CoupleChallengeKind.CLOSENESS,"على راحتنا","خدوا وقتكم في قرب رومانسي متبادل، وكل طرف يختار اللي يريحه من غير استعجال."),
        Triple(CoupleChallengeKind.SURPRISE,"مفاجأة برضاك","حضّر مبادرة رومانسية مفاجئة، واسأل شريكك قبل ما تبدأها."),
        Triple(CoupleChallengeKind.TOGETHER,"اختاروا سوا","اتفقوا على نشاط واحد مختلف لقعدتكم دلوقتي، وخلّوا الاختيار للاتنين."),
        Triple(CoupleChallengeKind.WHISPER,"اللي مقلتهوش","قول لشريكك كلام غزل صريح عن إحساسك بيه، من غير ما تستنى رد معين."),
        Triple(CoupleChallengeKind.CHOICE,"جرأة اختيارية","كل واحد يختار تحدي من أفكاره الخاصة، والتاني يقدر يقبله أو يقترح بديل."),
        Triple(CoupleChallengeKind.TALK,"بنتكلم بوضوح","اسأل شريكك عن تفضيل خاص بيه في وقتكم سوا، واسمعه من غير حكم."),
        Triple(CoupleChallengeKind.CLOSENESS,"قرب محسوب","خدوا لحظة قريبة من بعض بالطريقة اللي توافقوا عليها، ومن حقكم توقفوا في أي وقت."),
        Triple(CoupleChallengeKind.SURPRISE,"فكرة الليلة","كل طرف يقترح مفاجأة رومانسية قابلة للتنفيذ في خمس دقايق، واختاروا واحدة."),
        Triple(CoupleChallengeKind.TOGETHER,"من غير روتين","بدّلوا عادة صغيرة في قعدتكم الخاصة النهارده بحاجة جديدة اخترتوها سوا."),
        Triple(CoupleChallengeKind.WHISPER,"ده اللي عايزه","اهمس لشريكك بجملة مباشرة عن رغبتك في وقت خاص معاه، وخليه يقول رأيه."),
        Triple(CoupleChallengeKind.CHOICE,"خمس دقايق لينا","شريكك يختار نشاط حميمي متفق عليه لمدة تصل لخمس دقايق، والتاني يقدر يوافق أو يختار بديل.")
    ))

    private val all = mapOf(
        Intensity.SOFT to soft,
        Intensity.NORMAL to normal,
        Intensity.SEXY to sexy,
        Intensity.HOT to hot,
        Intensity.HARD to hard
    )

    fun deck(level: Intensity): List<Challenge> =
        if (level == Intensity.CUSTOM) emptyList()
        else all.getValue(level).map { it.toChallenge() }

    fun counts(): Map<Intensity, Int> = all.mapValues { it.value.size }
}

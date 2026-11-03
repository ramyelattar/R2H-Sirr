package com.r2h.sirr

import com.r2h.sirr.core.Challenge
import com.r2h.sirr.core.Intensity
import com.r2h.sirr.core.LocalGame
import com.r2h.sirr.core.Turn
import com.r2h.sirr.core.CoupleCompetition
import com.r2h.sirr.core.CoupleOutcome
import com.r2h.sirr.core.CoupleTieBreak
import com.r2h.sirr.core.KnowledgePrompt
import com.r2h.sirr.core.TieStage
import org.json.JSONArray
import org.json.JSONObject

/** Full local game stays inside the encrypted Keystore-backed vault, not a Bundle. */
internal object LocalSessionStore {
    fun save(vault:LocalVault,name:String,game:LocalGame) {
        require(game.turns.size in 6..18 && game.round in 0..game.turns.size)
        val json=JSONObject().put("version",if(game.competition==null) 1 else 2)
            .put("group",game.group)
            .put("level",game.level.name).put("round",game.round).put("ended",game.ended)
            .put("names",JSONArray(game.playerNames))
            .put("scores",JSONArray(game.scores)).put("skipped",JSONArray(game.skipped))
        game.competition?.let { rules ->
            require(!game.group && game.playerNames.size == 2 && rules.outcomes.size == game.round)
            val details=JSONObject()
                .put("reward",rules.reward).put("sessionMinutes",rules.sessionMinutes)
                .put("startedAtMillis",rules.startedAtMillis)
                .put("settledEarly",rules.settledEarly)
                .put("outcomes",JSONArray(rules.outcomes.map { it.name }))
            rules.tieBreak?.let { tie ->
                val tieJson=JSONObject().put("stage",tie.stage.name)
                    .put("sharedCompleted",JSONArray(tie.sharedCompleted))
                    .put("knowledgeRound",tie.knowledgeRound)
                    .put("winnerIndex",tie.winnerIndex ?: -1)
                val questions=JSONArray()
                tie.questions.forEach { prompt ->
                    questions.put(JSONObject().put("question",prompt.question)
                        .put("expectedAnswer",prompt.expectedAnswer))
                }
                details.put("tieBreak",tieJson.put("questions",questions))
            }
            json.put("competition",details)
        }
        val turns=JSONArray()
        game.turns.forEach { t ->
            turns.put(JSONObject().put("index",t.roundIndex).put("actor",t.actorIndex)
                .put("id",t.prompt.id).put("level",t.prompt.level.name).put("text",t.prompt.text))
        }
        vault.writePrivate(name,json.put("turns",turns).toString())
    }

    fun load(vault:LocalVault,name:String,expectedGroup:Boolean):LocalGame? = runCatching {
        val raw=vault.readPrivate(name)?:return null
        val json=JSONObject(raw)
        val version=json.getInt("version")
        require(version in 1..2 && json.getBoolean("group")==expectedGroup)
        require(version != 2 || !expectedGroup)
        val ns=json.getJSONArray("names")
        val names=(0 until ns.length()).map{ns.getString(it)}
        require(names.size in 2..4 && (expectedGroup || names.size==2) &&
            names.all{it.isNotBlank() && it.length<=32})
        val ts=json.getJSONArray("turns")
        require(ts.length() in 6..18)
        val turns=(0 until ts.length()).map{i->
            val t=ts.getJSONObject(i)
            val actor=t.getInt("actor")
            require(t.getInt("index")==i && actor in names.indices)
            val prompt=t.getString("text")
            require(prompt.length in 1..500)
            Turn(i,actor,Challenge(t.getString("id"),Intensity.valueOf(t.getString("level")),prompt))
        }
        fun counts(key:String, signed:Boolean=false):List<Int>{
            val values=json.getJSONArray(key)
            require(values.length()==names.size)
            return (0 until values.length()).map { index ->
                values.getInt(index).also { value ->
                    require(if(signed) value in -180..180 else value in 0..ts.length())
                }
            }
        }
        val round=json.getInt("round")
        require(round in 0..turns.size)
        val scores=counts("scores",version==2);val skipped=counts("skipped")
        val competition = if(version == 2) {
            val rules=json.getJSONObject("competition")
            val rawOutcomes=rules.getJSONArray("outcomes")
            val outcomes=(0 until rawOutcomes.length()).map {
                CoupleOutcome.valueOf(rawOutcomes.getString(it))
            }
            require(outcomes.size == round)
            val state=rules.optJSONObject("tieBreak")?.let { t ->
                val shared=t.getJSONArray("sharedCompleted")
                val prompts=t.getJSONArray("questions")
                CoupleTieBreak(
                    stage=TieStage.valueOf(t.getString("stage")),
                    sharedCompleted=(0 until shared.length()).map { shared.getBoolean(it) },
                    questions=(0 until prompts.length()).map { idx ->
                        val p=prompts.getJSONObject(idx)
                        KnowledgePrompt(p.getString("question"),p.getString("expectedAnswer"))
                    },
                    knowledgeRound=t.getInt("knowledgeRound"),
                    winnerIndex=t.getInt("winnerIndex").takeIf { it >= 0 }
                )
            }
            CoupleCompetition(
                reward=rules.getString("reward"),
                sessionMinutes=rules.getInt("sessionMinutes"),
                startedAtMillis=rules.getLong("startedAtMillis"),
                outcomes=outcomes,
                settledEarly=rules.optBoolean("settledEarly",false),
                tieBreak=state
            )
        } else null
        if(competition == null) {
            require(scores.sum()+skipped.sum()==round)
        } else {
            val expectedScores=MutableList(2){0}
            val expectedSkipped=MutableList(2){0}
            competition.outcomes.forEachIndexed { index,outcome ->
                val actor=turns[index].actorIndex
                when(outcome) {
                    CoupleOutcome.COMPLETED -> expectedScores[actor]+=10
                    CoupleOutcome.NOT_COMPLETED -> {expectedScores[actor]-=10;expectedSkipped[actor]++}
                    CoupleOutcome.AGREED_SKIP -> expectedSkipped[actor]++
                }
            }
            require(scores==expectedScores && skipped==expectedSkipped)
            require(!competition.settledEarly || round>0)
            require(competition.tieBreak==null ||
                (round==turns.size || competition.settledEarly) && scores[0]==scores[1])
        }
        LocalGame(names,Intensity.valueOf(json.getString("level")),expectedGroup,turns,round,
            scores,skipped,json.getBoolean("ended"),competition)
    }.getOrNull()

    fun readReply(vault:LocalVault,name:String,round:Int):String? = runCatching {
        val stored=vault.readPrivate(name)?:return null
        JSONObject(stored).optString(round.toString()).takeIf{it.isNotEmpty()}
    }.getOrNull()

    fun writeReply(vault:LocalVault,name:String,round:Int,text:String) {
        require(round in 0..17 && text.length<=1000)
        val json=runCatching{JSONObject(vault.readPrivate(name).orEmpty())}.getOrElse{JSONObject()}
        if(text.isBlank())json.remove(round.toString()) else json.put(round.toString(),text)
        vault.writePrivate(name,json.toString())
    }
}

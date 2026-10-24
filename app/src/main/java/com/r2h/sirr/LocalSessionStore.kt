package com.r2h.sirr

import com.r2h.sirr.core.Challenge
import com.r2h.sirr.core.Intensity
import com.r2h.sirr.core.LocalGame
import com.r2h.sirr.core.Turn
import org.json.JSONArray
import org.json.JSONObject

/** Full local game stays inside the encrypted Keystore-backed vault, not a Bundle. */
internal object LocalSessionStore {
    fun save(vault:LocalVault,name:String,game:LocalGame) {
        require(game.turns.size in 6..18 && game.round in 0..game.turns.size)
        val json=JSONObject().put("version",1).put("group",game.group)
            .put("level",game.level.name).put("round",game.round).put("ended",game.ended)
            .put("names",JSONArray(game.playerNames))
            .put("scores",JSONArray(game.scores)).put("skipped",JSONArray(game.skipped))
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
        require(json.getInt("version")==1 && json.getBoolean("group")==expectedGroup)
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
        fun counts(key:String):List<Int>{
            val values=json.getJSONArray(key)
            require(values.length()==names.size)
            return (0 until values.length()).map{values.getInt(it).also{v->require(v in 0..ts.length())}}
        }
        val round=json.getInt("round")
        require(round in 0..turns.size)
        val scores=counts("scores");val skipped=counts("skipped")
        require(scores.sum()+skipped.sum()==round)
        LocalGame(names,Intensity.valueOf(json.getString("level")),expectedGroup,turns,round,
            scores,skipped,json.getBoolean("ended"))
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

package com.r2h.sirr

import org.json.JSONObject

/** Encrypted with app-owned Android Keystore key in no-backup storage, never in plain preferences. */
internal class RemoteSessionStorage(private val vault: LocalVault) {
    fun save(id: RemoteIdentity, expiresAt: Long) {
        require(expiresAt > System.currentTimeMillis() && id.sessionKey.size == 32)
        vault.writePrivate("active-remote",JSONObject()
            .put("version",1).put("server",id.server).put("roomId",id.roomId)
            .put("auth",id.auth).put("participantId",id.participantId)
            .put("key",RemoteCrypto.encode(id.sessionKey)).put("inviteUrl",id.inviteUrl)
            .put("expiresAt",expiresAt).toString())
    }
    fun restore(): RemoteIdentity? = runCatching {
        val json=JSONObject(vault.readPrivate("active-remote")?:return null)
        require(json.getInt("version")==1 && json.getLong("expiresAt")>System.currentTimeMillis())
        val server=json.getString("server")
        require(server.startsWith("https://") && !server.contains('@'))
        val roomId=json.getString("roomId")
        require(roomId.matches(Regex("[0-9a-f-]{36}")))
        val auth=json.getString("auth")
        require(auth.matches(Regex("[A-Za-z0-9_-]{32,}")))
        val key=RemoteCrypto.decode(json.getString("key"));require(key.size==32)
        RemoteIdentity(server,roomId,auth,json.getString("participantId"),key,
            json.optString("inviteUrl").takeIf{it.startsWith("https://")})
    }.getOrNull()
    fun clear() = vault.deletePrivate("active-remote")
}

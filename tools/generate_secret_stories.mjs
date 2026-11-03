#!/usr/bin/env node
// Deterministic content generator: node tools/generate_secret_stories.mjs [--check]
// Writes only the secret-story Kotlin catalog, never frozen Challenge v10 files.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
const base=path.resolve(path.dirname(fileURLToPath(import.meta.url)),"..");
const source=path.join(base,"content","whispers_scratch_stories_v1.json");
const target=path.join(base,"app","src","main","java","com","r2h","sirr","core","SecretStoryCatalog.kt");
const d=JSON.parse(fs.readFileSync(source,"utf8"));
if(d.version!==1||d.locale!=="ar-EG"||d.whispers.length!==20||d.scratch.length!==20) throw Error("Unexpected story schema");
const quote=s=>JSON.stringify(s).replaceAll("$","\\$");
const tiers=["SOFT","FLIRT","ADULT"];
for(const [name,rows,prefix] of [["whispers",d.whispers,"w"],["scratch",d.scratch,"s"]]){
  const ids=new Set(),texts=new Set();
  for(let i=0;i<rows.length;i++){
    const story=rows[i];
    if(story.id!==prefix+String(i+1).padStart(2,"0")||!tiers.includes(story.tier)||!Array.isArray(story.lines)||story.lines.length!==3)throw Error("Malformed story "+name+" "+i);
    if(ids.has(story.id))throw Error("Duplicate id");
    ids.add(story.id);
    for(const text of story.lines){
      if(typeof text!=="string"||text.trim().length<12||text.length>350||texts.has(text))throw Error("Malformed/duplicate line: "+story.id);
      texts.add(text);
    }
  }
  for(const tier of tiers) if(rows.filter(s=>s.tier===tier).length===0)throw Error("Missing tier "+tier);
}
const a=["package com.r2h.sirr.core","","/** Canonical stories are generated from content/whispers_scratch_stories_v1.json. */",
"internal enum class SecretTier(val arabic: String) {", '    SOFT("هادي"), FLIRT("غزل"), ADULT("حميم 18+")', "}",
"",
"internal data class SecretStory(val id: String, val tier: SecretTier, val lines: List<String>) {",
"    init {", '        require(id.matches(Regex("[ws][0-9]{2}")))', "        require(lines.size == 3 && lines.all { it.isNotBlank() && it.length <= 350 })", "    }", "}",
"",
"internal object SecretStoryCatalog {", "    val whispers: List<SecretStory> = listOf("];
for(const [name,prefix] of [["whispers","w"],["scratch","s"]]){
  if(prefix==="s")a.push("    )","","    val scratch: List<SecretStory> = listOf(");
  for(const [i,story]of d[name].entries())
    a.push("        SecretStory("+quote(story.id)+", SecretTier."+story.tier+", listOf("+story.lines.map(quote).join(", ")+"))"+(i<d[name].length-1?",":""));
}
a.push("    )","","    fun pick(whispersMode: Boolean, tier: SecretTier, pickIndex: Int): SecretStory {",
"        val matches = (if (whispersMode) whispers else scratch).filter { it.tier == tier }",
"        require(matches.isNotEmpty())",
"        return matches[Math.floorMod(pickIndex, matches.size)]",
"    }",
"",
"    /** For a one-card scratch the final reveal must contain the actual secret. */",
"    fun scratchLayers(story: SecretStory, layerCount: Int): List<String> {",
'        require(story.id.startsWith("s") && layerCount in 1..3)',
"        return story.lines.takeLast(layerCount)",
"    }",
"}","");
const expected=a.join("\n");
if(process.argv.includes("--check")){
  if(fs.readFileSync(target,"utf8")!==expected)throw Error("Stale SecretStoryCatalog Kotlin");
  console.log("SecretStoryCatalog up to date, 40 stories");
}else{
  fs.writeFileSync(target,expected,"utf8");
  console.log("Generated 40 stories");
}

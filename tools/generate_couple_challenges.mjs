#!/usr/bin/env node
// Offline generator for the user-reviewable original couple challenge bank.
// Usage: node tools/generate_couple_challenges.mjs [--check]
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const contentPath = path.join(root, "content", "couple_challenges_v2.json");
const outputPath = path.join(root,"app","src","main","java","com","r2h","sirr","core","CoupleChallengeCatalog.kt");
const data = JSON.parse(fs.readFileSync(contentPath,"utf8"));
const levels=["SOFT","NORMAL","SEXY","HOT","HARD"];
const kinds=new Set(["TALK","WHISPER","CHOICE","CLOSENESS","TOGETHER","SURPRISE"]);
if(data.version!==2||Object.keys(data.couple||{}).sort().join(",")!==[...levels].sort().join(","))throw Error("Invalid content schema");
const ids=new Set(),texts=new Set();
for(const level of levels){
  const rows=data.couple[level];
  if(!Array.isArray(rows)||rows.length!==20)throw Error("Need 20 activities in "+level);
  for(const [i,row] of rows.entries()){
    if(row.id!=="couple2-"+level+"-"+(i+1)||!kinds.has(row.kind)||typeof row.title!=="string"||
       typeof row.text!=="string"||row.title.trim().length<3||row.text.trim().length<20||
       row.text.length>500||!Number.isInteger(row.seconds)||row.seconds<30||row.seconds>300)throw Error("Invalid challenge "+row.id);
    if(ids.has(row.id)||texts.has(row.text))throw Error("Duplicate activity "+row.id);
    ids.add(row.id);texts.add(row.text);
  }
}
const render = function render(d){
  const levels=["SOFT","NORMAL","SEXY","HOT","HARD"];
  const kinds=[["TALK","كلام بينكم"],["WHISPER","همسة"],["CHOICE","اختيار"],["CLOSENESS","قرب"],["TOGETHER","سوا"],["SURPRISE","مفاجأة"],["PERSONAL","من عندكم"]];
  const kotlinString = s => JSON.stringify(s).replaceAll("$", "\\$");
  const out=["package com.r2h.sirr.core","","/** Generated from content/couple_challenges_v2.json. Do not edit manually. */",
    "enum class CoupleChallengeKind(val arabic: String) {",...kinds.map(([k,v],i)=>`    ${k}(${kotlinString(v)})${i<kinds.length-1?",":""}`),"}","",
    "object CoupleChallengeCatalog {","    private val all: Map<Intensity, List<Challenge>> = mapOf("];
  levels.forEach((level,ix)=>{
    out.push(`        Intensity.${level} to listOf(`);
    d.couple[level].forEach((row,i)=>{
      out.push(`            Challenge(${kotlinString(row.id)}, Intensity.${level}, ${kotlinString(row.text)},`);
      out.push(`                title = ${kotlinString(row.title)}, kind = CoupleChallengeKind.${row.kind},`);
      out.push(`                durationSeconds = ${row.seconds})${i<d.couple[level].length-1?",":""}`);
    });
    out.push(`        )${ix<levels.length-1?",":""}`);
  });
  out.push("    )","","    fun deck(level: Intensity): List<Challenge> =",
    "        if (level == Intensity.CUSTOM) emptyList() else all.getValue(level)",
    "    fun counts(): Map<Intensity, Int> = all.mapValues { it.value.size }","}","");
  return out.join("\n");
};
const generated=render(data);
if(process.argv.includes("--check")){
   if(fs.readFileSync(outputPath,"utf8")!==generated)throw Error("Generated Kotlin activities differ from canonical JSON");
   console.log("Couple challenge content is synchronized: "+ids.size+" activities");
}else{
   fs.writeFileSync(outputPath,generated,"utf8");
   console.log("Generated "+ids.size+" couple activities");
}

#!/usr/bin/env node
// Deterministic generator for the staged user-owned v10 content. No Android build.
// Usage: node tools/generate_couple_style_v10.mjs [--check]
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const src = path.join(root, "content", "couple_challenges_v10_refined.json");
const dest = path.join(root, "app", "src", "main", "java", "com", "r2h", "sirr", "core", "CoupleStyleV10Catalog.kt");
const d = JSON.parse(fs.readFileSync(src, "utf8"));
const levels = ["SOFT","NORMAL","SEXY","HOT","HARD"];
if (d.version !== 10 || Object.keys(d.couple ?? {}).join(",") !== levels.join(",")) {
  throw new Error("Unexpected v10 schema or level order");
}
const ids = new Set(), kinds = new Set();
for (const lvl of levels) {
  if (!Array.isArray(d.couple[lvl]) || d.couple[lvl].length !== 20) throw new Error("Expected 20 entries: "+lvl);
  d.couple[lvl].forEach((row,i) => {
    if (row.id !== "c-"+lvl+"-"+(i+1) || !/^[A-Z][A-Z0-9_]+$/.test(row.kind) ||
        typeof row.title !== "string" || row.title.trim().length < 2 ||
        typeof row.text !== "string" || row.text.trim().length < 10 || row.text.length > 500 ||
        !Number.isInteger(row.seconds) || row.seconds < 30 || row.seconds > 300) {
      throw new Error("Invalid item "+row.id);
    }
    if (ids.has(row.id) || kinds.has(row.kind)) throw new Error("v10 ID/kind uniqueness failed");
    ids.add(row.id); kinds.add(row.kind);
  });
}
const quote = s => JSON.stringify(s).replaceAll("$","\\$");
const l = ["package com.r2h.sirr.core","","/** Staged owner content v10. NOT activated in SessionFactory until reviewed. */",
"internal data class CoupleStyleV10Activity(",
"    val id: String,",
"    val level: Intensity,",
"    val kindCode: String,",
"    val title: String,",
"    val text: String,",
"    val durationSeconds: Int",
")",
"",
"internal object CoupleStyleV10Catalog {",
"    private val byLevel: Map<Intensity, List<CoupleStyleV10Activity>> = mapOf("];
for (let x=0;x<levels.length;x++) {
  const lvl=levels[x]; l.push("        Intensity."+lvl+" to listOf(");
  for (let j=0;j<d.couple[lvl].length;j++) {
    const row=d.couple[lvl][j];
    l.push("            CoupleStyleV10Activity("+
      quote(row.id)+", Intensity."+lvl+", "+quote(row.kind)+", "+quote(row.title)+
      ", "+quote(row.text)+", "+row.seconds+")"+(j<d.couple[lvl].length-1?",":""));
  }
  l.push("        )"+(x<levels.length-1?",":""));
}
l.push("    )","","    fun deck(level: Intensity): List<CoupleStyleV10Activity> = byLevel[level].orEmpty()","}","");
const generated=l.join("\n");
if (process.argv.includes("--check")) {
  if (fs.readFileSync(dest,"utf8")!==generated) throw new Error("Generated v10 catalog is stale");
  console.log("Staged v10 catalog matched: "+ids.size+" records");
} else {
  fs.writeFileSync(dest,generated,"utf8");
  console.log("Generated staged v10 catalog: "+ids.size+" records");
}

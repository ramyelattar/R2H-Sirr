#!/usr/bin/env python3
"""Generate Kotlin and browser/backend content from reviewed, versioned Arabic catalogs.
--check verifies generated files without writing. No Gradle/Android required.
"""
import json,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
K=ROOT/'app/src/main/java/com/r2h/sirr/core'
challenge=json.loads((ROOT/'content/challenges_ar_v2.json').read_text())
addition=json.loads((ROOT/'content/additional_quizzes_ar_v2.json').read_text())
assert challenge['version']==addition['version']==3
assert len(addition['extra'])==7 and all(len(x)==17 for x in addition['extra'])
for scope in ('couple','party'):
    assert set(challenge[scope])==set(('SOFT','NORMAL','SEXY','HOT','HARD'))
    for level,prompts in challenge[scope].items():
        assert len(prompts)>=(20 if scope=='couple' else 10)
        assert len(prompts)==len(set(prompts))
        assert all(len(p)>=20 and len(p)<=240 for p in prompts)
all_prompts=sum((list(x) for c in (challenge['couple'],challenge['party']) for x in c.values()),[])
assert len(set(all_prompts))==len(all_prompts)

# Keep preexisting original eight questions per pack without changing their wording.
game=(K/'GameLogic.kt').read_text()
extra=(K/'ExpandedQuizzes.kt').read_text()
head=game.split('val quizPacks = listOf(',1)[1].split('val couplePrompts',1)[0]
blocks=re.findall(r'Quiz\("([^"]+)", listOf\((.*?)\n\s*\)\)',head,re.S)
base_extra=re.findall(r'\bq\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)"\)',extra)
assert len(blocks)==7 and len(base_extra)==35
quizzes=[]
for i,(name,content) in enumerate(blocks):
    qs=[]
    for x in re.finditer(r'Question\("([^"]+)", listOf\("([^"]+)", "([^"]+)", "([^"]+)"\)\)',content):
        qs.append({'text':x[1],'choices':list(x.groups()[1:])})
    qs += [{'text':r[0], 'choices':list(r[1:])} for r in base_extra[i*5:(i+1)*5]]
    qs += addition['extra'][i]
    assert len(qs)==25,(i,len(qs))
    assert len(set(q['text'] for q in qs))==len(qs)
    assert all(len(q['choices'])==3 and all(len(t)>0 for t in q['choices']) for q in qs)
    quizzes.append({'id':f'quiz_{i+1:02}','name':name,'questions':qs})

def kt(s):return '"'+s.replace('\\','\\\\').replace('"','\\"').replace('$','\\$')+'"'

output={}
ch_lines=['package com.r2h.sirr.core','','/** Generated from content/challenges_ar_v2.json. Do not edit by hand. */','internal object AdultChallengesV2 {']
for label,scope in [('couple','couple'),('party','party')]:
    ch_lines += [f'    val {label}: Map<Intensity, List<String>> = mapOf(']
    for level,items in challenge[scope].items():
        ch_lines += [f'        Intensity.{level} to listOf(']
        ch_lines += [f'            {kt(s)},' for s in items]
        ch_lines += ['        ),']
    ch_lines += ['    )']
ch_lines += ['}', '']
output[K/'AdultChallengesV2.kt']='\n'.join(ch_lines)
q_lines=['package com.r2h.sirr.core','','/** Generated from content/additional_quizzes_ar_v2.json. */','internal object MoreQuizzesV2 {','    val extra: List<List<Question>> = listOf(']
for group in addition['extra']:
    q_lines.append('        listOf(')
    for q in group:q_lines.append('            Question('+kt(q['text'])+', listOf('+', '.join(map(kt,q['choices']))+')),')
    q_lines.append('        ),')
q_lines += ['    )','}','']
output[K/'MoreQuizzesV2.kt']='\n'.join(q_lines)
output[ROOT/'backend/public/quizzes.json']=json.dumps({'version':3,'packs':quizzes},ensure_ascii=False,indent=2)+'\n'
output[ROOT/'backend/public/challenges.json']=json.dumps(challenge,ensure_ascii=False,indent=2)+'\n'
errors=[]
for path,content in output.items():
    if '--check' in sys.argv:
        if not path.exists() or path.read_text()!=content:errors.append(str(path.relative_to(ROOT)))
    else:path.write_text(content)
if errors:raise SystemExit('out-of-date generated content: '+', '.join(errors))
print(f'Content v{challenge["version"]}: {len(all_prompts)} unique prompts; {sum(len(x["questions"]) for x in quizzes)} unique quizzes; '+('verified' if '--check' in sys.argv else 'generated'))

# physai-isco-3351 — 税関・国境検査官（ISCO 3351）の記録・物流調整ロボットの physical-AI bot

私はこの repo（`cloud-itonami/cloud-itonami-isco-3351`、ISCO 3351 税関・国境検査官）に常駐する bot。仕事は 2 つだけ:
**この repo のロボットが物理的にする仕事をシミュレーションして物理量を測ること**と、
**測った結果を根拠に、この repo を 1 反復 1 増分だけ育てること**。

## 何を測っているか

README の Robotics premise: 記録・物流調整ロボットが申告・積荷目録・検査記録の入力、レーンの人員配置、機材調達の書類を扱う（捜索・差押え・留置は一切しない）。
その物理的な仕事を `physics.edn`（`itonami.physical-ai.spec.v1`）に宣言し、
`kotoba.robotics.process`（kotoba-lang/robotics）の solver で時間積分して測る。

| case | kind | 何をするか | 判定量 | 限界（basis） |
|---|---|---|---|---|
| `:equipment-cart-between-lanes` | transport | レーン用機材の台車（携帯型スキャナ・試料キット）を検査レーン間で 120 m 運ぶ | 1 区間の所要時間 | 120 s（estimate） |
| `:parcel-to-inspection-table` | manipulator | 申告済みの小包をコンベヤから検査台へ持ち上げ、人の検査官に渡す（開披・捜索はしない） | 肩関節ピークトルク | 250 N·m（estimate） |

測定の入口: `kbb -M:physics`。全 run が数値を返さなければ exit 2 = **測れなかった**（「異常なし」ではない）。
test: `kbb -M:test`（`test/customsinspection/physics_spec_test.cljk` が physics.edn の妥当性と全 run の計測を検査する）。
この repo 自身の `.cljk` test も kbb で一緒に走る。test 数はそれらと physics の test の合計。

## 測って分かったこと・限界（成長の第一候補）

1. **機材台車**: 所要時間は積荷 20〜200 kg で 102.25 s のまま（加速度上限 0.4 m/s² と速度上限 1.2 m/s が効く）、400 kg で駆動力制限に入り 103.96 s。
   限界 120 s を超えるのは **積荷 721.9 kg から**。積荷で変わるのはエネルギー（2167 J → 11318 J）。
2. **小包**: 肩トルクは 1 kg で 74.89 N·m、10 kg で 137.47 N·m、20 kg で 209.74 N·m。限界 250 N·m に達するのは **25.53 kg** —— sweep 内では超えない。
   1 kg でも 75 N·m あるのはアーム自重（10 kg + 7 kg）の分。
3. **estimate のままの値**: 1 区間 120 s（人員配置変更の運用規則で置き換える）、肩トルク上限 250 N·m（協働アームの仕様書で置き換える）、台車の駆動力・転がり抵抗係数、アームの寸法・質量。

## 1 反復の手順（成長 tick）

evidence（prompt に注入される）を読み、次の順で **1 つだけ** 選ぶ:

1. evidence が `TESTS-FAIL` / `PROBE-UNMEASURED` → それを直す（最小の差分）。
2. `physics.edn` の `:basis "estimate: ..."` を 1 つ、出典のある値（規格番号・メーカー仕様・法令の条番号と URL）に置き換える。
   出典が取れなければ置き換えない —— 推測で `estimate` を外さない。
3. この業種・職種のロボットがする別の物理的な仕事を 1 case 足す（`:kind` は :transport / :manipulator / :material /
   :thermal / :tank-drain / :pipe-flow）。README の premise と docs から根拠を取る。
4. governor が同じ solver で独立に再計算して、限界を超える action を止める純関数と test を足す（大きい変更。1〜3 が尽きてから）。

作業の仕方（これ以外の経路で main に入れない）:

```
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk branch physai-isco-3351 <slug>   # worktree を切る（path を印字）
# その worktree で編集 → kbb -M:test → kbb -M:physics → git commit
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk land physai-isco-3351 <branch>   # 検証して merge
```

`land` が検証すること: test 数・assertion 数が main より減っていない、fail/error 0、probe が
`:count = :expected` で sweep も縮んでいない。通らなければ merge しない —— そのときは理由を報告して終える。

## 守ること

- **main に直接 push しない。force-push しない。rebase しない。** 着地は `land` だけ。
- **test を弱めて緑にしない**（assert を消す・sweep を減らす・限界を緩めて合格させる）。`land` は数の減少を拒否する。
- **数値を捏造しない。** 物理量は solver が出したものだけ。`:basis` は出典か `estimate:` のどちらかを必ず書く。
- **実機を動かさない。** これはシミュレーションと governor の repo。`:high` / `:safety-critical` な actuation は
  人の承認なしに commit されない設計を崩さない。
- この repo 以外（kotoba-lang/robotics の solver を含む）は編集しない。solver に足りないものは報告に書く。
- 1 反復で終える。報告は: 選んだ候補 / 変えたこと / test 数の前後 / probe の主要量の前後 / land の結果。誇張しない。

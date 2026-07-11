/*
    All Rights Reserved, Copyright (C) 2025, mikoto2000
      Licensed Material of mikoto2000.

    All Rights Reserved, Copyright (C) 2026, Moto+4 Applications LLC
      Licensed Material of Moto+4 Applications LLC.
 */

package com.motoplus4.J_KeyboardKanaPlus.ime

import android.util.Log
import com.motoplus4.J_KeyboardKanaPlus.R
import com.motoplus4.J_KeyboardKanaPlus.ime.JapaneseKeyboardService.keyCodeToMoji
import kotlin.arrayOf

/**
 * Simple streaming Romaji -> Kana converter for IME composing.
 * - Greedy consume with longest-match mapping (3, 2, 1 chars)
 * - Handles sokuon (double consonant except 'n') -> っ
 * - Handles 'nn' -> ん, and single 'n' before non-vowel/y or at flush -> ん
 * - Keeps produced Kana separate from pending romaji buffer
 */
class RomajiConverter {
//===============================================================================
//            S E C T O I N   C O M M O N   V A R I A B L E
//===============================================================================
    private val produced = StringBuilder()
    private val buffer = StringBuilder()

//===============================================================================
//          S E C T O I N   C O N S T A N  T  D A T A
//===============================================================================
    private val mydbg = "[MYDBG3]"

    private val vowels = setOf('a','i','u','e','o')

    // Base romaji->kana table (not exhaustive but practical)
    private val map = mapOf(
        // Vowels
        "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",

        // K
        "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
        "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",

        // S
        "sa" to "さ", "shi" to "し", "si" to "し", "su" to "す", "se" to "せ", "so" to "そ",
        "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",

        // T
        "ta" to "た", "chi" to "ち", "ti" to "ち", "tsu" to "つ", "tu" to "つ", "te" to "て", "to" to "と",
        "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",

        // N
        "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
        "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",

        // H
        "ha" to "は", "hi" to "ひ", "fu" to "ふ", "hu" to "ふ", "he" to "へ", "ho" to "ほ",
        "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",

        // M
        "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
        "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",

        // Y
        "ya" to "や", "yu" to "ゆ", "yo" to "よ",

        // R
        "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
        "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",

        // W
        "wa" to "わ", "wo" to "を",

        // G
        "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
        "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",

        // Z/J
        "za" to "ざ", "zi" to "じ", "ji" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
        "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",

        // D
        "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
        "dya" to "ぢゃ", "dyu" to "ぢゅ", "dyo" to "ぢょ",

        // B
        "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
        "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",

        // P
        "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
        "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",

        // F (extended)
        "fa" to "ふぁ", "fi" to "ふぃ", "fe" to "ふぇ", "fo" to "ふぉ",

        // Small vowels (optional)
        "xa" to "ぁ", "xi" to "ぃ", "xu" to "ぅ", "xe" to "ぇ", "xo" to "ぉ"
    )

    private val HiraganaToKatakanaMap = listOf(
            arrayOf( "ぁ", "ァ" ),
            arrayOf( "あ", "ア" ),
            arrayOf( "ぃ", "ィ" ),
            arrayOf( "い", "イ" ),
            arrayOf( "ぅ", "ゥ" ),
            arrayOf( "う", "ウ" ),
            arrayOf( "ぇ", "ェ" ),
            arrayOf( "え", "エ" ),
            arrayOf( "ぉ", "ォ" ),
            arrayOf( "お", "オ" ),
            arrayOf( "か", "カ" ),
            arrayOf( "が", "ガ" ),
            arrayOf( "き", "キ" ),
            arrayOf( "ぎ", "ギ" ),
            arrayOf( "く", "ク" ),
            arrayOf( "ぐ", "グ" ),
            arrayOf( "け", "ケ" ),
            arrayOf( "げ", "ゲ" ),
            arrayOf( "こ", "コ" ),
            arrayOf( "ご", "ゴ" ),
            arrayOf( "さ", "サ" ),
            arrayOf( "ざ", "ザ" ),
            arrayOf( "し", "シ" ),
            arrayOf( "じ", "ジ" ),
            arrayOf( "す", "ス" ),
            arrayOf( "ず", "ズ" ),
            arrayOf( "せ", "セ" ),
            arrayOf( "ぜ", "ゼ" ),
            arrayOf( "そ", "ソ" ),
            arrayOf( "ぞ", "ゾ" ),
            arrayOf( "た", "タ" ),
            arrayOf( "だ", "ダ" ),
            arrayOf( "ち", "チ" ),
            arrayOf( "ぢ", "ヂ" ),
            arrayOf( "っ", "ッ" ),
            arrayOf( "つ", "ツ" ),
            arrayOf( "づ", "ヅ" ),
            arrayOf( "て", "テ" ),
            arrayOf( "で", "デ" ),
            arrayOf( "と", "ト" ),
            arrayOf( "ど", "ド" ),
            arrayOf( "な", "ナ" ),
            arrayOf( "に", "ニ" ),
            arrayOf( "ぬ", "ヌ" ),
            arrayOf( "ね", "ネ" ),
            arrayOf( "の", "ノ" ),
            arrayOf( "は", "ハ" ),
            arrayOf( "ば", "バ" ),
            arrayOf( "ぱ", "パ" ),
            arrayOf( "ひ", "ヒ" ),
            arrayOf( "び", "ビ" ),
            arrayOf( "ぴ", "ピ" ),
            arrayOf( "ふ", "フ" ),
            arrayOf( "ぶ", "ブ" ),
            arrayOf( "ぷ", "プ" ),
            arrayOf( "へ", "ヘ" ),
            arrayOf( "べ", "ベ" ),
            arrayOf( "ぺ", "ペ" ),
            arrayOf( "ほ", "ホ" ),
            arrayOf( "ぼ", "ボ" ),
            arrayOf( "ぽ", "ポ" ),
            arrayOf( "ま", "マ" ),
            arrayOf( "み", "ミ" ),
            arrayOf( "む", "ム" ),
            arrayOf( "め", "メ" ),
            arrayOf( "も", "モ" ),
            arrayOf( "ゃ", "ャ" ),
            arrayOf( "や", "ヤ" ),
            arrayOf( "ゅ", "ュ" ),
            arrayOf( "ゆ", "ユ" ),
            arrayOf( "ょ", "ョ" ),
            arrayOf( "よ", "ヨ" ),
            arrayOf( "ら", "ラ" ),
            arrayOf( "り", "リ" ),
            arrayOf( "る", "ル" ),
            arrayOf( "れ", "レ" ),
            arrayOf( "ろ", "ロ" ),
            arrayOf( "わ", "ワ" ),
            arrayOf( "を", "ヲ" ),
            arrayOf( "ん", "ン" ),
            arrayOf( "ゔ", "ヴ" )
    )

//===============================================================================
//                      S E C T O I N   P R O G R A M
//===============================================================================
    data class dakutenToMoji (
        var  seion : String,    // ex) た
        var  dakuon : String    // ex) だ
    )

    private val HiraganaDakutenToMojiMap = arrayOf(
        // NOTE: Not assgin "う" + "゛"  in UTF-8.
        dakutenToMoji( "か", "が" ),
        dakutenToMoji( "き", "ぎ" ),
        dakutenToMoji( "く", "ぐ" ),
        dakutenToMoji( "け", "げ" ),
        dakutenToMoji( "こ", "ご" ),
        dakutenToMoji( "さ", "ざ" ),
        dakutenToMoji( "し", "じ" ),
        dakutenToMoji( "す", "ず" ),
        dakutenToMoji( "せ", "ぜ" ),
        dakutenToMoji( "そ", "ぞ" ),
        dakutenToMoji( "た", "だ" ),
        dakutenToMoji( "ち", "ぢ" ),
        dakutenToMoji( "つ", "づ" ),
        dakutenToMoji( "て", "で" ),
        dakutenToMoji( "と", "ど" ),
        dakutenToMoji( "は", "ば" ),
        dakutenToMoji( "ひ", "び" ),
        dakutenToMoji( "ふ", "ぶ" ),
        dakutenToMoji( "へ", "べ" ),
        dakutenToMoji( "ほ", "ぼ" ),
    )

    private val HiraganaHandakutenToMojiMap = arrayOf(
        dakutenToMoji( "は", "ぱ" ),
        dakutenToMoji( "ひ", "ぴ" ),
        dakutenToMoji( "ふ", "ぷ" ),
        dakutenToMoji( "へ", "ぺ" ),
        dakutenToMoji( "ほ", "ぽ" ),
    )

    private val KatakanaDakutenToMojiMap = arrayOf(
        dakutenToMoji( "ウ", "ヴ" ),
        dakutenToMoji( "カ", "ガ" ),
        dakutenToMoji( "キ", "ギ" ),
        dakutenToMoji( "ク", "グ" ),
        dakutenToMoji( "ケ", "ゲ" ),
        dakutenToMoji( "コ", "ゴ" ),
        dakutenToMoji( "サ", "ザ" ),
        dakutenToMoji( "シ", "ジ" ),
        dakutenToMoji( "ス", "ズ" ),
        dakutenToMoji( "セ", "ゼ" ),
        dakutenToMoji( "ソ", "ゾ" ),
        dakutenToMoji( "タ", "ダ" ),
        dakutenToMoji( "チ", "ヂ" ),
        dakutenToMoji( "ツ", "ヅ" ),
        dakutenToMoji( "テ", "デ" ),
        dakutenToMoji( "ト", "ド" ),
        dakutenToMoji( "ハ", "バ" ),
        dakutenToMoji( "ヒ", "ビ" ),
        dakutenToMoji( "フ", "ブ" ),
        dakutenToMoji( "ヘ", "ベ" ),
        dakutenToMoji( "ホ", "ボ" ),
    )

    private val KatakanaHandakutenToMojiMap = arrayOf(
        dakutenToMoji( "ハ", "パ" ),
        dakutenToMoji( "ヒ", "ピ" ),
        dakutenToMoji( "フ", "プ" ),
        dakutenToMoji( "ヘ", "ペ" ),
        dakutenToMoji( "ホ", "ポ" ),
    )

    public fun Clear() {
        produced.clear()
        buffer.clear()
    }

    public fun hasComposing(): Boolean = produced.isNotEmpty() || buffer.isNotEmpty()

    public fun PushChar(c: Char) {
        val ch = c.lowercaseChar()
        if (ch !in 'a'..'z') return // ignore non-letters here
        buffer.append(ch)
        consume()   // Alphabets convert to kana.
    }

    public fun PushHiraganaChar(str: String) {
        pushKanaCharCore( str, HiraganaDakutenToMojiMap, HiraganaHandakutenToMojiMap )
    }

    public fun PushKatakanaChar(str: String) {
        pushKanaCharCore( str, KatakanaDakutenToMojiMap, KatakanaHandakutenToMojiMap )
    }

    private fun pushKanaCharCore(
        str: String,
        dakuonMap    :Array<dakutenToMoji>,
        handakuonMap :Array<dakutenToMoji>
    ) {
        if ( buffer.isNotEmpty() ) {
            val keyMap : Array<dakutenToMoji>

            if ( str == "゛" ) {
                keyMap = dakuonMap.copyOf()
            }
            else if ( str == "゜" ) {
                keyMap = handakuonMap.copyOf()
            }
            else {  // No replace
                buffer.append(str)  // case: no dakuon at second KANA onword
                return
            }

            for( rec in keyMap ) {
                val seion = buffer.get(buffer.lastIndex).toString()
                if ( seion == rec.seion ) {  // find dakuon-go
                    buffer.deleteCharAt(buffer.lastIndex)   // delete seion
                    buffer.append(rec.dakuon)               // replace seion to dakuon
                    return
                }
            }
        }

        buffer.append(str)  // case: no dakuon at first KANA, umtach dakuon
    }

    public fun Backspace() {
        if (buffer.isNotEmpty()) {
            buffer.deleteCharAt(buffer.lastIndex)
            return
        }
        if (produced.isNotEmpty()) {
            produced.deleteCharAt(produced.lastIndex)
        }
    }

    public fun Delete() {
        if (buffer.isNotEmpty()) {
            if (buffer.lastIndex > 0) buffer.deleteCharAt( buffer.lastIndex-1 )
            return
        }
        if (produced.isNotEmpty()) {
            if (produced.lastIndex > 0) produced.deleteCharAt( produced.lastIndex-1 )
        }
    }

    public fun GetComposing(): String {
        // Show produced kana and any pending raw romaji so consonants are visible while composing.
        return produced.toString() + buffer.toString()
    }

    public fun Flush(): String {
        // Finalize pending buffer (resolve 'n' to ん, and emit any leftover romaji literally)
        finalizeN()
        val out = produced.toString() + buffer.toString()
        Log.d(mydbg, "Flush txt=" + out);
        Clear()
        return out
    }

    public fun HiraganaToKatakana(): String {
        finalizeN()
        val out = produced.toString() + buffer.toString()

        var newOut : String = ""
        for( ch in out ) {
            val idx = HiraganaToKatakanaMap.binarySearch { it[0].compareTo(ch.toString()) }
            if ( idx >= 0) {
                newOut += HiraganaToKatakanaMap[idx][1]
            }
            else {
                newOut += ch.toString()
            }
        }

        Log.d(mydbg, "Katakana txt=" + newOut);
        Clear()
        return newOut
    }

    public fun RestoreFromKana(kana: String) {
        produced.setLength(0)
        produced.append(kana)
        buffer.setLength(0)
    }

    private fun consume() {
        // Handle sokuon for double consonants (except 'n') at buffer head
        while (true) {
            if (buffer.length >= 2) {
                val c1 = buffer[0]
                val c2 = buffer[1]
                if (c1 == c2 && c1 !in vowels && c1 != 'n') {
                    // っ then drop one leading consonant
                    produced.append('っ')
                    buffer.deleteCharAt(0)
                    continue
                }
            }

            // Handle 'nn' -> ん
            if (buffer.startsWith("nn")) {
                produced.append('ん')
                buffer.delete(0, 2)
                continue
            }

            // If buffer starts with single 'n' followed by non-vowel and not 'y', commit ん
            if (buffer.length >= 2 && buffer[0] == 'n') {
                val nxt = buffer[1]
                if (nxt !in vowels && nxt != 'y') {
                    produced.append('ん')
                    buffer.deleteCharAt(0)
                    continue
                }
            }

            // Try longest romaji match (3 -> 2 -> 1)
            val consumed = when {
                buffer.length >= 3 && map.containsKey(buffer.substring(0, 3)) -> 3
                buffer.length >= 2 && map.containsKey(buffer.substring(0, 2)) -> 2
                buffer.length >= 1 && map.containsKey(buffer.substring(0, 1)) -> 1
                else -> 0
            }
            if (consumed > 0) {
                val key = buffer.substring(0, consumed)
                val kana = map[key]!!
                produced.append(kana)
                buffer.delete(0, consumed)
                continue
            }
            break
        }
    }

    private fun finalizeN() {
        if (buffer.toString() == "n") {
            produced.append('ん')
            buffer.clear()
        }
    }
}

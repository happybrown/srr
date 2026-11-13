package app.sureureuk.ui

/**
 * 한국어 어절 안에서는 줄이 바뀌지 않게 한다 (웹의 `word-break: keep-all`).
 *
 * 안드로이드는 한글을 글자 단위로 끊어 "버튼/을", "멈추/기"처럼 줄이 바뀐다.
 * 어절 안의 글자 사이에 폭 없는 WORD JOINER(U+2060)를 넣어 띄어쓰기에서만 줄이 바뀌게 한다.
 * 한 어절이 한 줄보다 길면 시스템이 알아서 끊는다.
 */
fun String.keepAll(): String {
    if (length < 2) return this
    val sb = StringBuilder(length * 2)
    for (i in indices) {
        val c = this[i]
        sb.append(c)
        if (i < lastIndex && !c.isWhitespace() && !this[i + 1].isWhitespace()) sb.append('⁠')
    }
    return sb.toString()
}

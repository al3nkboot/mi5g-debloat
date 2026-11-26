package com.midroid.debloat

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringsParityTest {
    private val placeholders = Regex("""%(?:\d+\$)?[a-zA-Z]""")

    private fun parse(file: File): Map<String, List<String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val result = mutableMapOf<String, List<String>>()
        val nodes = doc.getElementsByTagName("string")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as org.w3c.dom.Element
            if (el.getAttribute("translatable") == "false") continue
            result[el.getAttribute("name")] = placeholders.findAll(el.textContent).map { it.value }.toList().sorted()
        }
        return result
    }

    @Test
    fun englishAndArabicHaveSameKeysAndPlaceholders() {
        val en = parse(File("src/main/res/values/strings.xml"))
        val ar = parse(File("src/main/res/values-ar/strings.xml"))
        assertEquals(en.keys, ar.keys)
        en.forEach { (key, args) -> assertEquals("placeholders differ for $key", args, ar[key]) }
    }
}

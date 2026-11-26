package com.midroid.debloat

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringsParityTest {
    private val placeholders = Regex("""%(?:\d+\$)?[a-zA-Z]""")
    private val enFile = File("src/main/res/values/strings.xml")
    private val arFile = File("src/main/res/values-ar/strings.xml")

    private fun document(file: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private fun parse(file: File): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        val nodes = document(file).getElementsByTagName("string")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as org.w3c.dom.Element
            if (el.getAttribute("translatable") == "false") continue
            result[el.getAttribute("name")] = placeholders.findAll(el.textContent).map { it.value }.toList().sorted()
        }
        return result
    }

    private fun parsePlurals(file: File): Map<String, Map<String, List<String>>> {
        val result = mutableMapOf<String, MutableMap<String, List<String>>>()
        val nodes = document(file).getElementsByTagName("plurals")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as org.w3c.dom.Element
            val items = mutableMapOf<String, List<String>>()
            val children = el.getElementsByTagName("item")
            for (j in 0 until children.length) {
                val item = children.item(j) as org.w3c.dom.Element
                items[item.getAttribute("quantity")] =
                    placeholders.findAll(item.textContent).map { it.value }.toList().sorted()
            }
            result[el.getAttribute("name")] = items
        }
        return result
    }

    @Test
    fun englishAndArabicHaveSameKeysAndPlaceholders() {
        val en = parse(enFile)
        val ar = parse(arFile)
        assertEquals(en.keys, ar.keys)
        en.forEach { (key, args) -> assertEquals("placeholders differ for $key", args, ar[key]) }
    }

    @Test
    fun arabicPluralsHaveAllQuantitiesAndMatchingPlaceholders() {
        val en = parsePlurals(enFile)
        val ar = parsePlurals(arFile)
        assertEquals(en.keys, ar.keys)
        val quantities = setOf("zero", "one", "two", "few", "many", "other")
        en.forEach { (name, enItems) ->
            val arItems = ar.getValue(name)
            assertEquals("quantities differ for $name", quantities, arItems.keys)
            val expected = enItems.getValue("other")
            arItems.forEach { (quantity, args) ->
                assertEquals("placeholders differ for $name/$quantity", expected, args)
            }
        }
    }
}

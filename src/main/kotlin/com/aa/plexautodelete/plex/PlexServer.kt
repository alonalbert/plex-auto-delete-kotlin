package com.aa.plexautodelete.plex

import com.aa.plexautodelete.plex.model.Season
import com.aa.plexautodelete.plex.model.Section
import com.aa.plexautodelete.plex.model.Show
import com.aa.plexautodelete.plex.model.ShowSection
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.helpers.DefaultHandler
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

private var DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().apply {
  isValidating = true
  isIgnoringElementContentWhitespace = true
}.newDocumentBuilder().apply {
  setErrorHandler(DefaultHandler())
}

class PlexServer(private val baseUrl: URL, private val token: String) {

  constructor(baseUrl: String, token: String) : this(URL(baseUrl), token)

  fun getSections(): List<Section> {
    val doc = loadDocument("/library/sections")
    return doc.getDirectories().mapNotNull {
      when {
        it.isShowSection() -> it.createShowSection()
        else -> null
      }
    }
  }

  private fun loadDocument(path: String): Document {
    val connection = URL("$baseUrl$path?X-Plex-Token=$token").openConnection()
    return DOCUMENT_BUILDER.parse(connection.getInputStream())
  }

  private fun Node.createShowSection(): ShowSection {
    val sectionKey = getKey()
    val shows = loadDocument("/library/sections/$sectionKey/all").getDirectories().map { show ->
      val showKey = show.getKey()
      val seasons = loadDocument(showKey).getDirectories().filter { it.getTitle() != "All episodes" } .map { season ->
        val seasonKey = season.getKey()
        Season(seasonKey, season.getTitle())
      }
      Show(showKey, show.getTitle(), seasons)
    }
    return ShowSection(sectionKey, getTitle(), shows)
  }
}

private fun Document.getDirectories(): List<Node> = firstChild.childNodes.items().filter(Node::isDirectory)

private fun Node.isDirectory() = nodeName == "Directory"

private fun Node.isShowSection() = getStringAttr("type") == "show"

private fun NodeList.items() = (0 until length).map { item(it) }

private fun Node.getStringAttr(name: String) = attributes.getNamedItem(name).textContent

private fun Node.getKey() = getStringAttr("key")

private fun Node.getTitle() = getStringAttr("title")

fun main(vararg args: String) {
  val server = PlexServer(args[0], args[1])
  server.getSections().filterIsInstance<ShowSection>().forEach { section ->
    println(section.title)
    section.shows.forEach { show ->
      println("  ${show.title}: ${show.key}")
      show.seasons.forEach { season ->
        println("    ${season.title}: ${season.key}")
      }
    }
  }
}

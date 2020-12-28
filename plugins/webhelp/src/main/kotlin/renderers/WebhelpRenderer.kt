package org.jetbrains.dokka.webhelp.renderers

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.base.renderers.html.HtmlRenderer
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.webhelp.renderers.tags.*

open class WebhelpRenderer(private val dokkaContext: DokkaContext) : HtmlRenderer(dokkaContext) {
    override val extension: String
        get() = ".xml"

    override fun FlowContent.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: FlowContent.() -> Unit
    ) {
        childrenCallback()
    }

    override fun FlowContent.buildLink(address: String, content: FlowContent.() -> Unit) =
        a(href = address) {
            attributes["nullable"] = "true"
            content()
        }

    override fun FlowContent.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) = table {
        node.header.takeIf { it.isNotEmpty() }?.let { headers ->
            tr {
                headers.forEach {
                    it.build(this@table, pageContext, sourceSetRestriction)
                }
            }
        }
    }

    override fun FlowContent.buildCodeBlock(code: ContentCodeBlock, pageContext: ContentPage) =
        code(CodeStyle.BLOCK, code.language) {
            code.children.forEach { buildContentNode(it, pageContext) }
        }

    override fun FlowContent.buildCodeInline(code: ContentCodeInline, pageContext: ContentPage) =
        code(lang = code.language) {
            code.children.forEach { buildContentNode(it, pageContext) }
        }


    override fun FlowContent.buildList(
        node: ContentList,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) = list {
            buildListItems(node.children, pageContext, sourceSetRestriction)
        }

    override fun FlowContent.buildPlatformDependent(
        nodes: Map<DisplaySourceSet, Collection<ContentNode>>,
        pageContext: ContentPage,
        extra: PropertyContainer<ContentNode>,
        styles: Set<Style>
    ) {
        nodes.forEach { (sourceset, nodes) ->
            div {
                attributes["section"] = sourceset.toString()
                nodes.forEach { node -> node.build(this, pageContext) }
            }
        }
    }

    open fun LIST.buildListItems(
        items: List<ContentNode>,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>? = null
    ) {
        items.forEach {
            if (it is ContentList)
                buildList(it, pageContext)
            else
                li { it.build(this, pageContext) }
        }
    }

    override fun FlowContent.buildNavigation(page: PageNode) {}

    override fun buildPage(page: ContentPage, content: (FlowContent, ContentPage) -> Unit): String =
        createHTML().topic(title = page.name, id = page.dri.toString()) {
            content(this, page)
        }.let { topic ->
            """<?xml version="1.0" encoding="UTF-8"?>
               <!DOCTYPE topic SYSTEM "https://helpserver.labs.jb.gg/help/html-entities.dtd">
               $topic
            """.trimMargin()
        }

}
package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object SyntaxHighlighter {
    // Kotlin / Java / C++ / General
    private val keywordPattern = "\\b(if|else|when|for|while|return|break|continue|val|var|fun|class|interface|object|is|as|in|!in|try|catch|finally|throw|true|false|null|import|package|public|private|protected|internal|override|suspend|def|let|const|function|from|new|this|super|type|extends|implements|switch|case|default|void|struct|enum|static|inline|constexpr|namespace|using)\\b".toRegex()
    
    // Python
    private val pythonKeywordPattern = "\\b(and|as|assert|async|await|break|class|continue|def|del|elif|else|except|False|finally|for|from|global|if|import|in|is|lambda|None|nonlocal|not|or|pass|raise|return|True|try|while|with|yield)\\b".toRegex()
    
    // XML / HTML
    private val xmlTagPattern = "(</?[a-zA-Z0-9_:-]+)|(/?>)".toRegex()
    private val xmlAttrPattern = "\\b([a-zA-Z0-9_:-]+)(?=\\s*=)".toRegex()
    
    // Gradle / Groovy
    private val gradleKeywordPattern = "\\b(plugins|dependencies|android|buildscript|repositories|task|apply|plugin|implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation)\\b".toRegex()
    
    // YAML / JSON
    private val jsonKeyPattern = "\"([^\"]+)\"\\s*:".toRegex()
    private val yamlKeyPattern = "^\\s*([a-zA-Z0-9_-]+)\\s*:".toRegex(RegexOption.MULTILINE)
    
    // General Base
    private val stringPattern = "(\"[^\"]*\")|('[^']*')|(`[^`]*`)".toRegex()
    private val commentMultiPattern = "(/\\*[\\s\\S]*?\\*/)|(<!--[\\s\\S]*?-->)".toRegex()
    private val commentSinglePattern = "(//.*)|(#.*)".toRegex()
    private val numberPattern = "\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b".toRegex()
    
    // Theme Colors (Dark-friendly Atom One Dark inspired)
    private val colorKeyword = Color(0xFFC678DD)
    private val colorString = Color(0xFF98C379)
    private val colorComment = Color(0xFF7F848E)
    private val colorNumber = Color(0xFFD19A66)
    private val colorTag = Color(0xFFE06C75)
    private val colorAttr = Color(0xFFD19A66)
    private val colorKey = Color(0xFFE5C07B)
    private val colorFind = Color(0xFFD32F2F) // Red bg for find
    
    fun highlight(text: String, extension: String, findQuery: String = "", replaceQuery: String = ""): AnnotatedString {
        if (text.length > 25000) { 
            // Fallback for large files to avoid UI lag. Only highlight search.
            return highlightFindOnly(text, findQuery)
        }
        
        return buildAnnotatedString {
            append(text)
            val ext = extension.lowercase()
            
            try {
                when (ext) {
                    "kt", "kts", "java", "cpp", "c", "h", "cs", "swift", "go", "rs", "js", "ts" -> {
                        keywordPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorKeyword), it.range.first, it.range.last + 1) }
                        numberPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorNumber), it.range.first, it.range.last + 1) }
                        stringPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorString), it.range.first, it.range.last + 1) }
                        commentMultiPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorComment), it.range.first, it.range.last + 1) }
                        commentSinglePattern.findAll(text).forEach { addStyle(SpanStyle(color = colorComment), it.range.first, it.range.last + 1) }
                    }
                    "py", "sh", "bash" -> {
                        pythonKeywordPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorKeyword), it.range.first, it.range.last + 1) }
                        numberPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorNumber), it.range.first, it.range.last + 1) }
                        stringPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorString), it.range.first, it.range.last + 1) }
                        commentSinglePattern.findAll(text).forEach { addStyle(SpanStyle(color = colorComment), it.range.first, it.range.last + 1) }
                    }
                    "xml", "html", "htm", "svg" -> {
                        xmlTagPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorTag), it.range.first, it.range.last + 1) }
                        xmlAttrPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorAttr), it.range.first, it.range.last + 1) }
                        stringPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorString), it.range.first, it.range.last + 1) }
                        commentMultiPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorComment), it.range.first, it.range.last + 1) }
                    }
                    "json" -> {
                        jsonKeyPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorKey), it.groups[1]!!.range.first, it.groups[1]!!.range.last + 1) }
                        stringPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorString), it.range.first, it.range.last + 1) }
                        numberPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorNumber), it.range.first, it.range.last + 1) }
                    }
                    "yml", "yaml" -> {
                        yamlKeyPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorKey), it.groups[1]!!.range.first, it.groups[1]!!.range.last + 1) }
                        stringPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorString), it.range.first, it.range.last + 1) }
                        numberPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorNumber), it.range.first, it.range.last + 1) }
                        commentSinglePattern.findAll(text).forEach { addStyle(SpanStyle(color = colorComment), it.range.first, it.range.last + 1) }
                    }
                    "gradle" -> {
                        gradleKeywordPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorKeyword), it.range.first, it.range.last + 1) }
                        stringPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorString), it.range.first, it.range.last + 1) }
                        commentMultiPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorComment), it.range.first, it.range.last + 1) }
                        commentSinglePattern.findAll(text).forEach { addStyle(SpanStyle(color = colorComment), it.range.first, it.range.last + 1) }
                    }
                    "css" -> {
                        stringPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorString), it.range.first, it.range.last + 1) }
                        commentMultiPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorComment), it.range.first, it.range.last + 1) }
                        numberPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorNumber), it.range.first, it.range.last + 1) }
                    }
                    "md", "txt", "csv", "tsv" -> {
                        // Minimal parsing or fallback
                        if (ext == "md") {
                            "(#+.*)".toRegex().findAll(text).forEach { addStyle(SpanStyle(color = colorKeyword), it.range.first, it.range.last + 1) }
                            "(\\*\\*.*\\*\\*)".toRegex().findAll(text).forEach { addStyle(SpanStyle(color = colorKey), it.range.first, it.range.last + 1) }
                        }
                    }
                    else -> {} // Plain text, no syntax highlight
                }
            } catch (e: Exception) {
                // If regex parsing fails (e.g., complexity limits), fallback gracefully to unformatted text.
            }
            
            if (findQuery.isNotEmpty()) {
                var index = text.indexOf(findQuery, ignoreCase = true)
                while (index >= 0) {
                    addStyle(
                        SpanStyle(background = colorFind, color = Color.White),
                        index,
                        index + findQuery.length
                    )
                    index = text.indexOf(findQuery, index + findQuery.length, ignoreCase = true)
                }
            }
        }
    }
    
    private fun highlightFindOnly(text: String, findQuery: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            if (findQuery.isNotEmpty()) {
                var index = text.indexOf(findQuery, ignoreCase = true)
                while (index >= 0) {
                    addStyle(
                        SpanStyle(background = colorFind, color = Color.White),
                        index,
                        index + findQuery.length
                    )
                    index = text.indexOf(findQuery, index + findQuery.length, ignoreCase = true)
                }
            }
        }
    }
}

class SyntaxVisualTransformation(
    private val extension: String,
    private val findQuery: String = ""
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(text.text, extension, findQuery)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

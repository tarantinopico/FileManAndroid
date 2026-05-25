package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object SyntaxHighlighter {
    // Basic Keywords
    private val keywordPattern = "\\b(if|else|when|for|while|return|break|continue|val|var|fun|class|interface|object|is|as|in|!in|try|catch|finally|throw|true|false|null|import|package|public|private|protected|internal|override|suspend|def|let|const|function|from|new|this|super|type|extends|implements|switch|case|default|void|struct|enum|static|inline|constexpr|namespace|using|export|default|await|async)\\b".toRegex()
    private val pythonKeywordPattern = "\\b(and|as|assert|async|await|break|class|continue|def|del|elif|else|except|False|finally|for|from|global|if|import|in|is|lambda|None|nonlocal|not|or|pass|raise|return|True|try|while|with|yield)\\b".toRegex()
    
    // Simplistic patterns to avoid Regex catastrophic backtracking
    private val xmlTagPattern = "(</?[a-zA-Z0-9_:-]+)|(/?>)".toRegex()
    private val xmlAttrPattern = "\\b([a-zA-Z0-9_:-]+)(?=\\s*=)".toRegex()
    private val gradleKeywordPattern = "\\b(plugins|dependencies|android|buildscript|repositories|task|apply|plugin|implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation|id|version)\\b".toRegex()
    private val jsonKeyPattern = "\"([^\"]+)\"\\s*:".toRegex()
    private val yamlKeyPattern = "^\\s*([a-zA-Z0-9_-]+)\\s*:".toRegex(RegexOption.MULTILINE)
    
    // Fast simplistic string/comment matching
    private val stringPattern = "(\"[^\"]*\")|('[^']*')".toRegex()
    private val commentSinglePattern = "(//.*)|(#.*)".toRegex()
    private val commentMultiPattern = "(/\\*[\\s\\S]*?\\*/)|(<!--[\\s\\S]*?-->)".toRegex()
    private val numberPattern = "\\b\\d+(\\.\\d+)?\\b".toRegex()
    private val bracketPattern = "(\\{|\\}|\\[|\\]|\\(|\\))".toRegex()
    
    // Markdown patterns
    private val mdHeaderPattern = "(^#+\\s.*)".toRegex(RegexOption.MULTILINE)
    private val mdBoldPattern = "(\\*\\*.*?\\*\\*)".toRegex()
    private val mdCodePattern = "(`[^`]+`)".toRegex()
    private val mdLinkPattern = "(\\[.*?\\]\\(.*?\\))".toRegex()
    
    // Theme Colors (Modern styling)
    private val colorKeyword = Color(0xFFC678DD)
    private val colorString = Color(0xFF98C379)
    private val colorComment = Color(0xFF7F848E)
    private val colorNumber = Color(0xFFD19A66)
    private val colorTag = Color(0xFFE06C75)
    private val colorAttr = Color(0xFFD19A66)
    private val colorKey = Color(0xFFE5C07B)
    private val colorBracket = Color(0xFF56B6C2)
    private val colorFind = Color(0xFFD32F2F) 
    
    fun highlight(text: String, extension: String, findQuery: String = "", activeLineHighlightEnabled: Boolean = false, activeLineIndex: Int = -1, activeLineColor: Color = Color(0x20888888)): AnnotatedString {
        // FAST FALLBACK FOR LARGE FILES (e.g. > 40KB) or binary data
        if (text.length > 40000 || text.contains('\u0000')) {
            return highlightFindOnly(text, findQuery, activeLineHighlightEnabled, activeLineIndex, activeLineColor)
        }
        
        return buildAnnotatedString {
            append(text)
            val ext = extension.lowercase()
            
            if (activeLineHighlightEnabled && activeLineIndex >= 0) {
                applyActiveLineHighlight(text, activeLineIndex, activeLineColor, this)
            }
            
            try {
                // If there's an extremely long line, bypass to avoid freezing
                if (text.length > 1000 && text.lines().any { it.length > 500 }) {
                    return highlightFindOnly(text, findQuery, activeLineHighlightEnabled, activeLineIndex, activeLineColor)
                }
                
                // Generic syntax highlighting for unknown/common text types
                val isGeneric = !listOf("kt", "kts", "java", "cpp", "c", "h", "cs", "swift", "go", "rs", "js", "ts", "py", "sh", "bash", "xml", "html", "htm", "svg", "json", "yml", "yaml", "gradle", "css", "md").contains(ext)
                
                when (ext) {
                    "kt", "kts", "java", "cpp", "c", "h", "cs", "swift", "go", "rs", "js", "ts" -> {
                        applyPattern(text, this, keywordPattern, colorKeyword)
                        applyPattern(text, this, numberPattern, colorNumber)
                        applyPattern(text, this, bracketPattern, colorBracket)
                        applyPattern(text, this, stringPattern, colorString)
                        applyPattern(text, this, commentMultiPattern, colorComment)
                        applyPattern(text, this, commentSinglePattern, colorComment)
                    }
                    "py", "sh", "bash" -> {
                        applyPattern(text, this, pythonKeywordPattern, colorKeyword)
                        applyPattern(text, this, numberPattern, colorNumber)
                        applyPattern(text, this, bracketPattern, colorBracket)
                        applyPattern(text, this, stringPattern, colorString)
                        applyPattern(text, this, commentSinglePattern, colorComment)
                    }
                    "xml", "html", "htm", "svg" -> {
                        applyPattern(text, this, xmlTagPattern, colorTag)
                        applyPattern(text, this, xmlAttrPattern, colorAttr)
                        applyPattern(text, this, stringPattern, colorString)
                        applyPattern(text, this, commentMultiPattern, colorComment)
                    }
                    "json" -> {
                        jsonKeyPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorKey), it.groups[1]!!.range.first, it.groups[1]!!.range.last + 1) }
                        applyPattern(text, this, numberPattern, colorNumber)
                        applyPattern(text, this, bracketPattern, colorBracket)
                        applyPattern(text, this, stringPattern, colorString)
                    }
                    "yml", "yaml" -> {
                        yamlKeyPattern.findAll(text).forEach { addStyle(SpanStyle(color = colorKey), it.groups[1]!!.range.first, it.groups[1]!!.range.last + 1) }
                        applyPattern(text, this, numberPattern, colorNumber)
                        applyPattern(text, this, stringPattern, colorString)
                        applyPattern(text, this, commentSinglePattern, colorComment)
                    }
                    "gradle" -> {
                        applyPattern(text, this, gradleKeywordPattern, colorKeyword)
                        applyPattern(text, this, stringPattern, colorString)
                        applyPattern(text, this, bracketPattern, colorBracket)
                        applyPattern(text, this, commentMultiPattern, colorComment)
                        applyPattern(text, this, commentSinglePattern, colorComment)
                    }
                    "css" -> {
                        applyPattern(text, this, stringPattern, colorString)
                        applyPattern(text, this, numberPattern, colorNumber)
                        applyPattern(text, this, bracketPattern, colorBracket)
                        applyPattern(text, this, commentMultiPattern, colorComment)
                    }
                    "md" -> {
                        applyPattern(text, this, mdHeaderPattern, colorKeyword)
                        applyPattern(text, this, mdBoldPattern, colorKey)
                        applyPattern(text, this, mdCodePattern, colorString)
                        applyPattern(text, this, mdLinkPattern, colorTag)
                    }
                    else -> {
                        if (isGeneric && !listOf("txt", "csv", "tsv").contains(ext)) {
                            // Universal Fallback Check
                            applyPattern(text, this, numberPattern, colorNumber)
                            applyPattern(text, this, bracketPattern, colorBracket)
                            applyPattern(text, this, stringPattern, colorString)
                            applyPattern(text, this, commentSinglePattern, colorComment)
                        } else if (listOf("csv", "tsv").contains(ext)) {
                            applyPattern(text, this, numberPattern, colorNumber)
                            applyPattern(text, this, stringPattern, colorString)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to text
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
    
    private fun applyPattern(text: String, builder: AnnotatedString.Builder, pattern: Regex, color: Color) {
        pattern.findAll(text).forEach {
            builder.addStyle(SpanStyle(color = color), it.range.first, it.range.last + 1)
        }
    }
    
    private fun applyActiveLineHighlight(text: String, activeLineIndex: Int, color: Color, builder: AnnotatedString.Builder) {
        var currentLine = 0
        var lineStart = 0
        var i = 0
        while (i < text.length) {
            if (text[i] == '\n' || i == text.length - 1) {
                val lineEnd = if (text[i] == '\n') i else i + 1
                if (currentLine == activeLineIndex) {
                    builder.addStyle(SpanStyle(background = color), lineStart, lineEnd)
                    break
                }
                currentLine++
                lineStart = i + 1
            }
            i++
        }
    }
    
    private fun highlightFindOnly(text: String, findQuery: String, activeLineHighlightEnabled: Boolean = false, activeLineIndex: Int = -1, activeLineColor: Color = Color(0x20888888)): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            
            if (activeLineHighlightEnabled && activeLineIndex >= 0) {
                applyActiveLineHighlight(text, activeLineIndex, activeLineColor, this)
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
}

class SyntaxVisualTransformation(
    private val extension: String,
    private val findQuery: String = "",
    private val activeLineHighlightEnabled: Boolean = false,
    private val activeLineIndex: Int = -1,
    private val activeLineColor: Color = Color(0x20888888)
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(text.text, extension, findQuery, activeLineHighlightEnabled, activeLineIndex, activeLineColor)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}


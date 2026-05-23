package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object SyntaxHighlighter {
    private val keywordPattern = "\\b(if|else|when|for|while|return|break|continue|val|var|fun|class|interface|object|is|as|in|!in|try|catch|finally|throw|true|false|null|import|package|public|private|protected|internal|override|suspend|def|let|const|function|import|export|from|new|this|super|type|extends|implements|switch|case|default|void)\\b".toRegex()
    private val stringPattern = "(\"[^\"]*\")|('[^']*')".toRegex()
    private val commentPattern = "(//.*)|(/\\*[\\s\\S]*?\\*/)".toRegex()
    private val numberPattern = "\\b\\d+(\\.\\d+)?\\b".toRegex()
    
    private val colorKeyword = Color(0xFFC678DD)
    private val colorString = Color(0xFF98C379)
    private val colorComment = Color(0xFF7F848E)
    private val colorNumber = Color(0xFFD19A66)
    
    fun highlight(text: String, extension: String, findQuery: String = "", replaceQuery: String = ""): AnnotatedString {
        if (text.length > 50000) { // Limit regex processing to max 50KB to prevent lag
            return highlightFind(text, findQuery)
        }
        
        return buildAnnotatedString {
            append(text)
            
            val codeExtensions = listOf("kt", "kts", "java", "json", "xml", "yml", "yaml", "py", "js", "html", "css", "cpp", "c", "h", "cs", "swift", "go", "rs")
            if (extension.lowercase() in codeExtensions) {
                try {
                    commentPattern.findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = colorComment), match.range.first, match.range.last + 1)
                    }
                    stringPattern.findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = colorString), match.range.first, match.range.last + 1)
                    }
                    keywordPattern.findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = colorKeyword), match.range.first, match.range.last + 1)
                    }
                    numberPattern.findAll(text).forEach { match ->
                        addStyle(SpanStyle(color = colorNumber), match.range.first, match.range.last + 1)
                    }
                } catch (e: Exception) {
                    // Fallback to plain text on complex matching issues
                }
            }
            
            // Highlight find query
            if (findQuery.isNotEmpty()) {
                var index = text.indexOf(findQuery, ignoreCase = true)
                while (index >= 0) {
                    addStyle(
                        SpanStyle(background = Color(0x66FFEB3B)),
                        index,
                        index + findQuery.length
                    )
                    index = text.indexOf(findQuery, index + findQuery.length, ignoreCase = true)
                }
            }
        }
    }
    
    private fun highlightFind(text: String, findQuery: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            if (findQuery.isNotEmpty()) {
                var index = text.indexOf(findQuery, ignoreCase = true)
                while (index >= 0) {
                    addStyle(
                        SpanStyle(background = Color(0x66FFEB3B)),
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

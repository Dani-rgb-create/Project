package com.example.data

import java.io.File

enum class IssuePriority {
    CRITICAL, MEDIUM, LOW
}

data class WebIssue(
    val id: String,
    val title: String,
    val priority: IssuePriority,
    val description: String,
    val impact: String,
    val reason: String,
    val targetFile: String, // e.g., "index.html"
    val diffContentTarget: String, // what to change
    val diffContentReplacement: String // what to replace it with
)

data class AnalysisResult(
    val healthScore: Int,
    val issues: List<WebIssue>
)

object OfflineAnalyzer {

    fun analyzeProject(projectDir: File): AnalysisResult {
        val issues = mutableListOf<WebIssue>()
        
        // Find index.html
        val indexHtml = File(projectDir, "index.html")
        val sitemapExists = File(projectDir, "sitemap.xml").exists()
        val robotsExists = File(projectDir, "robots.txt").exists()

        var hasIndex = indexHtml.exists() && indexHtml.isFile

        if (!hasIndex) {
            issues.add(
                WebIssue(
                    id = "missing_index",
                    title = "Missing index.html",
                    priority = IssuePriority.CRITICAL,
                    description = "An entry-point 'index.html' is required for static websites.",
                    impact = "The website is completely unrenderable by web servers.",
                    reason = "Browsers and servers look for index.html as the primary landing document by default.",
                    targetFile = "index.html",
                    diffContentTarget = "",
                    diffContentReplacement = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Local Business</title>
</head>
<body>
    <h1>Welcome to My Business</h1>
</body>
</html>"""
                )
            )
            return AnalysisResult(0, issues)
        }

        val htmlContent = indexHtml.readText(Charsets.UTF_8)

        // 1. Check viewport
        if (!htmlContent.contains("name=\"viewport\"", ignoreCase = true) && !htmlContent.contains("name=\'viewport\'", ignoreCase = true)) {
            issues.add(
                WebIssue(
                    id = "missing_viewport",
                    title = "Missing Responsive Viewport Meta Tag",
                    priority = IssuePriority.CRITICAL,
                    description = "No viewport configuration found in metadata header.",
                    impact = "Miserable accessibility scoring and broken display scales on mobile devices.",
                    reason = "Search engines down-rank sites that aren't optimized for modern responsive touchscreen devices.",
                    targetFile = "index.html",
                    diffContentTarget = "</head>",
                    diffContentReplacement = "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n</head>"
                )
            )
        }

        // 2. Check title
        val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(htmlContent)
        if (titleMatch == null || titleMatch.groupValues[1].isBlank()) {
            issues.add(
                WebIssue(
                    id = "missing_title",
                    title = "Empty or Missing SEO Title Tag",
                    priority = IssuePriority.CRITICAL,
                    description = "No <title> tags found in HTML head or title text is blank.",
                    impact = "Severe search index penalty. Web tab will only display raw file or IP url address.",
                    reason = "The HTML title tags remain the strongest on-page header relevance clue for crawler robots.",
                    targetFile = "index.html",
                    diffContentTarget = "</head>",
                    diffContentReplacement = "    <title>Artisanal Local Haven | Best Services Nearby</title>\n</head>"
                )
            )
        }

        // 3. Check description
        val hasDescription = htmlContent.contains("name=\"description\"", ignoreCase = true) || htmlContent.contains("name=\'description\'", ignoreCase = true)
        if (!hasDescription) {
            issues.add(
                WebIssue(
                    id = "missing_description",
                    title = "Missing SEO Meta Description",
                    priority = IssuePriority.MEDIUM,
                    description = "No metadata summary description of business services found.",
                    impact = "Google will display search snippets with parsed disjointed layout text instead of a clean, high-conversion caption.",
                    reason = "User search-result-click-rates improve dramatically when descriptions offer readable highlights.",
                    targetFile = "index.html",
                    diffContentTarget = "</head>",
                    diffContentReplacement = "    <meta name=\"description\" content=\"Professional family-owned services tailored carefully to your local needs. Fully certified & licensed in Dublin.\">\n</head>"
                )
            )
        }

        // 4. Check Open Graph
        val hasOg = htmlContent.contains("property=\"og:", ignoreCase = true) || htmlContent.contains("property=\'og:", ignoreCase = true)
        if (!hasOg) {
            issues.add(
                WebIssue(
                    id = "missing_og",
                    title = "Missing Open Graph Social Schema",
                    priority = IssuePriority.LOW,
                    description = "Social crawl rules (og:title, og:image) are completely missing.",
                    impact = "When shared on chat apps (WhatsApp, Facebook, Discord, Slack), link preview elements look empty or lack visual thumbnails.",
                    reason = "Modern businesses gain word-of-mouth visibility on mobile apps — sharing looks highly incomplete without previews.",
                    targetFile = "index.html",
                    diffContentTarget = "</head>",
                    diffContentReplacement = "    <meta property=\"og:title\" content=\"Local Haven Services\">\n    <meta property=\"og:description\" content=\"Your trusted partner nearby.\">\n    <meta property=\"og:image\" content=\"assets/social-card.jpg\">\n    <meta property=\"og:type\" content=\"website\">\n</head>"
                )
            )
        }

        // 5. Check favicon
        val hasFavicon = htmlContent.contains("rel=\"icon\"", ignoreCase = true) || htmlContent.contains("rel=\"shortcut icon\"", ignoreCase = true) || htmlContent.contains("rel=\'icon\'", ignoreCase = true)
        if (!hasFavicon) {
            issues.add(
                WebIssue(
                    id = "missing_favicon",
                    title = "Missing Favicon Asset Reference",
                    priority = IssuePriority.LOW,
                    description = "No shortcut or vector icon linkage found in the header declarations.",
                    impact = "Default blank paper icon displays in browser tabs, impacting brand identity.",
                    reason = "A beautiful bespoke bookmark favicon increases tab visibility and credibility.",
                    targetFile = "index.html",
                    diffContentTarget = "</head>",
                    diffContentReplacement = "    <link rel=\"icon\" href=\"assets/favicon.svg\" type=\"image/svg+xml\">\n</head>"
                )
            )
        }

        // 6. Check images without alt attribute
        val imgRegex = Regex("<img\\s+([^>]*?)>", RegexOption.IGNORE_CASE)
        var unlabelledImagesCount = 0
        imgRegex.findAll(htmlContent).forEach { match ->
            val attrs = match.groupValues[1]
            val hasAlt = attrs.contains("alt=", ignoreCase = true)
            var hasValuedAlt = false
            if (hasAlt) {
                val valMatch = Regex("alt=[\"'](.*?)[\"']", RegexOption.IGNORE_CASE).find(attrs)
                if (valMatch != null && valMatch.groupValues[1].isNotBlank()) {
                    hasValuedAlt = true
                }
            }
            if (!hasValuedAlt) {
                unlabelledImagesCount++
            }
        }

        if (unlabelledImagesCount > 0) {
            issues.add(
                WebIssue(
                    id = "missing_image_alts",
                    title = "Image tags ($unlabelledImagesCount) lack accessibility labels (alt)",
                    priority = IssuePriority.CRITICAL,
                    description = "Images found on the index file without proper descriptive text in alt attributes.",
                    impact = "Visual screenreader tools for visually-impaired readers fail, hurting compliance audits (WCAG/ADA).",
                    reason = "Search engine crawlers rely strictly on text labels to index and rank relevant image searches.",
                    targetFile = "index.html",
                    diffContentTarget = "<img src=\"assets/cleaning.jpg\">",
                    diffContentReplacement = "<img src=\"assets/cleaning.jpg\" alt=\"Gentle teeth cleaning process under Dr Sarah's care\">"
                )
            )
        }

        // 7. Check double H1 tags
        val h1Regex = Regex("<h1[^>]*>(.*?)</h1>", RegexOption.IGNORE_CASE)
        val h1Count = h1Regex.findAll(htmlContent).count()
        if (h1Count > 1) {
            issues.add(
                WebIssue(
                    id = "duplicate_h1",
                    title = "Duplicate H1 Headings ($h1Count found)",
                    priority = IssuePriority.MEDIUM,
                    description = "Multiple primary heading elements <h1> found in HTML document.",
                    impact = "Confusion in search engine reading hierarchies and poor semantic validation.",
                    reason = "Each webpage should hold exactly one unique definitive H1 representing its core service description.",
                    targetFile = "index.html",
                    diffContentTarget = "<h1>BrightSmile Family Dentistry - Local Dentist Clinic</h1>", // sample matching target
                    diffContentReplacement = "<span style=\"font-size: 24px; font-weight: bold; color: #0b3c5d;\">BrightSmile Family Dentistry</span>"
                )
            )
        }

        // 8. Check schema LocalBusiness
        val hasSchemaLocalJoint = htmlContent.contains("\"@type\"\\s*:\\s*\"LocalBusiness\"", ignoreCase = true) ||
                htmlContent.contains("\"@type\"\\s*:\\s*\"Dentist\"", ignoreCase = true) ||
                htmlContent.contains("\"@type\"\\s*:\\s*\"Bakery\"", ignoreCase = true)
        if (!hasSchemaLocalJoint) {
            issues.add(
                WebIssue(
                    id = "missing_schema",
                    title = "Missing LocalBusiness schema.org Structured Data",
                    priority = IssuePriority.CRITICAL,
                    description = "No JSON-LD metadata found to inform search engines of your business name, address, coordinates, and hours.",
                    impact = "Lost representation in Google Maps local search listings, Google local packs, and localized search results.",
                    reason = "A JSON-LD LocalBusiness metadata script feeds rich snippet graphs in local lookups directly.",
                    targetFile = "index.html",
                    diffContentTarget = "</head>",
                    diffContentReplacement = """    <script type="application/ld+json">
    {
      "@context": "https://schema.org",
      "@type": "LocalBusiness",
      "name": "BrightSmile Dentistry",
      "image": "assets/clinic-front.jpg",
      "address": {
        "@type": "PostalAddress",
        "streetAddress": "32 Lower Abbey St",
        "addressLocality": "Dublin",
        "postalCode": "Dublin 2",
        "addressCountry": "IE"
      },
      "telephone": "+35318642055",
      "priceRange": "$$"
    }
    </script>
</head>"""
                )
            )
        }

        // 9. Check sitemap
        if (!sitemapExists) {
            issues.add(
                WebIssue(
                    id = "missing_sitemap",
                    title = "Missing XML Sitemap",
                    priority = IssuePriority.LOW,
                    description = "File 'sitemap.xml' is not present in the workspace root.",
                    impact = "Crawlers navigate through the domain structure slower and might miss newly published content or auxiliary static files.",
                    reason = "A sitemap acts as a clear roadmap for search indexing bots.",
                    targetFile = "sitemap.xml",
                    diffContentTarget = "",
                    diffContentReplacement = """<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
   <url>
      <loc>https://example.com/</loc>
      <lastmod>2026-06-07</lastmod>
      <changefreq>monthly</changefreq>
      <priority>1.0</priority>
   </url>
</urlset>"""
                )
            )
        }

        // 10. Check robots.txt
        if (!robotsExists) {
            issues.add(
                WebIssue(
                    id = "missing_robots",
                    title = "Missing robots.txt Crawler Boundaries",
                    priority = IssuePriority.LOW,
                    description = "File 'robots.txt' not found in workspace base folder.",
                    impact = "Unrestricted crawling of assets and configuration folders, consuming useless host bandwidth.",
                    reason = "A robots.txt specifies guidelines on resource scanning to search bots.",
                    targetFile = "robots.txt",
                    diffContentTarget = "",
                    diffContentReplacement = """User-agent: *
Allow: /
Sitemap: https://example.com/sitemap.xml"""
                )
            )
        }

        // Calculate score
        // Default starts at 100
        // -30 for missing criticals (HIndex is always there, but major viewport/title/alts count or schema)
        // -15 for mediums
        // -5 for lows
        var score = 100
        issues.forEach { issue ->
            when (issue.priority) {
                IssuePriority.CRITICAL -> score -= 25
                IssuePriority.MEDIUM -> score -= 15
                IssuePriority.LOW -> score -= 10
            }
        }
        if (score < 4) score = 4 // floor boundary

        return AnalysisResult(score, issues)
    }
}

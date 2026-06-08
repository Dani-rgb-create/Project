package com.example.data

import android.content.Context
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class WebFileNode(
    val name: String,
    val relativePath: String, // e.g. "assets/style.css" or "index.html"
    val isDirectory: Boolean,
    val children: List<WebFileNode> = emptyList(),
    val size: Long = 0,
    val extension: String = ""
)

object WebProjectManager {

    private const val PROJECTS_DIR_NAME = "web_projects"

    fun getProjectsDir(context: Context): File {
        val dir = File(context.filesDir, PROJECTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getProjectDir(context: Context, folderName: String): File {
        val dir = File(getProjectsDir(context), folderName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // Recursively build tree nodes of files
    fun getProjectTree(context: Context, folderName: String): WebFileNode {
        val baseDir = getProjectDir(context, folderName)
        return buildNode(baseDir, baseDir)
    }

    private fun buildNode(file: File, baseDir: File): WebFileNode {
        val relativePath = file.relativeTo(baseDir).path
        val isDir = file.isDirectory
        val children = if (isDir) {
            file.listFiles()?.map { buildNode(it, baseDir) }?.sortedWith(
                compareBy<WebFileNode> { !it.isDirectory }.thenBy { it.name }
            ) ?: emptyList()
        } else {
            emptyList()
        }
        val ext = if (!isDir) file.extension.lowercase() else ""
        return WebFileNode(
            name = file.name,
            relativePath = if (relativePath.isBlank()) "" else relativePath,
            isDirectory = isDir,
            children = children,
            size = if (isDir) 0 else file.length(),
            extension = ext
        )
    }

    // Write file content safely
    fun writeFile(context: Context, folderName: String, relativePath: String, content: String) {
        val projectDir = getProjectDir(context, folderName)
        val file = File(projectDir, relativePath)
        // Ensure parent directories exist
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    // Read file content
    fun readFile(context: Context, folderName: String, relativePath: String): String {
        val projectDir = getProjectDir(context, folderName)
        val file = File(projectDir, relativePath)
        return if (file.exists() && file.isFile) {
            file.readText(Charsets.UTF_8)
        } else {
            ""
        }
    }

    // Delete a file or folder recursively
    fun deleteFile(context: Context, folderName: String, relativePath: String): Boolean {
        val projectDir = getProjectDir(context, folderName)
        val file = File(projectDir, relativePath)
        return if (file.exists()) {
            file.deleteRecursively()
        } else {
            false
        }
    }

    // Create default mock/preset web projects on startup
    fun setupPresets(context: Context, webDao: WebDao, onComplete: suspend () -> Unit) {
        // Run in coroutine context inside ViewModel, we just expose the logic
        val dentistFolder = "dentist_clinic"
        val bakeryFolder = "local_bakery"
        val emptyTemplateFolder = "empty_scaffolding"

        val dentistDir = getProjectDir(context, dentistFolder)
        val bakeryDir = getProjectDir(context, bakeryFolder)
        val emptyDir = getProjectDir(context, emptyTemplateFolder)

        // Dentist Site Init
        if (dentistDir.listFiles()?.isEmpty() != false) {
            writeDentistPreset(context, dentistFolder)
        }

        // Bakery Site Init
        if (bakeryDir.listFiles()?.isEmpty() != false) {
            writeBakeryPreset(context, bakeryFolder)
        }

        // Empty Template Site Init
        if (emptyDir.listFiles()?.isEmpty() != false) {
            writeEmptyPreset(context, emptyTemplateFolder)
        }
    }

    private fun writeDentistPreset(context: Context, folder: String) {
        // High quality static landing page for dentist but with multiple critical deficiencies!
        val indexHtml = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <!-- MISSING VIEWPORT META (Accessibility/Mobile bug) -->
    <!-- MISSING META TITLE & DESCRIPTION -->
    <!-- MISSING OPEN GRAPH META -->
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <header>
        <!-- FIRST H1 HEADER -->
        <h1>BrightSmile Family Dentistry - Local Dentist Clinic</h1>
        <nav>
            <a href="#">Home</a>
            <a href="#services">Services</a>
            <a href="#about">About</a>
            <a href="#reviews">Reviews</a>
            <!-- DEAD LINK/MISSING ACTION -->
            <a href="">Book appointment</a>
        </nav>
    </header>

    <main>
        <section class="hero">
            <!-- DUPLICATE H1 HEADER (SEO penalty) -->
            <h1>We Make Your Smile Shine Bright Like A Diamond!</h1>
            <p>Welcome to BrightSmile, located right in the heart of Dublin 2. We provide comprehensive dental care for kids, seniors, and everyone in between. Our dental office features modern technologies to make your dental visit cozy, swift, and completely painless!</p>
            
            <!-- MISSING CLEAR ACTION CTA (Conversion deficit) -->
            <button class="cta-btn">Click Here</button>
        </section>

        <section id="services" class="services">
            <h2>Our Dental Services</h2>
            <div class="services-grid">
                <div class="service-card">
                    <!-- IMAGE WITHOUT ALT TEXT (Accessibility issue) -->
                    <img src="assets/cleaning.jpg">
                    <h3>Teeth Cleanings</h3>
                    <p>Professional scale and polish to keep plaque and cavities far away.</p>
                </div>
                <div class="service-card">
                    <img src="assets/whitening.jpg">
                    <h3>Laser Whitening</h3>
                    <p>Reclaim your pearlescent pearls back in just one 45 minutes session!</p>
                </div>
                <div class="service-card">
                    <!-- IMAGE WITH EMPTY ALT TEXT -->
                    <img src="assets/implants.jpg" alt="">
                    <h3>Root Canals & Implants</h3>
                    <p>Durable, stable implants designed to look and function exactly like organic teeth.</p>
                </div>
            </div>
        </section>

        <section id="about" class="about">
            <h2>Meet Dr. Sarah Vance</h2>
            <p>Dr. Vance graduated from Trinity College Dental Hospital with honors and has spent over 14 years healing toothaches and aligning bright smiles in Dublin. Our welcoming dental clinic staff stays updated on clinical advancements to provide top-notch gentle therapies.</p>
        </section>
    </main>

    <!-- MISSING REGULATORY OR PRIVACY FOOTER -->
    <!-- MISSING SCHEMA.ORG LOCAL BUSINESS JSON-LD -->
    <footer>
        <p>&copy; 2026 BrightSmile. Dublin 2, Ireland.</p>
    </footer>
    <script src="script.js"></script>
</body>
</html>
        """.trimIndent()

        val styleCss = """
/* Duplicate or excessive CSS styles */
body {
    font-family: 'Helvetica Neue', Arial, sans-serif;
    background-color: #f7f9fc;
    color: #333333;
    margin: 0;
    padding: 0;
}

body {
    background-color: #f7f9fc; /* Duplicate body selector and property */
}

header {
    background-color: #ffffff;
    padding: 20px 40px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #eaeaea;
}

h1 {
    color: #0b3c5d;
    font-size: 24px;
}

h1 {
    font-size: 24px; /* Duplicate */
    color: #0b3c5d;
}

.hero {
    background: linear-gradient(135deg, #0b3c5d 0%, #328cc1 100%);
    color: white;
    padding: 80px 40px;
    text-align: center;
}

.hero h1 {
    color: white;
    font-size: 42px;
}

.cta-btn {
    background-color: #d9b310;
    color: #0b3c5d;
    padding: 12px 30px;
    border: none;
    font-size: 16px;
    font-weight: bold;
    cursor: pointer;
    border-radius: 4px;
}

.services {
    padding: 60px 40px;
}

.services h2 {
    text-align: center;
    color: #0b3c5d;
    margin-bottom: 40px;
}

.services-grid {
    display: flex;
    gap: 30px;
}

.service-card {
    background: white;
    padding: 24px;
    border-radius: 8px;
    flex: 1;
    box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}

.service-card img {
    width: 100%;
    height: 180px;
    object-fit: cover;
    border-radius: 4px;
}
        """.trimIndent()

        val scriptJs = """
// Static Dentist script
console.log("BrightSmile Web Interface Initialized.");
document.querySelector(".cta-btn")?.addEventListener("click", function() {
    alert("This placeholder does not link to live scheduling. We need WebAI Copilot to add a high-conversion call widget!");
});
        """.trimIndent()

        writeFile(context, folder, "index.html", indexHtml)
        writeFile(context, folder, "style.css", styleCss)
        writeFile(context, folder, "script.js", scriptJs)
    }

    private fun writeBakeryPreset(context: Context, folder: String) {
        val indexHtml = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>The Golden Crust | Artisanal Bakery in Boston</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="announcement">We deliver organic sourdough fresh daily!</div>
    
    <!-- MISSING HEADER STRUCTURE AND SITEMAP/ROBOTS -->
    <section class="splash">
        <h1>Warm, Crispy, Fresh Bread & Sourdough</h1>
        <p>Using centuries-old slow fermentation methods, we craft authentic loaves that heal the soul. Visit our local brick oven store for delicious hot cinnamon rolls, chocolate croissants, and crusty baguettes.</p>
        <div class="actions">
            <!-- VAGUE CONVERSION TRIGGER -->
            <a href="contact.html">Click here to contact</a>
        </div>
    </section>

    <section class="gallery">
        <!-- NO IMG ALTS -->
        <div class="row">
            <img src="assets/bread1.jpg">
            <img src="assets/bread2.jpg">
        </div>
    </section>

    <!-- NO LOCATION BUSINESS SCHEMA -->
    <footer>
        <p>&copy; 2026 The Golden Crust. Boston, MA.</p>
    </footer>
</body>
</html>
        """.trimIndent()

        val styleCss = """
body {
    font-family: 'Georgia', serif;
    background-color: #faf6f0;
    color: #4a3424;
    margin: 0;
}
.announcement {
    background-color: #d4a373;
    color: white;
    padding: 10px;
    text-align: center;
    font-size: 14px;
    font-weight: bold;
}
.splash {
    text-align: center;
    padding: 60px 20px;
}
h1 {
    font-size: 36px;
    color: #582f0e;
}
.actions a {
    background-color: #582f0e;
    color: white;
    padding: 12px 24px;
    text-decoration: none;
    border-radius: 20px;
}
        """.trimIndent()

        writeFile(context, folder, "index.html", indexHtml)
        writeFile(context, folder, "style.css", styleCss)
    }

    private fun writeEmptyPreset(context: Context, folder: String) {
        val indexHtml = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Responsive Local Business Scaffolding</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <h1>Welcome to your Local Site Scaffolding</h1>
    <p>Use WebAI Copilot to generate structures like rich FAQ, reviews, local schemas, and SEO markers.</p>
    <script src="script.js"></script>
</body>
</html>
        """.trimIndent()

        val styleCss = """
/* Clean initial standard layout */
body {
    font-family: sans-serif;
    max-width: 800px;
    margin: 40px auto;
    padding: 0 20px;
    background-color: #ffffff;
    color: #333333;
    line-height: 1.6;
}
h1 {
    color: #1a73e8;
}
        """.trimIndent()

        val scriptJs = ""

        writeFile(context, folder, "index.html", indexHtml)
        writeFile(context, folder, "style.css", styleCss)
        writeFile(context, folder, "script.js", scriptJs)
    }

    // Export files of a project to a single ZIP stream
    fun exportToZip(context: Context, folderName: String, outStream: OutputStream) {
        val projectDir = getProjectDir(context, folderName)
        val files = projectDir.walkTopDown()
        ZipOutputStream(outStream).use { zos ->
            for (file in files) {
                if (file.isFile) {
                    val relativePath = file.relativeTo(projectDir).path
                    val zipEntry = ZipEntry(relativePath)
                    zos.putNextEntry(zipEntry)
                    file.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    // Import a file system of project elements from a ZIP payload
    fun importFromZip(context: Context, folderName: String, zipInStream: InputStream) {
        val destDir = getProjectDir(context, folderName)
        destDir.deleteRecursively()
        destDir.mkdirs()

        ZipInputStream(zipInStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                // Prevent path traversal vulnerability (security best practice)
                val canonicalDestinationPath = file.canonicalPath
                if (!canonicalDestinationPath.startsWith(destDir.canonicalPath)) {
                    throw SecurityException("Malicious ZIP entry blocked: ${entry.name}")
                }

                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}

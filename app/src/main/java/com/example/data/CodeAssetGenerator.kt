package com.example.data

data class GeneratedAsset(
    val targetFileName: String,
    val contents: String
)

object CodeAssetGenerator {

    fun generateAsset(type: String, metadata: Map<String, String>): GeneratedAsset {
        val bizName = metadata["businessName"] ?: "Local Business Co."
        val street = metadata["street"] ?: "12 Main St"
        val city = metadata["city"] ?: "Boston"
        val phone = metadata["phone"] ?: "+1 617-555-0199"
        val mapPlace = metadata["mapQuery"] ?: "Dublin 2 Location"
        val email = metadata["email"] ?: "contact@example.com"

        return when (type.lowercase()) {
            "hero" -> {
                GeneratedAsset(
                    targetFileName = "index.html",
                    contents = """
    <!-- Generated High-Conversion Hero Grid -->
    <section class="generated-hero" style="background: linear-gradient(rgba(0,0,0,0.6), rgba(0,0,0,0.6)), url('assets/hero-bg.jpg') center/cover no-repeat; color: white; padding: 100px 20px; text-align: center; font-family: sans-serif; border-radius: 12px; margin: 20px 0;">
        <h1 style="font-size: 2.8rem; margin-bottom: 16px; font-weight: 800; letter-spacing: -0.5px;">Premium Care Tailored for $bizName</h1>
        <p style="font-size: 1.2rem; max-width: 600px; margin: 0 auto 30px auto; color: #e2e8f0; line-height: 1.6;">Your local community partner in $city. Book a consult today and feel the difference of custom-focused high quality services.</p>
        <div style="display: flex; gap: 15px; justify-content: center; flex-wrap: wrap;">
            <a href="tel:$phone" class="cta-primary" style="background-color: #f59e0b; color: #1e1b4b; padding: 14px 28px; text-decoration: none; font-weight: bold; border-radius: 30px; box-shadow: 0 4px 10px rgba(245, 158, 11, 0.4); transition: transform 0.2s;">Call Now: $phone</a>
            <a href="#contact" class="cta-secondary" style="background-color: rgba(255,255,255,0.15); color: white; padding: 14px 28px; text-decoration: none; font-weight: 600; border-radius: 30px; border: 1px solid rgba(255,255,255,0.4); backdrop-filter: blur(5px);">Request Consultation</a>
        </div>
    </section>
"""
                )
            }
            "faq" -> {
                GeneratedAsset(
                    targetFileName = "index.html",
                    contents = """
    <!-- Generated Local FAQ Accordion -->
    <section id="faq" class="generated-faq" style="background-color: #f8fafc; padding: 60px 20px; font-family: sans-serif; border-radius: 12px; margin: 20px 0;">
        <div style="max-width: 800px; margin: 0 auto;">
            <h2 style="text-align: center; color: #1e293b; font-size: 2rem; margin-bottom: 10px; font-weight: 700;">Frequently Asked Questions</h2>
            <p style="text-align: center; color: #64748b; margin-bottom: 40px;">Find swift answers about our services at $bizName in $city.</p>
            
            <div style="display: flex; flex-direction: column; gap:16px;">
                <details style="background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; cursor: pointer;">
                    <summary style="font-weight: 600; color: #0f172a; list-style: none; display: flex; justify-content: space-between; align-items: center;">
                        <span>What are the operating hours for $bizName?</span>
                        <span style="color: #f59e0b; font-weight: bold;">+</span>
                    </summary>
                    <p style="color: #475569; margin-top: 12px; line-height: 1.6; font-size: 0.95rem;">We are open Monday through Friday from 8:00 AM to 6:00 PM, and Saturdays by appointment only. Closed on Sundays and major national holidays.</p>
                </details>
                
                <details style="background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; cursor: pointer;">
                    <summary style="font-weight: 600; color: #0f172a; list-style: none; display: flex; justify-content: space-between; align-items: center;">
                        <span>Where are you located and is parking free?</span>
                        <span style="color: #f59e0b; font-weight: bold;">+</span>
                    </summary>
                    <p style="color: #475569; margin-top: 12px; line-height: 1.6; font-size: 0.95rem;">Our physical storefront is at $street, in beautiful $city. There is ample free street parking right outside the lobby and general underground garage capacity across the avenue.</p>
                </details>
                
                <details style="background: white; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; cursor: pointer;">
                    <summary style="font-weight: 600; color: #0f172a; list-style: none; display: flex; justify-content: space-between; align-items: center;">
                        <span>Do you accept credit cards and insurance?</span>
                        <span style="color: #f59e0b; font-weight: bold;">+</span>
                    </summary>
                    <p style="color: #475569; margin-top: 12px; line-height: 1.6; font-size: 0.95rem;">Yes! We accept all major credit cards, bank drafts, Android Pay, and coordinate directly with dental and local wellness insurance networks where qualified.</p>
                </details>
            </div>
        </div>
    </section>
"""
                )
            }
            "contact" -> {
                GeneratedAsset(
                    targetFileName = "index.html",
                    contents = """
    <!-- Generated Local Contact & Map Component -->
    <section id="contact" class="generated-contact" style="background-color: #ffffff; padding: 60px 20px; font-family: sans-serif; border-radius: 12px; margin: 20px 0; border: 1px solid #e2e8f0;">
        <div style="max-width: 1000px; margin: 0 auto; display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 40px;">
            <div>
                <h2 style="color: #1a202c; font-size: 2rem; font-weight: 700; margin-bottom: 20px;">Contact $bizName</h2>
                <p style="color: #4a5568; margin-bottom: 24px; line-height: 1.6;">Ready to start your local wellness transformation? Send us a quick note or give Dr. Sarah's reception desk an immediate ring. We reply within 2 hours.</p>
                
                <div style="display: flex; flex-direction: column; gap: 14px;">
                    <p style="margin:0; color:#2d3748;">📍 <strong>Address:</strong> $street, $city</p>
                    <p style="margin:0; color:#2d3748;">📞 <strong>Phone:</strong> <a href="tel:$phone" style="color: #f59e0b; text-decoration: none;">$phone</a></p>
                    <p style="margin:0; color:#2d3748;">📧 <strong>Email:</strong> <a href="mailto:$email" style="color: #f59e0b; text-decoration: none;">$email</a></p>
                </div>
            </div>
            
            <div>
                <!-- Mock Map visual overlay satisfying UX map placeholder requirements -->
                <div style="width: 100%; height: 240px; background-color: #edf2f7; border-radius: 10px; display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative; border: 1px solid #e2e8f0; overflow: hidden;">
                    <div style="width:100%; height:100%; position:absolute; background: radial-gradient(circle at 50% 50%, #f59e0b 5%, transparent 6%), repeating-linear-gradient(0deg, #ccd5e0 0px, #ccd5e0 1px, transparent 1px, transparent 40px), repeating-linear-gradient(90deg, #ccd5e0 0px, #ccd5e0 1px, transparent 1px, transparent 40px); opacity:0.8;"></div>
                    <div style="z-index: 1; background: white; padding: 12px 20px; border-radius: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); text-align: center;">
                        <span style="font-size: 20px;">📍</span>
                        <p style="margin: 4px 0 0 0; font-size: 13px; font-weight: bold; color: #1a202c;">$bizName Clinic Marker Location</p>
                        <p style="margin: 2px 0 0 0; font-size: 11px; color: #718096;">$street, $city (Virtual Mapping Active)</p>
                    </div>
                </div>
            </div>
        </div>
    </section>
"""
                )
            }
            "sitemap" -> {
                GeneratedAsset(
                    targetFileName = "sitemap.xml",
                    contents = """<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
   <url>
      <loc>https://example.com/</loc>
      <lastmod>2026-06-07</lastmod>
      <changefreq>weekly</changefreq>
      <priority>1.0</priority>
   </url>
   <url>
      <loc>https://example.com/privacy-policy.html</loc>
      <lastmod>2026-06-07</lastmod>
      <changefreq>yearly</changefreq>
      <priority>0.3</priority>
   </url>
</urlset>"""
                )
            }
            "robots" -> {
                GeneratedAsset(
                    targetFileName = "robots.txt",
                    contents = """User-agent: *
Allow: /
Disallow: /admin/
Disallow: /private/

Sitemap: https://example.com/sitemap.xml
"""
                )
            }
            "schema" -> {
                GeneratedAsset(
                    targetFileName = "index.html",
                    contents = """
    <!-- Generated LocalBusiness JSON-LD structured layout -->
    <script type="application/ld+json">
    {
      "@context": "https://schema.org",
      "@type": "LocalBusiness",
      "name": "$bizName",
      "image": "https://example.com/assets/photos/front.jpg",
      "address": {
        "@type": "PostalAddress",
        "streetAddress": "$street",
        "addressLocality": "$city",
        "postalCode": "Dublin 2",
        "addressCountry": "IE"
      },
      "telephone": "$phone",
      "email": "$email",
      "priceRange": "$$",
      "url": "https://example.com",
      "openingHoursSpecification": [
        {
          "@type": "OpeningHoursSpecification",
          "dayOfWeek": [
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday"
          ],
          "opens": "08:00",
          "closes": "18:00"
        }
      ]
    }
    </script>
"""
                )
            }
            "privacy" -> {
                GeneratedAsset(
                    targetFileName = "privacy-policy.html",
                    contents = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Privacy Policy | $bizName</title>
    <style>
        body { font-family: system-ui, -apple-system, sans-serif; line-height: 1.6; max-width: 800px; margin: 40px auto; padding: 20px; background-color: #f8fafc; color: #1e293b; }
        h1 { color: #0f172a; border-bottom: 2px solid #e2e8f0; padding-bottom: 12px; }
        h2 { color: #334155; margin-top: 30px; }
        p { color: #475569; }
    </style>
</head>
<body>
    <h1>Privacy Policy</h1>
    <p>Last updated: June 7, 2026</p>
    <p>At $bizName, accessible from our storefront at $street, $city, one of our main priorities is the privacy of our visitors. This Privacy Policy document contains types of information that is collected and recorded by us and how we use it.</p>
    
    <h2>1. General Data Protection Regulation (GDPR)</h2>
    <p>We are a Data Controller of your information. Our legal basis for collecting and using the personal information described in this Privacy Policy depends on the Personal Information we collect and the specific context in which we collect the information:</p>
    <ul>
        <li>We need to perform a contract with you</li>
        <li>You have given us permission to do so</li>
        <li>Processing your personal information is in our legitimate interests</li>
        <li>We need to comply with the law</li>
    </ul>

    <h2>2. Consent</h2>
    <p>By using our website, you hereby consent to our Privacy Policy and agree to its terms.</p>
    
    <h2>3. Contact Us</h2>
    <p>If you have any questions about this Policy or our data gathering practices, contact reception at $email.</p>
</body>
</html>"""
                )
            }
            else -> {
                GeneratedAsset(
                    targetFileName = "index.html",
                    contents = "<!-- WebAI Copilot section additions -->"
                )
            }
        }
    }
}

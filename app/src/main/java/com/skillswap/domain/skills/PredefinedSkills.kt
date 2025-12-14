package com.skillswap.domain.skills

enum class SkillCategory {
    TECHNOLOGY,
    DESIGN,
    BUSINESS,
    LANGUAGE,
    ART,
    MUSIC,
    SPORT,
    COOKING,
    CRAFT,
    OTHER
}

data class Skill(
    val name: String,
    val category: SkillCategory,
    val description: String? = null,
    val popularity: Int = 0
)

object PredefinedSkills {
    val POPULAR_SKILLS = listOf(
        // Technology
        Skill("Python", SkillCategory.TECHNOLOGY, "Langage de programmation polyvalent", 95),
        Skill("JavaScript", SkillCategory.TECHNOLOGY, "Langage web frontend et backend", 90),
        Skill("Java", SkillCategory.TECHNOLOGY, "Langage de programmation orienté objet", 85),
        Skill("React", SkillCategory.TECHNOLOGY, "Bibliothèque JavaScript pour interfaces", 88),
        Skill("Node.js", SkillCategory.TECHNOLOGY, "Runtime JavaScript côté serveur", 82),
        Skill("Android", SkillCategory.TECHNOLOGY, "Développement d'applications mobiles", 80),
        Skill("iOS/Swift", SkillCategory.TECHNOLOGY, "Développement d'applications iOS", 78),
        Skill("SQL", SkillCategory.TECHNOLOGY, "Gestion de bases de données", 85),
        Skill("Git", SkillCategory.TECHNOLOGY, "Contrôle de version", 90),
        Skill("Docker", SkillCategory.TECHNOLOGY, "Conteneurisation d'applications", 75),
        
        // Design
        Skill("Photoshop", SkillCategory.DESIGN, "Édition et retouche d'images", 88),
        Skill("Illustrator", SkillCategory.DESIGN, "Création graphique vectorielle", 82),
        Skill("Figma", SkillCategory.DESIGN, "Design d'interfaces UI/UX", 90),
        Skill("After Effects", SkillCategory.DESIGN, "Animation et effets visuels", 75),
        Skill("Blender", SkillCategory.DESIGN, "Modélisation 3D et animation", 70),
        Skill("UI/UX Design", SkillCategory.DESIGN, "Conception d'expériences utilisateur", 92),
        Skill("Graphic Design", SkillCategory.DESIGN, "Design graphique général", 85),
        
        // Business
        Skill("Marketing Digital", SkillCategory.BUSINESS, "Stratégies marketing en ligne", 88),
        Skill("Excel", SkillCategory.BUSINESS, "Tableur et analyse de données", 90),
        Skill("Gestion de Projet", SkillCategory.BUSINESS, "Planification et coordination", 85),
        Skill("Comptabilité", SkillCategory.BUSINESS, "Gestion financière", 78),
        Skill("SEO", SkillCategory.BUSINESS, "Optimisation pour moteurs de recherche", 82),
        Skill("Communication", SkillCategory.BUSINESS, "Compétences relationnelles", 95),
        Skill("Leadership", SkillCategory.BUSINESS, "Management d'équipe", 80),
        
        // Languages
        Skill("Anglais", SkillCategory.LANGUAGE, "Langue internationale", 95),
        Skill("Français", SkillCategory.LANGUAGE, "Langue française", 88),
        Skill("Arabe", SkillCategory.LANGUAGE, "Langue arabe", 85),
        Skill("Espagnol", SkillCategory.LANGUAGE, "Langue espagnole", 80),
        Skill("Allemand", SkillCategory.LANGUAGE, "Langue allemande", 75),
        Skill("Chinois", SkillCategory.LANGUAGE, "Langue chinoise mandarin", 72),
        
        // Art
        Skill("Dessin", SkillCategory.ART, "Techniques de dessin", 85),
        Skill("Peinture", SkillCategory.ART, "Peinture acrylique, huile, aquarelle", 80),
        Skill("Sculpture", SkillCategory.ART, "Art de la sculpture", 65),
        Skill("Photographie", SkillCategory.ART, "Art photographique", 88),
        Skill("Calligraphie", SkillCategory.ART, "Art de la belle écriture", 70),
        
        // Music
        Skill("Piano", SkillCategory.MUSIC, "Instrument de musique à touches", 85),
        Skill("Guitare", SkillCategory.MUSIC, "Instrument à cordes", 90),
        Skill("Chant", SkillCategory.MUSIC, "Techniques vocales", 82),
        Skill("Production Musicale", SkillCategory.MUSIC, "Création et mixage audio", 78),
        Skill("Solfège", SkillCategory.MUSIC, "Théorie musicale", 75),
        
        // Sport
        Skill("Yoga", SkillCategory.SPORT, "Pratique corps-esprit", 90),
        Skill("Football", SkillCategory.SPORT, "Sport collectif", 85),
        Skill("Tennis", SkillCategory.SPORT, "Sport de raquette", 78),
        Skill("Musculation", SkillCategory.SPORT, "Entraînement physique", 88),
        Skill("Course à pied", SkillCategory.SPORT, "Cardio et endurance", 85),
        Skill("Natation", SkillCategory.SPORT, "Sport aquatique", 82),
        
        // Cooking
        Skill("Cuisine Tunisienne", SkillCategory.COOKING, "Gastronomie traditionnelle", 92),
        Skill("Pâtisserie", SkillCategory.COOKING, "Art des desserts", 88),
        Skill("Cuisine Italienne", SkillCategory.COOKING, "Cuisine méditerranéenne", 85),
        Skill("Cuisine Française", SkillCategory.COOKING, "Haute gastronomie", 80),
        Skill("Cuisine Végétarienne", SkillCategory.COOKING, "Cuisine sans viande", 78),
        
        // Craft
        Skill("Couture", SkillCategory.CRAFT, "Confection de vêtements", 82),
        Skill("Tricot", SkillCategory.CRAFT, "Création textile", 75),
        Skill("Menuiserie", SkillCategory.CRAFT, "Travail du bois", 78),
        Skill("Poterie", SkillCategory.CRAFT, "Art de la céramique", 70),
        Skill("Bijouterie", SkillCategory.CRAFT, "Création de bijoux", 68),
        
        // Other
        Skill("Jardinage", SkillCategory.OTHER, "Culture de plantes", 80),
        Skill("Mécanique Auto", SkillCategory.OTHER, "Réparation automobile", 75),
        Skill("Électronique", SkillCategory.OTHER, "Circuits et composants", 72),
        Skill("Premiers Secours", SkillCategory.OTHER, "Gestes de sauvetage", 85)
    )
    
    fun getByCategory(category: SkillCategory): List<Skill> {
        return POPULAR_SKILLS.filter { it.category == category }
    }
    
    fun search(query: String): List<Skill> {
        return POPULAR_SKILLS.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description?.contains(query, ignoreCase = true) == true
        }
    }
}
